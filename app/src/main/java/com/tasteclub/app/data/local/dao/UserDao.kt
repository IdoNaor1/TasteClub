package com.tasteclub.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.tasteclub.app.data.local.entity.UserEntity

@Dao
interface UserDao {

    // ---- Read ----
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun getById(uid: String): LiveData<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAll(): LiveData<List<UserEntity>>

    // ---- Write ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    // ---- Delete ----
    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteById(uid: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
