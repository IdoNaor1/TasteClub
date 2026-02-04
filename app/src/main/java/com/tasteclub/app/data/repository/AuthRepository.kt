package com.tasteclub.app.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.tasteclub.app.data.local.dao.UserDao
import com.tasteclub.app.data.local.entity.toDomain
import com.tasteclub.app.data.local.entity.toEntity
import com.tasteclub.app.data.model.User
import com.tasteclub.app.data.remote.firebase.FirebaseAuthSource
import com.tasteclub.app.data.remote.firebase.FirestoreSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthRepository(
    private val authSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource,
    private val userDao: UserDao
) {

    fun currentUserId(): String? = authSource.currentUserId()
    fun isLoggedIn(): Boolean = authSource.isLoggedIn()

    /**
     * Observe user profile from local cache (Room).
     * UI should observe this.
     */
    fun observeUser(uid: String): LiveData<User?> {
        return userDao.getById(uid).map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * Register:
     * 1) Firebase Auth create user -> uid
     * 2) Create initial User profile
     * 3) Save to Firestore
     * 4) Cache to Room
     */
    suspend fun register(email: String, password: String, userName: String): Result<User> {
        android.util.Log.d("AuthRepository", "Register function called")
        return try {
            android.util.Log.d("AuthRepository", "Calling authSource.register")
            val uid = authSource.register(email, password)
            android.util.Log.d("AuthRepository", "authSource.register returned UID: $uid")

            val now = System.currentTimeMillis()
            val user = User(
                uid = uid,
                email = email,
                userName = userName,
                profileImageUrl = "",
                bio = "",
                followersCount = 0,
                followingCount = 0,
                createdAt = now,
                lastUpdated = now
            )

            android.util.Log.d("AuthRepository", "Calling firestoreSource.upsertUser")
            firestoreSource.upsertUser(user)
            android.util.Log.d("AuthRepository", "firestoreSource.upsertUser finished")
            userDao.upsert(user.toEntity())

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Registration failed with exception", e)
            Result.failure(e)
        }
    }

    /**
     * Login:
     * 1) Firebase Auth sign in -> uid
     * 2) Pull profile from Firestore (if exists) and cache to Room
     */
    suspend fun login(email: String, password: String): User? {
        val uid = authSource.login(email, password)
        return refreshUserFromRemote(uid)
    }

    /**
     * Pull the latest user profile from Firestore and store in Room.
     * Returns the fetched user (or null if no user doc exists yet).
     */
    suspend fun refreshUserFromRemote(uid: String): User? {
        val remoteUser = firestoreSource.getUser(uid)
        if (remoteUser != null) {
            userDao.upsert(remoteUser.toEntity())
        }
        return remoteUser
    }

    /**
     * Update profile in Firestore and cache.
     * (Use this after uploading profile image and getting URL.)
     */
    suspend fun updateProfile(
        uid: String,
        userName: String? = null,
        bio: String? = null,
        profileImageUrl: String? = null
    ) {
        firestoreSource.updateUserProfile(
            uid = uid,
            userName = userName,
            bio = bio,
            profileImageUrl = profileImageUrl
        )

        // After remote update, fetch canonical version and cache it
        refreshUserFromRemote(uid)
    }

    fun logout() {
        authSource.logout()
        // Optional: clean local cache
    }

    suspend fun sendPasswordReset(email: String) {
        authSource.sendPasswordReset(email)
    }

    /**
     * Observe authentication state changes.
     * Emits true if user is logged in, false otherwise.
     */
    fun observeAuthState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }
}
