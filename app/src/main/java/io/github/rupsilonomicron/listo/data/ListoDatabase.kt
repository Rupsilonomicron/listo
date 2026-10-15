package io.github.rupsilonomicron.listo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ListEntity::class, ItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ListoDatabase : RoomDatabase() {
    abstract fun dao(): ListoDao

    companion object {
        @Volatile
        private var instance: ListoDatabase? = null

        fun get(context: Context): ListoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ListoDatabase::class.java,
                    "listo.db",
                ).build().also { instance = it }
            }
    }
}
