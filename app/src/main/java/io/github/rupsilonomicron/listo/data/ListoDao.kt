package io.github.rupsilonomicron.listo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListoDao {

    // ---- リスト ----

    @Query(
        """
        SELECT l.*, (SELECT COUNT(*) FROM items i WHERE i.listId = l.id AND i.isDone = 0) AS activeCount
        FROM lists l ORDER BY l.sortOrder, l.id
        """,
    )
    fun observeListSummaries(): Flow<List<ListSummary>>

    @Query("SELECT * FROM lists ORDER BY sortOrder, id")
    fun observeLists(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observeList(id: Long): Flow<ListEntity?>

    @Query("SELECT * FROM lists ORDER BY sortOrder, id")
    suspend fun getLists(): List<ListEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM lists")
    suspend fun maxListSortOrder(): Int

    @Insert
    suspend fun insertList(list: ListEntity): Long

    @Update
    suspend fun updateLists(lists: List<ListEntity>)

    @Query("UPDATE lists SET name = :name WHERE id = :id")
    suspend fun renameList(id: Long, name: String)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteListRow(id: Long)

    @Query("DELETE FROM items WHERE listId = :listId")
    suspend fun deleteItemsOfList(listId: Long)

    /** 外部キー制約の有効・無効に依存しないよう、項目も明示的に削除する */
    @Transaction
    suspend fun deleteListWithItems(id: Long) {
        deleteItemsOfList(id)
        deleteListRow(id)
    }

    // ---- 項目 ----

    @Query("SELECT * FROM items WHERE listId = :listId AND isDone = 0 ORDER BY isImportant DESC, sortOrder, id")
    fun observeActiveItems(listId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE listId = :listId AND isDone = 1 ORDER BY completedAt DESC, id DESC")
    fun observeDoneItems(listId: Long): Flow<List<ItemEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM items WHERE listId = :listId")
    suspend fun maxItemSortOrder(listId: Long): Int

    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Query("UPDATE items SET title = :title, isImportant = :isImportant WHERE id = :id")
    suspend fun updateItemContent(id: Long, title: String, isImportant: Boolean)

    @Query("UPDATE items SET isImportant = :isImportant WHERE id = :id")
    suspend fun setImportant(id: Long, isImportant: Boolean)

    @Query("UPDATE items SET isDone = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markDone(id: Long, completedAt: Long)

    /** 完了の取り消し。元の並び位置のまま未完了に戻す */
    @Query("UPDATE items SET isDone = 0, completedAt = NULL WHERE id = :id")
    suspend fun markUndone(id: Long)

    /** 履歴から再追加。未完了リストの末尾に戻す */
    @Query(
        """
        UPDATE items SET isDone = 0, completedAt = NULL,
            sortOrder = (SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM items
                         WHERE listId = (SELECT listId FROM items WHERE id = :id))
        WHERE id = :id
        """,
    )
    suspend fun restoreToEnd(id: Long)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM items WHERE listId = :listId AND isDone = 1")
    suspend fun clearHistory(listId: Long)
}
