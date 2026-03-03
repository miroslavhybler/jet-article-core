package com.jet.article.core

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/**
 *
 * @since 1.0.0
 */
@Keep
@Immutable
sealed class Link(
    open val rawLink: String,
    open val fullLink: String,
) {

    @Keep
    @Immutable
    data class UriLink(
        override val rawLink: String,
        override val fullLink: String,
    ) : Link(
        rawLink = rawLink,
        fullLink = fullLink,
    )

    @Keep
    @Immutable
    data class SameDomainLink(
        override val rawLink: String,
        override val fullLink: String,
    ) : Link(
        rawLink = rawLink,
        fullLink = fullLink
    )

    @Keep
    @Immutable
    data class OtherDomainLink(
        override val rawLink: String,
        override val fullLink: String,
    ) : Link(
        rawLink = rawLink,
        fullLink = fullLink
    )

    @Keep
    @Immutable
    data class SectionLink(
        override val rawLink: String,
        override val fullLink: String,
    ) : Link(
        rawLink = rawLink,
        fullLink = fullLink,
    )
}
