package com.tasteclub.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tasteclub.app.data.local.dao.ReviewDao
import com.tasteclub.app.data.local.dao.UserDao
import com.tasteclub.app.data.local.dao.RestaurantDao
import com.tasteclub.app.data.local.entity.ReviewEntity
import com.tasteclub.app.data.local.entity.UserEntity
import com.tasteclub.app.data.local.entity.RestaurantEntity

@Database(
    entities = [UserEntity::class, ReviewEntity::class, RestaurantEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TasteClubDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun reviewDao(): ReviewDao
    abstract fun restaurantDao(): RestaurantDao

    companion object {
        @Volatile
        private var INSTANCE: TasteClubDatabase? = null

        fun getInstance(context: Context): TasteClubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TasteClubDatabase::class.java,
                    "tasteclub_db"
                )
                    // for development - should not be used in production
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
