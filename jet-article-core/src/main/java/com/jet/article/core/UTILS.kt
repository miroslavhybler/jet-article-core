@file:Suppress("RedundantVisibilityModifier")

package com.jet.article.core

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.net.URI
import java.net.URISyntaxException

/**
 * @since 1.0.0
 * @author Miroslav Hýbler <br>
 * created on 25.08.2023
 */
////////////////////////////////////////////////////////////////////////////////////////////////////
/////
/////   Context utils
/////
////////////////////////////////////////////////////////////////////////////////////////////////////


/**
 * @since 1.0.0
 */
@Deprecated(message = "Old proj")
fun Context.openEmailApp(
    email: String,
    subject: String? = null,
    text: String? = null,
    title: String = "",
) {
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SENDTO)
                .setData("mailto:".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(email)),
            title,
        )
    )
}


/**
 * @since 1.0.0
 */
@Deprecated(message = "Old proj")
fun Context.openDialApp(
    phoneNumber: String,
) {
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_DIAL)
                .setData("tel:${phoneNumber}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ""
        )
    )
}


/**
 * @since 1.0.0
 */
@Deprecated(message = "Old proj")
fun Context.openMapsApp(
    address: String,
) {
    val url = "http://maps.google.com/maps?daddr=$address"
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}