package com.tasteclub.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun currentUserId(): String? = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun register(email: String, password: String): String {
        require(email.isNotBlank()) { "email must not be blank" }
        require(password.isNotBlank()) { "password must not be blank" }

        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user?.uid) { "Firebase returned null user after register" }
    }

    suspend fun login(email: String, password: String): String {
        require(email.isNotBlank()) { "email must not be blank" }
        require(password.isNotBlank()) { "password must not be blank" }

        val result = auth.signInWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user?.uid) { "Firebase returned null user after login" }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "email must not be blank" }
        auth.sendPasswordResetEmail(email).await()
    }
}
