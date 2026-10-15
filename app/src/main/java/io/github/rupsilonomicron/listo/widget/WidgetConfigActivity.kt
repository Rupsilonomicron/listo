package io.github.rupsilonomicron.listo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.rupsilonomicron.listo.ui.TextInputDialog
import io.github.rupsilonomicron.listo.ui.rememberRepository
import io.github.rupsilonomicron.listo.ui.theme.ListoTheme
import kotlinx.coroutines.launch

/** ウィジェット設置時（および再設定時）に表示するリストを選ぶ画面 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        // 選択せずに閉じた場合は設置をキャンセル扱いにする
        setResult(RESULT_CANCELED, resultIntent)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            ListoTheme {
                WidgetConfigScreen(
                    onSelect = { listId ->
                        lifecycleScope.launch {
                            val context = this@WidgetConfigActivity
                            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                            updateAppWidgetState(context, glanceId) { prefs ->
                                prefs[ListoWidget.LIST_ID_KEY] = listId
                            }
                            ListoWidget().update(context, glanceId)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(onSelect: (Long) -> Unit, onCancel: () -> Unit) {
    val repository = rememberRepository()
    val lists by remember { repository.observeLists() }.collectAsStateWithLifecycle(initialValue = emptyList())
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("表示するリストを選択") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "キャンセル") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lists, key = { it.id }) { list ->
                Card(onClick = { onSelect(list.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        list.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            item {
                OutlinedButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新しいリストを作成して選択")
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
                scope.launch { onSelect(repository.createListNow(name)) }
            },
        )
    }
}
