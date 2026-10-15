package io.github.rupsilonomicron.listo.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val isImportant: Boolean = false,
    val isDone: Boolean = false,
    /** 履歴の並び順に使う。未完了なら null */
    val completedAt: Long? = null,
    val sortOrder: Int,
    val createdAt: Long,
)

/** リスト一覧の表示用：リストと未完了件数 */
data class ListSummary(
    @Embedded val list: ListEntity,
    val activeCount: Int,
)
