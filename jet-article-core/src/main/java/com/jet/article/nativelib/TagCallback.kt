package com.jet.article.nativelib

/**
 * A SAX-style callback interface for receiving granular HTML parsing events from the native parser.
 */
interface TagCallback {
    /**
     * Called when a start tag is encountered (e.g., `<p>`, `<b>`).
     *
     * @param tagName The name of the HTML tag.
     * @param attributes A map of the tag's attributes.
     */
    fun onStartTag(tagName: String, attributes: Map<String, String>)

    /**
     * Called when an end tag is encountered (e.g., `</p>`, `</b>`).
     *
     * @param tagName The name of the HTML tag.
     */
    fun onEndTag(tagName: String)

    /**
     * Called when a text node is encountered.
     *
     * @param content The raw text content.
     */
    fun onText(content: String)
}
