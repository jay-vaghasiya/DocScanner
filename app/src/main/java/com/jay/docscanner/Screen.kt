package com.jay.docscanner

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations using Kotlin Serialization
 */
sealed interface Screen {

    @Serializable
    data object Home : Screen

    @Serializable
    data object Scanner : Screen

    @Serializable
    data class Preview(
        val imageUris: List<String>,
        val pdfPath: String?
    ) : Screen

    @Serializable
    data class DocumentDetail(
        val documentId: String,
        val documentName: String
    ) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object History : Screen
}