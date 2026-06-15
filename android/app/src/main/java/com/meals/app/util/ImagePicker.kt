package com.meals.app.util

/**
 * Placeholder for image picker functionality.
 * Will be implemented with ActivityResultContracts.GetContent in a future update.
 */
object ImagePicker {
    // TODO: Implement with ActivityResultContracts.GetContent
    //
    // Usage example (to be implemented):
    //
    // val imagePickerLauncher = rememberLauncherForActivityResult(
    //     contract = ActivityResultContracts.GetContent()
    // ) { uri: Uri? ->
    //     uri?.let {
    //         // Handle selected image URI
    //         // Upload to server and get URL
    //     }
    // }
    //
    // To launch: imagePickerLauncher.launch("image/*")

    /**
     * Supported image MIME types for the picker.
     */
    val supportedMimeTypes = listOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    /**
     * Maximum file size in bytes (5MB).
     */
    const val MAX_FILE_SIZE = 5 * 1024 * 1024L

    /**
     * Maximum image dimension (width or height) in pixels.
     */
    const val MAX_DIMENSION = 2048
}
