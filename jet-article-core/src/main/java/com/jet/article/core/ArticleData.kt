package com.jet.article.core

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable


/**
 * @author Miroslav Hýbler <br>
 * created on 11.02.2026
 */
@Keep
@Immutable
public data class ArticleData public constructor(
    val url: String,
    val elements: List<ArticleElement>,
    val headData: ArticleHeadData,
) {

    @Keep
    companion object {
        /**
         * Empty data instance, can be used to avoid nullability
         * @since 1.0.0
         */
        val Empty: ArticleData = ArticleData(
            url = "",
            elements = emptyList(),
            headData = ArticleHeadData.Empty,
        )
    }


    /**
     * True when data are empty
     * @since 1.0.0
     */
    val isEmpty: Boolean
        get() = elements.isEmpty() && url == "" && headData == ArticleHeadData.Empty


}
