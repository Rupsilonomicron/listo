package io.github.rupsilonomicron.listo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rupsilonomicron.listo.data.ListEntity
import io.github.rupsilonomicron.listo.data.ListSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(onOpenList: (Long) -> Unit) {
    val repository = rememberRepository()
    val summaries by remember { repository.observeListSummaries() }
        .collectAsStateWithLifecycle(initialValue = null)

    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ListEntity?>(null) }
    var deleting by remember { mutableStateOf<ListEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Listo") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("リストを作成") },
            )
        },
    ) { padding ->
        val data = summaries
        when {
            data == null -> Unit
            data.isEmpty() -> EmptyState(
                title = "リストがありません",
                message = "右下のボタンから\n「買い物」「今週のタスク」などの\nリストを作成しましょう",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(data, key = { _, s -> s.list.id }) { index, summary ->
                    ListCard(
                        summary = summary,
                        canMoveUp = index > 0,
                        canMoveDown = index < data.lastIndex,
                        onClick = { onOpenList(summary.list.id) },
                        onRename = { renaming = summary.list },
                        onMove = { up -> repository.moveList(summary.list.id, up) },
                        onDelete = { deleting = summary.list },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (creating) {
        TextInputDialog(
            title = "新しいリスト",
            label = "リスト名",
            confirmLabel = "作成",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                repository.createList(name)
            },
        )
    }
    renaming?.let { list ->
        TextInputDialog(
            title = "名前を変更",
            label = "リスト名",
            confirmLabel = "変更",
            initial = list.name,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                repository.renameList(list.id, name)
            },
        )
    }
    deleting?.let { list ->
        ConfirmDialog(
            title = "「${list.name}」を削除",
            message = "リスト内の項目と履歴もすべて削除されます。元に戻せません。",
            confirmLabel = "削除",
            onDismiss = { deleting = null },
            onConfirm = {
                deleting = null
                repository.deleteList(list.id)
            },
        )
    }
}

@Composable
private fun ListCard(
    summary: ListSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    summary.list.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (summary.activeCount == 0) "未完了なし" else "未完了 ${summary.activeCount} 件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "メニュー")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("名前を変更") }, onClick = { expanded = false; onRename() })
                    if (canMoveUp) {
                        DropdownMenuItem(text = { Text("上へ移動") }, onClick = { expanded = false; onMove(true) })
                    }
                    if (canMoveDown) {
                        DropdownMenuItem(text = { Text("下へ移動") }, onClick = { expanded = false; onMove(false) })
                    }
                    DropdownMenuItem(
                        text = { Text("削除", color = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false; onDelete() },
                    )
                }
            }
        }
    }
}
