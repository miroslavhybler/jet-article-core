package com.jet.article.core

import android.util.Log
import androidx.annotation.Keep

/**
 * @author Miroslav Hýbler <br>
 * created on 07.02.2026
 */
@Keep
class JetArticleNativeLib {

    init {
        try {
            System.loadLibrary("nativelib")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("ContentTransformer", "Failed to load native library", e)
        }
    }


    /**
     * This function will be called from native code
     */
    external fun parse(html: String, callback: TagCallback)
}
