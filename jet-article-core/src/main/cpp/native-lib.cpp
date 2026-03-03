#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <android/log.h>
#include "gumbo.h"

#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// JNI global references
jclass g_tagCallbackClass = nullptr;
jmethodID g_onStartTagMethodID = nullptr;
jmethodID g_onEndTagMethodID = nullptr;
jmethodID g_onTextMethodID = nullptr;
jclass g_hashMapClass = nullptr;
jmethodID g_hashMapConstructor = nullptr;
jmethodID g_hashMapPutMethod = nullptr;

// Helper to create a Java HashMap from a C++ map
jobject createJavaHashMap(JNIEnv* env, const std::map<std::string, std::string>& cppMap) {
    if (!g_hashMapClass) return nullptr;
    jobject hashMap = env->NewObject(g_hashMapClass, g_hashMapConstructor);
    if (!hashMap) return nullptr;

    for (const auto& pair : cppMap) {
        jstring key = env->NewStringUTF(pair.first.c_str());
        jstring value = env->NewStringUTF(pair.second.c_str());
        env->CallObjectMethod(hashMap, g_hashMapPutMethod, key, value);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(value);
    }
    return hashMap;
}

// The new SAX-style traversal function
void traverse_gumbo_node_sax(JNIEnv* env, jobject callback, GumboNode* node) {
    if (!node) return;

    switch (node->type) {
        case GUMBO_NODE_ELEMENT: {
            GumboElement* element = &node->v.element;
            const char* tagName = gumbo_normalized_tagname(element->tag);
             if (!tagName || tagName[0] == '\0') {
                GumboStringPiece originalTag = element->original_tag;
                gumbo_tag_from_original_text(&originalTag);
                tagName = std::string(originalTag.data, originalTag.length).c_str();
            }

            // Fire onStartTag
            std::map<std::string, std::string> attributes;
            for (unsigned int i = 0; i < element->attributes.length; ++i) {
                GumboAttribute* attr = static_cast<GumboAttribute*>(element->attributes.data[i]);
                attributes[attr->name] = attr->value;
            }
            jstring jTagName = env->NewStringUTF(tagName);
            jobject jAttributes = createJavaHashMap(env, attributes);
            env->CallVoidMethod(callback, g_onStartTagMethodID, jTagName, jAttributes);
            env->DeleteLocalRef(jAttributes);

            // Recurse for children
            GumboVector* children = &element->children;
            for (unsigned int i = 0; i < children->length; ++i) {
                traverse_gumbo_node_sax(env, callback, static_cast<GumboNode*>(children->data[i]));
            }

            // Fire onEndTag
            env->CallVoidMethod(callback, g_onEndTagMethodID, jTagName);
            env->DeleteLocalRef(jTagName);
            break;
        }
        case GUMBO_NODE_TEXT:
        case GUMBO_NODE_WHITESPACE: {
            // Fire onText
            jstring jContent = env->NewStringUTF(node->v.text.text);
            env->CallVoidMethod(callback, g_onTextMethodID, jContent);
            env->DeleteLocalRef(jContent);
            break;
        }
        default:
            // For document root, just recurse
            if (node->type == GUMBO_NODE_DOCUMENT) {
                GumboVector* children = &node->v.document.children;
                for (unsigned int i = 0; i < children->length; ++i) {
                    traverse_gumbo_node_sax(env, callback, static_cast<GumboNode*>(children->data[i]));
                }
            }
            // Other node types (comments, cdata, etc.) are ignored
            break;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_jet_article_nativelib_JetArticleNativeLib_parse(
        JNIEnv* env,
        jobject thiz,
        jstring html,
        jobject callback) {

    if (!html || !callback) {
        LOGE("Input HTML string or callback is null.");
        return;
    }

    // Initialize JNI global references
    if (!g_tagCallbackClass) {
        jclass localCallbackClass = env->FindClass("com/jet/article/core/TagCallback");
        if (!localCallbackClass) { LOGE("Failed to find TagCallback class."); return; }
        g_tagCallbackClass = static_cast<jclass>(env->NewGlobalRef(localCallbackClass));
        env->DeleteLocalRef(localCallbackClass);

        g_onStartTagMethodID = env->GetMethodID(g_tagCallbackClass, "onStartTag", "(Ljava/lang/String;Ljava/util/Map;)V");
        g_onEndTagMethodID = env->GetMethodID(g_tagCallbackClass, "onEndTag", "(Ljava/lang/String;)V");
        g_onTextMethodID = env->GetMethodID(g_tagCallbackClass, "onText", "(Ljava/lang/String;)V");

        if (!g_onStartTagMethodID || !g_onEndTagMethodID || !g_onTextMethodID) {
            LOGE("Failed to get method IDs for TagCallback.");
            return;
        }
    }

    if (!g_hashMapClass) {
        jclass localHashMapClass = env->FindClass("java/util/HashMap");
        if (!localHashMapClass) { LOGE("Failed to find HashMap class."); return; }
        g_hashMapClass = static_cast<jclass>(env->NewGlobalRef(localHashMapClass));
        env->DeleteLocalRef(localHashMapClass);

        g_hashMapConstructor = env->GetMethodID(g_hashMapClass, "<init>", "()V");
        g_hashMapPutMethod = env->GetMethodID(g_hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        if (!g_hashMapConstructor || !g_hashMapPutMethod) {
            LOGE("Failed to get method IDs for HashMap.");
            return;
        }
    }

    const char* html_chars = env->GetStringUTFChars(html, nullptr);
    if (!html_chars) { LOGE("Failed to get UTF chars from HTML string."); return; }

    GumboOutput* output = gumbo_parse_with_options(&kGumboDefaultOptions, html_chars, strlen(html_chars));
    if (!output) {
        LOGE("Gumbo parsing failed.");
        env->ReleaseStringUTFChars(html, html_chars);
        return;
    }

    traverse_gumbo_node_sax(env, callback, output->document);

    gumbo_destroy_output(&kGumboDefaultOptions, output);
    env->ReleaseStringUTFChars(html, html_chars);
}
