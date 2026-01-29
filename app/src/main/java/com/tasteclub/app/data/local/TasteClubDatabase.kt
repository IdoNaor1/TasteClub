package com.tasteclub.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tasteclub.app.data.local.dao.ReviewDao
import com.tasteclub.app.data.local.dao.UserDao
import com.tasteclub.app.data.local.entity.ReviewEntity
import com.tasteclub.app.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, ReviewEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TasteClubDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun reviewDao(): ReviewDao

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
                    // במעבר לפיתוח: אפשר להשאיר. בפרודקשן עושים migrations.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
