package io.github.rupsilonomicron.listo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.rupsilonomicron.listo.ui.ListDetailScreen
import io.github.rupsilonomicron.listo.ui.ListsScreen
import io.github.rupsilonomicron.listo.ui.theme.ListoTheme

class MainActivity : ComponentActivity() {

    /** ウィジェットなど外部から開くよう要求されたリスト */
    private var requestedListId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestedListId = intent.listIdExtra()
        setContent {
            ListoTheme {
                ListoApp(
                    requestedListId = requestedListId,
                    onRequestHandled = { requestedListId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedListId = intent.listIdExtra()
    }

    companion object {
        private const val EXTRA_LIST_ID = "io.github.rupsilonomicron.listo.LIST_ID"

        fun openListIntent(context: Context, listId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_LIST_ID, listId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        private fun Intent.listIdExtra(): Long? = getLongExtra(EXTRA_LIST_ID, -1L).takeIf { it > 0 }
    }
}

@Composable
private fun ListoApp(requestedListId: Long?, onRequestHandled: () -> Unit) {
    var openListId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(requestedListId) {
        if (requestedListId != null) {
            openListId = requestedListId
            onRequestHandled()
        }
    }

    val listId = openListId
    if (listId == null) {
        ListsScreen(onOpenList = { openListId = it })
    } else {
        BackHandler { openListId = null }
        ListDetailScreen(listId = listId, onBack = { openListId = null })
    }
}
