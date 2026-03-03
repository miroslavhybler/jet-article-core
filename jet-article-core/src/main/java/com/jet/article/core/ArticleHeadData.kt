@file:Suppress(
    "RedundantVisibilityModifier",
    "DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING"
)

package com.jet.article.core

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable


/**
 * @author Miroslav Hýbler <br>
 * created on 08.12.2023
 */
@Keep
@Immutable
public data class ArticleHeadData internal constructor(
    val title: String?,
) {

    @Keep
    companion object {
        val Empty: ArticleHeadData = ArticleHeadData(title = null)
    }
}