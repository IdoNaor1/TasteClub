package com.tasteclub.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasteclub.app.data.local.entity.RestaurantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {

    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getById(id: String): RestaurantEntity?

    @Query("SELECT * FROM restaurants WHERE id = :id LIMIT 1")
    fun observeById(id: String): LiveData<RestaurantEntity?>

    @Query("SELECT * FROM restaurants ORDER BY lastUpdated DESC")
    fun getAll(): Flow<List<RestaurantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(restaurant: RestaurantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(restaurants: List<RestaurantEntity>)

    @Query("DELETE FROM restaurants WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM restaurants")
    suspend fun deleteAll()
}
