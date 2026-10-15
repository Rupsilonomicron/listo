package io.github.rupsilonomicron.listo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.ActionParameters
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.rupsilonomicron.listo.MainActivity
import io.github.rupsilonomicron.listo.R
import io.github.rupsilonomicron.listo.data.GroupEntity
import io.github.rupsilonomicron.listo.data.ItemEntity
import io.github.rupsilonomicron.listo.data.ListEntity
import io.github.rupsilonomicron.listo.data.ListoRepository
import kotlinx.coroutines.flow.combine

class ListoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ListoWidget()
}

private data class WidgetData(val list: ListEntity?, val groups: List<GroupEntity>, val items: List<ItemEntity>)

/** ウィジェットの1行：グループ見出しか項目 */
private sealed interface WidgetRow {
    val key: Long

    data class Header(val group: GroupEntity) : WidgetRow {
        override val key get() = -group.id // 項目 id と衝突しないよう負数にする
    }

    data class Item(val item: ItemEntity) : WidgetRow {
        override val key get() = item.id
    }
}

/** グループなしの項目を先頭に、続いて項目のあるグループだけを見出し付きで並べる */
private fun WidgetData.rows(): List<WidgetRow> {
    val groupIds = groups.map { it.id }.toSet()
    val byGroup = items.groupBy { it.groupId }
    return buildList {
        items.filter { it.groupId == null || it.groupId !in groupIds }.forEach { add(WidgetRow.Item(it)) }
        groups.forEach { group ->
            val groupItems = byGroup[group.id].orEmpty()
            if (groupItems.isNotEmpty()) {
                add(WidgetRow.Header(group))
                groupItems.forEach { add(WidgetRow.Item(it)) }
            }
        }
    }
}

// アプリ本体の重要色（Theme.kt）と揃える
private val importantAccent = ColorProvider(day = Color(0xFFC62828), night = Color(0xFFFF8A80))
private val importantContainer = ColorProvider(day = Color(0xFFFFEBEE), night = Color(0xFF4A2326))

class ListoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ListoRepository.get(context)
        provideContent {
            GlanceTheme {
                val listId = currentState(LIST_ID_KEY)
                if (listId == null) {
                    NotConfigured()
                } else {
                    // Room の Flow を購読しているので、表示中の変更はそのまま反映される
                    val data by remember(listId) {
                        combine(
                            repository.observeList(listId),
                            repository.observeGroups(listId),
                            repository.observeActiveItems(listId),
                        ) { list, groups, items -> WidgetData(list, groups, items) }
                    }.collectAsState(initial = null)
                    Content(listId, data)
                }
            }
        }
    }

    companion object {
        val LIST_ID_KEY = longPreferencesKey("listId")
    }
}

@Composable
private fun WidgetFrame(content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        content()
    }
}

@Composable
private fun NotConfigured() {
    WidgetFrame {
        Text(
            "ウィジェットを長押しして、表示するリストを選んでください",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            modifier = GlanceModifier.padding(8.dp),
        )
    }
}

@Composable
private fun Content(listId: Long, data: WidgetData?) {
    val context = LocalContext.current
    val openList = actionStartActivity(MainActivity.openListIntent(context, listId))
    WidgetFrame {
        when {
            data == null -> Unit
            data.list == null -> Text(
                "リストが削除されました。ウィジェットを長押しして再設定してください",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                modifier = GlanceModifier.padding(8.dp),
            )
            else -> {
                Header(data.list.name, data.items.size, openList)
                if (data.items.isEmpty()) {
                    Text(
                        "未完了の項目はありません",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        modifier = GlanceModifier.fillMaxWidth().padding(8.dp).clickable(openList),
                    )
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(data.rows(), itemId = { it.key }) { row ->
                            when (row) {
                                is WidgetRow.Header -> GroupHeaderRow(row.group.name, openList)
                                is WidgetRow.Item -> Column {
                                    ItemRow(row.item, openList)
                                    Spacer(GlanceModifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(name: String, count: Int, openList: Action) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).clickable(openList),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            "$count 件",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

@Composable
private fun GroupHeaderRow(name: String, openList: Action) {
    Text(
        name,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        modifier = GlanceModifier.fillMaxWidth().padding(start = 6.dp, top = 6.dp, bottom = 2.dp).clickable(openList),
    )
}

@Composable
private fun ItemRow(item: ItemEntity, openList: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(8.dp)
            .background(if (item.isImportant) importantContainer else GlanceTheme.colors.widgetBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .padding(6.dp)
                .clickable(
                    actionRunCallback<CompleteItemAction>(
                        actionParametersOf(CompleteItemAction.ITEM_ID_KEY to item.id),
                    ),
                ),
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_checkbox),
                contentDescription = "「${item.title}」を完了にする",
                colorFilter = ColorFilter.tint(if (item.isImportant) importantAccent else GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(22.dp),
            )
        }
        Spacer(GlanceModifier.width(2.dp))
        Text(
            item.title,
            maxLines = 2,
            style = TextStyle(
                color = if (item.isImportant) importantAccent else GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = if (item.isImportant) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = GlanceModifier.defaultWeight().padding(vertical = 6.dp).clickable(openList),
        )
    }
}

class CompleteItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ITEM_ID_KEY] ?: return
        ListoRepository.get(context).completeNow(itemId)
    }

    companion object {
        val ITEM_ID_KEY = ActionParameters.Key<Long>("itemId")
    }
}
