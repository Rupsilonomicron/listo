package io.github.rupsilonomicron.listo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rupsilonomicron.listo.data.ItemEntity
import io.github.rupsilonomicron.listo.data.ListEntity
import io.github.rupsilonomicron.listo.ui.theme.LocalImportantColors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 読み込み中（null）と「リストが存在しない」を区別するための包み */
private data class LoadedList(val list: ListEntity?)

private val completedAtFormatter = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(listId: Long, onBack: () -> Unit) {
    val repository = rememberRepository()
    val loaded by remember(listId) { repository.observeList(listId).map { LoadedList(it) } }
        .collectAsStateWithLifecycle(initialValue = null)
    val activeItems by remember(listId) { repository.observeActiveItems(listId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val doneItems by remember(listId) { repository.observeDoneItems(listId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // ウィジェットから削除済みのリストを開いた場合など
    LaunchedEffect(loaded) {
        if (loaded != null && loaded?.list == null) onBack()
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showSnackbar(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loaded?.list?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    if (tab == 1 && doneItems.isNotEmpty()) {
                        TextButton(onClick = { confirmClearHistory = true }) { Text("履歴を消去") }
                    }
                },
            )
        },
        bottomBar = {
            if (tab == 0) {
                AddItemBar(onAdd = { title, important -> repository.addItem(listId, title, important) })
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("未完了 (${activeItems.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("履歴 (${doneItems.size})") })
            }
            if (tab == 0) {
                if (activeItems.isEmpty()) {
                    EmptyState(
                        title = "未完了の項目はありません",
                        message = if (doneItems.isEmpty()) "下の入力欄から項目を追加しましょう" else "「履歴」タブから以前の項目を再追加できます",
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                        items(activeItems, key = { it.id }) { item ->
                            Column(Modifier.animateItem()) {
                                ActiveItemRow(
                                    item = item,
                                    onComplete = {
                                        repository.complete(item.id)
                                        showSnackbar("「${item.title}」を完了にしました", "元に戻す") {
                                            repository.undoComplete(item.id)
                                        }
                                    },
                                    onToggleImportant = { repository.setImportant(item.id, !item.isImportant) },
                                    onEdit = { editing = item },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            } else {
                if (doneItems.isEmpty()) {
                    EmptyState(
                        title = "履歴はありません",
                        message = "完了にした項目がここに残り、タップひとつで再追加できます",
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                        items(doneItems, key = { it.id }) { item ->
                            Column(Modifier.animateItem()) {
                                HistoryItemRow(
                                    item = item,
                                    onRestore = {
                                        repository.restoreFromHistory(item.id)
                                        showSnackbar("「${item.title}」を再追加しました")
                                    },
                                    onDelete = { repository.deleteItem(item.id) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { item ->
        ItemEditDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = { title, important ->
                editing = null
                repository.updateItem(item.id, title, important)
            },
            onDelete = {
                editing = null
                repository.deleteItem(item.id)
            },
        )
    }
    if (confirmClearHistory) {
        ConfirmDialog(
            title = "履歴を消去",
            message = "このリストの履歴 ${doneItems.size} 件をすべて削除します。元に戻せません。",
            confirmLabel = "消去",
            onDismiss = { confirmClearHistory = false },
            onConfirm = {
                confirmClearHistory = false
                repository.clearHistory(listId)
            },
        )
    }
}

@Composable
private fun ActiveItemRow(
    item: ItemEntity,
    onComplete: () -> Unit,
    onToggleImportant: () -> Unit,
    onEdit: () -> Unit,
) {
    val importantColors = LocalImportantColors.current
    Surface(
        onClick = onEdit,
        color = if (item.isImportant) importantColors.container else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (item.isImportant) importantColors.accent else Color.Transparent),
            )
            Checkbox(checked = false, onCheckedChange = { onComplete() })
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isImportant) FontWeight.Bold else null,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
            IconButton(onClick = onToggleImportant) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = if (item.isImportant) "重要を解除" else "重要にする",
                    tint = if (item.isImportant) importantColors.accent else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryItemRow(item: ItemEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isImportant) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "重要",
                        tint = LocalImportantColors.current.accent,
                        modifier = Modifier.size(16.dp).padding(end = 2.dp),
                    )
                }
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
            }
            item.completedAt?.let {
                Text(
                    "${completedAtFormatter.format(Instant.ofEpochMilli(it))} に完了",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRestore) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("再追加")
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "履歴から削除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddItemBar(onAdd: (title: String, important: Boolean) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var important by rememberSaveable { mutableStateOf(false) }
    val importantColors = LocalImportantColors.current
    val submit = {
        val title = text.trim()
        if (title.isNotEmpty()) {
            onAdd(title, important)
            text = ""
            important = false
        }
    }
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconToggleButton(checked = important, onCheckedChange = { important = it }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = if (important) "重要：オン" else "重要：オフ",
                    tint = if (important) importantColors.accent else MaterialTheme.colorScheme.outline,
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("項目を追加") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = submit, enabled = text.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = "追加")
            }
        }
    }
}

@Composable
private fun ItemEditDialog(
    item: ItemEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, important: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(item.title) }
    var important by remember { mutableStateOf(item.isImportant) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("項目を編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("重要", modifier = Modifier.weight(1f))
                    Switch(checked = important, onCheckedChange = { important = it })
                }
                TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("この項目を削除", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim(), important) }, enabled = title.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
