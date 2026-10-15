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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rupsilonomicron.listo.data.GroupEntity
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
    val groups by remember(listId) { repository.observeGroups(listId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val activeItems by remember(listId) { repository.observeActiveItems(listId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val doneItems by remember(listId) { repository.observeDoneItems(listId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // ウィジェットから削除済みのリストを開いた場合など
    LaunchedEffect(loaded) {
        if (loaded != null && loaded?.list == null) onBack()
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    // 入力欄から追加する先のグループ。null ならグループなし
    var targetGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    val targetGroup = groups.firstOrNull { it.id == targetGroupId }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }
    var creatingGroup by remember { mutableStateOf(false) }
    var renamingGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var deletingGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    val inputFocus = remember { FocusRequester() }
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
                    if (tab == 0) {
                        TextButton(onClick = { creatingGroup = true }) { Text("グループ追加") }
                    } else if (doneItems.isNotEmpty()) {
                        TextButton(onClick = { confirmClearHistory = true }) { Text("履歴を消去") }
                    }
                },
            )
        },
        bottomBar = {
            if (tab == 0) {
                AddItemBar(
                    groups = groups,
                    targetGroup = targetGroup,
                    onTargetChange = { targetGroupId = it },
                    onCreateGroup = { creatingGroup = true },
                    focusRequester = inputFocus,
                    onAdd = { title, important -> repository.addItem(listId, targetGroup?.id, title, important) },
                )
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
                if (activeItems.isEmpty() && groups.isEmpty()) {
                    EmptyState(
                        title = "未完了の項目はありません",
                        message = if (doneItems.isEmpty()) {
                            "下の入力欄から項目を追加できます\n売り場ごとに分けたいときは\n右上の「グループ追加」から"
                        } else {
                            "「履歴」タブから\n以前の項目を再追加できます"
                        },
                    )
                } else {
                    ActiveItemsList(
                        groups = groups,
                        activeItems = activeItems,
                        onComplete = { item ->
                            repository.complete(item.id)
                            showSnackbar("「${item.title}」を完了にしました", "元に戻す") {
                                repository.undoComplete(item.id)
                            }
                        },
                        onToggleImportant = { repository.setImportant(it.id, !it.isImportant) },
                        onEdit = { editing = it },
                        onAddToGroup = { group ->
                            targetGroupId = group.id
                            inputFocus.requestFocus()
                        },
                        onRenameGroup = { renamingGroup = it },
                        onMoveGroup = { group, up -> repository.moveGroup(group.id, up) },
                        onDeleteGroup = { deletingGroup = it },
                    )
                }
            } else {
                if (doneItems.isEmpty()) {
                    EmptyState(
                        title = "履歴はありません",
                        message = "完了にした項目はここに残り\n「再追加」で未完了に戻せます",
                    )
                } else {
                    val groupNames = groups.associate { it.id to it.name }
                    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                        items(doneItems, key = { it.id }) { item ->
                            Column(Modifier.animateItem()) {
                                HistoryItemRow(
                                    item = item,
                                    groupName = item.groupId?.let { groupNames[it] },
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
            groups = groups,
            onDismiss = { editing = null },
            onSave = { title, important, groupId ->
                editing = null
                repository.updateItem(item.id, title, important, groupId)
            },
            onDelete = {
                editing = null
                repository.deleteItem(item.id)
            },
        )
    }
    if (creatingGroup) {
        TextInputDialog(
            title = "新しいグループ",
            label = "グループ名（例：スーパーマーケット）",
            confirmLabel = "作成",
            onDismiss = { creatingGroup = false },
            onConfirm = { name ->
                creatingGroup = false
                repository.createGroup(listId, name)
            },
        )
    }
    renamingGroup?.let { group ->
        TextInputDialog(
            title = "グループ名を変更",
            label = "グループ名",
            confirmLabel = "変更",
            initial = group.name,
            onDismiss = { renamingGroup = null },
            onConfirm = { name ->
                renamingGroup = null
                repository.renameGroup(group.id, name)
            },
        )
    }
    deletingGroup?.let { group ->
        val count = activeItems.count { it.groupId == group.id } + doneItems.count { it.groupId == group.id }
        ConfirmDialog(
            title = "「${group.name}」を削除",
            message = if (count == 0) {
                "このグループを削除します。"
            } else {
                "グループ内の項目 $count 件（履歴を含む）も削除されます。元に戻せません。"
            },
            confirmLabel = "削除",
            onDismiss = { deletingGroup = null },
            onConfirm = {
                deletingGroup = null
                if (targetGroupId == group.id) targetGroupId = null
                repository.deleteGroup(group.id)
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

/** グループなしの項目を先頭に、続いてグループごとに見出し付きで並べる */
@Composable
private fun ActiveItemsList(
    groups: List<GroupEntity>,
    activeItems: List<ItemEntity>,
    onComplete: (ItemEntity) -> Unit,
    onToggleImportant: (ItemEntity) -> Unit,
    onEdit: (ItemEntity) -> Unit,
    onAddToGroup: (GroupEntity) -> Unit,
    onRenameGroup: (GroupEntity) -> Unit,
    onMoveGroup: (GroupEntity, up: Boolean) -> Unit,
    onDeleteGroup: (GroupEntity) -> Unit,
) {
    val groupIds = groups.map { it.id }.toSet()
    val ungrouped = activeItems.filter { it.groupId == null || it.groupId !in groupIds }
    val itemsByGroup = activeItems.groupBy { it.groupId }

    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
        fun itemRows(rows: List<ItemEntity>) {
            items(rows, key = { it.id }) { item ->
                Column(Modifier.animateItem()) {
                    ActiveItemRow(
                        item = item,
                        onComplete = { onComplete(item) },
                        onToggleImportant = { onToggleImportant(item) },
                        onEdit = { onEdit(item) },
                    )
                    HorizontalDivider()
                }
            }
        }

        itemRows(ungrouped)
        groups.forEachIndexed { index, group ->
            val rows = itemsByGroup[group.id].orEmpty()
            item(key = "group-${group.id}") {
                GroupHeader(
                    group = group,
                    count = rows.size,
                    canMoveUp = index > 0,
                    canMoveDown = index < groups.lastIndex,
                    onAdd = { onAddToGroup(group) },
                    onRename = { onRenameGroup(group) },
                    onMove = { up -> onMoveGroup(group, up) },
                    onDelete = { onDeleteGroup(group) },
                    modifier = Modifier.animateItem(),
                )
            }
            itemRows(rows)
        }
    }
}

@Composable
private fun GroupHeader(
    group: GroupEntity,
    count: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onAdd: () -> Unit,
    onRename: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                group.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                "  $count 件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "「${group.name}」に追加", tint = MaterialTheme.colorScheme.primary)
            }
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "グループのメニュー")
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
private fun HistoryItemRow(item: ItemEntity, groupName: String?, onRestore: () -> Unit, onDelete: () -> Unit) {
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
            val completed = item.completedAt?.let { "${completedAtFormatter.format(Instant.ofEpochMilli(it))} に完了" }
            val subtitle = listOfNotNull(groupName, completed).joinToString("　・　")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
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

/** グループ選択用のボタン＋メニュー。追加先の切り替えと項目編集で共用する */
@Composable
private fun GroupPicker(
    groups: List<GroupEntity>,
    selected: GroupEntity?,
    onSelect: (Long?) -> Unit,
    prefix: String,
    onCreateGroup: (() -> Unit)? = null,
) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        TextButton(onClick = { expanded = true }) {
            Text(
                "$prefix${selected?.name ?: "グループなし"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("グループなし") }, onClick = { expanded = false; onSelect(null) })
            groups.forEach { group ->
                DropdownMenuItem(text = { Text(group.name) }, onClick = { expanded = false; onSelect(group.id) })
            }
            if (onCreateGroup != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("新しいグループを作成…") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { expanded = false; onCreateGroup() },
                )
            }
        }
    }
}

@Composable
private fun AddItemBar(
    groups: List<GroupEntity>,
    targetGroup: GroupEntity?,
    onTargetChange: (Long?) -> Unit,
    onCreateGroup: () -> Unit,
    focusRequester: FocusRequester,
    onAdd: (title: String, important: Boolean) -> Unit,
) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (groups.isNotEmpty()) {
                GroupPicker(
                    groups = groups,
                    selected = targetGroup,
                    onSelect = onTargetChange,
                    prefix = "追加先：",
                    onCreateGroup = onCreateGroup,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    // 追加先は上の「追加先：〇〇」で示す。入力欄のヒントは入力済みの文字と見間違えないよう淡くする
                    placeholder = { Text("項目名を入力", color = MaterialTheme.colorScheme.outline) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
                IconButton(onClick = submit, enabled = text.isNotBlank()) {
                    Icon(Icons.Filled.Add, contentDescription = "追加")
                }
            }
        }
    }
}

@Composable
private fun ItemEditDialog(
    item: ItemEntity,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, important: Boolean, groupId: Long?) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(item.title) }
    var important by remember { mutableStateOf(item.isImportant) }
    var groupId by remember { mutableStateOf(item.groupId) }
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
                if (groups.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("グループ", modifier = Modifier.weight(1f))
                        GroupPicker(
                            groups = groups,
                            selected = groups.firstOrNull { it.id == groupId },
                            onSelect = { groupId = it },
                            prefix = "",
                        )
                    }
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.padding(top = 16.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("この項目を削除", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim(), important, groupId) }, enabled = title.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
