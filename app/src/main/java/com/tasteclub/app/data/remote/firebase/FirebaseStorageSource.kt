package com.tasteclub.app.data.remote.firebase

import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class FirebaseStorageSource(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val rootRef = storage.reference

    /**
     * Upload profile image to: profile_images/{uid}.jpg
     * Returns: download URL
     */
    suspend fun uploadProfileImage(uid: String, bitmap: Bitmap): String {
        require(uid.isNotBlank()) { "uid must not be blank" }

        val bytes = bitmap.toJpegBytes(85)
        val ref = rootRef.child("profile_images/$uid.jpg")

        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload review image to: review_images/{reviewId}.jpg
     * Returns: download URL
     */
    suspend fun uploadReviewImage(reviewId: String, bitmap: Bitmap): String {
        require(reviewId.isNotBlank()) { "reviewId must not be blank" }

        val bytes = bitmap.toJpegBytes(85)
        val ref = rootRef.child("review_images/$reviewId.jpg")

        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteProfileImage(uid: String) {
        require(uid.isNotBlank()) { "uid must not be blank" }
        rootRef.child("profile_images/$uid.jpg").delete().await()
    }

    suspend fun deleteReviewImage(reviewId: String) {
        require(reviewId.isNotBlank()) { "reviewId must not be blank" }
        rootRef.child("review_images/$reviewId.jpg").delete().await()
    }

    // --------------------
    // Helpers
    // --------------------
    private fun Bitmap.toJpegBytes(quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), out)
        return out.toByteArray()
    }
}
