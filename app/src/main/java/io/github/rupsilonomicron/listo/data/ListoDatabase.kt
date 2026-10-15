package io.github.rupsilonomicron.listo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ListEntity::class, GroupEntity::class, ItemEntity::class],
    version = 2,
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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        /** v2：グループ（item_groups）の追加と、項目への groupId 列の追加 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`listId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`listId`) REFERENCES `lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_groups_listId` ON `item_groups` (`listId`)")
                db.execSQL("ALTER TABLE `items` ADD COLUMN `groupId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_groupId` ON `items` (`groupId`)")
            }
        }
    }
}
