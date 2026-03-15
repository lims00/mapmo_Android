package com.a6w.memo.domain.model

import kotlin.math.min

/**
 * Mapmo model
 * - Detailed information of Mapmo
 * - Available on the memo detail page
 */
data class Mapmo(
    // Mapmo unique Id
    val mapmoID: String,
    // Mapmo title & content
    val content: String,
    // Mapmo notification settings status
    val isNotifyEnabled: Boolean,
    // Label name of the Mapmo
    val labelID: String? = null,
    // Last modified date and time
    val updatedAt: Long,
) {
    companion object {
        private const val MAX_TITLE_LENGTH = 20
    }
    // Mapmo title
    // - Automatically generated from the content
    // - If mapmo content is short, uses total content as title
    // - Otherwise, uses front MAX_TITLE_LENGTH of content as title
    val title: String
        get() = if(content.length > MAX_TITLE_LENGTH) {
            // Content is too long, add ellipsis
            "${content.take(MAX_TITLE_LENGTH)}..."
        } else {
            content
        }
}


