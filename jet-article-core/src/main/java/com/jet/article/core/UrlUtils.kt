package com.jet.article.core

import java.net.URISyntaxException
import java.net.URI

internal fun String.toDomainName(): String? {
    return try {
        val uri = URI(this)
        uri.host
    } catch (e: URISyntaxException) {
        null
    }
}