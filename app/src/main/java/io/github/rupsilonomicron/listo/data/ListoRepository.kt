package io.github.rupsilonomicron.listo.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.rupsilonomicron.listo.widget.ListoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * データ操作の窓口。書き込みは画面のライフサイクルに左右されないよう
 * アプリ全体のスコープで実行し、終わったらウィジェットを更新する。
 */
class ListoRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dao = ListoDatabase.get(appContext).dao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeListSummaries() = dao.observeListSummaries()
    fun observeLists() = dao.observeLists()
    fun observeList(id: Long) = dao.observeList(id)
    fun observeActiveItems(listId: Long) = dao.observeActiveItems(listId)
    fun observeDoneItems(listId: Long) = dao.observeDoneItems(listId)

    // ---- リスト ----

    suspend fun createListNow(name: String): Long {
        val id = dao.insertList(
            ListEntity(name = name, sortOrder = dao.maxListSortOrder() + 1, createdAt = now()),
        )
        refreshWidgets()
        return id
    }

    fun createList(name: String) = write { dao.insertList(ListEntity(name = name, sortOrder = dao.maxListSortOrder() + 1, createdAt = now())) }

    fun renameList(id: Long, name: String) = write { dao.renameList(id, name) }

    fun deleteList(id: Long) = write { dao.deleteListWithItems(id) }

    fun moveList(id: Long, up: Boolean) = write {
        val lists = dao.getLists().toMutableList()
        val from = lists.indexOfFirst { it.id == id }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to !in lists.indices) return@write
        lists.add(to, lists.removeAt(from))
        dao.updateLists(lists.mapIndexed { index, list -> list.copy(sortOrder = index) })
    }

    // ---- 項目 ----

    fun addItem(listId: Long, title: String, isImportant: Boolean) = write {
        dao.insertItem(
            ItemEntity(
                listId = listId,
                title = title,
                isImportant = isImportant,
                sortOrder = dao.maxItemSortOrder(listId) + 1,
                createdAt = now(),
            ),
        )
    }

    fun updateItem(id: Long, title: String, isImportant: Boolean) = write { dao.updateItemContent(id, title, isImportant) }

    fun setImportant(id: Long, isImportant: Boolean) = write { dao.setImportant(id, isImportant) }

    fun complete(id: Long) = write { dao.markDone(id, now()) }

    /** ウィジェットのアクションから呼ぶ。完了まで待つ */
    suspend fun completeNow(id: Long) {
        dao.markDone(id, now())
        refreshWidgets()
    }

    fun undoComplete(id: Long) = write { dao.markUndone(id) }

    fun restoreFromHistory(id: Long) = write { dao.restoreToEnd(id) }

    fun deleteItem(id: Long) = write { dao.deleteItem(id) }

    fun clearHistory(listId: Long) = write { dao.clearHistory(listId) }

    private fun write(block: suspend () -> Unit) {
        scope.launch {
            block()
            refreshWidgets()
        }
    }

    private suspend fun refreshWidgets() {
        ListoWidget().updateAll(appContext)
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        @Volatile
        private var instance: ListoRepository? = null

        fun get(context: Context): ListoRepository =
            instance ?: synchronized(this) {
                instance ?: ListoRepository(context).also { instance = it }
            }
    }
}
