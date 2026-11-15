package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.matchesSearch
import com.oriteam.ori.ui.components.dialog.OriAlertDialog
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** The icon of an app in the list: smaller than on the home screen, so many names fit. */
private val ListIconSize = 40.dp

/** What a list of apps with boxes is for: its title and what checking means. */
internal class ChecklistTexts(val title: String, val hint: String)

/**
 * A list of apps with a box each, as One UI's "Adicionar apps" of a folder and "Ocultar apps": [apps] in their order,
 * the ones of [checked] checked. Nothing changes until "Pronto" ([onDone], with the keys checked then, in the order
 * they were checked); "Cancelar" or back leaves everything as it was. A search finds an app by name, as in all the
 * apps.
 */
@Composable
internal fun AppChecklist(
    texts: ChecklistTexts,
    apps: List<LauncherApp>,
    checked: List<String>,
    isMonochrome: Boolean,
    onDone: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var chosen by rememberSaveable { mutableStateOf(checked) }
    var query by rememberSaveable { mutableStateOf("") }
    val found = remember(apps, query) { apps.filter { it.label.matchesSearch(query) } }
    OriAlertDialog(
        onDismissRequest = onDismiss,
        title = texts.title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
                Text(text = texts.hint, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    shape = CircleShape,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(text = stringResource(R.string.launcher_search_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn {
                    items(found, key = { it.key }) { app ->
                        ChecklistRow(app, isChecked = app.key in chosen, isMonochrome) { isChecked ->
                            chosen = if (isChecked) chosen + app.key else chosen - app.key
                        }
                    }
                }
            }
        },
        confirmButton = {
            OriTextButton(text = stringResource(R.string.launcher_done), onClick = { onDone(chosen) })
        },
        dismissButton = {
            OriTextButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        }
    )
}

/** One app of the list: its icon, its name and a box, read as one choice that is checked or not. */
@Composable
private fun ChecklistRow(app: LauncherApp, isChecked: Boolean, isMonochrome: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OriDimensions.MinTouchTarget)
            .toggleable(value = isChecked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = OriSpacing.ExtraSmall)
    ) {
        Checkbox(checked = isChecked, onCheckedChange = null)
        AppIcon(app = app, size = ListIconSize, isMonochrome = isMonochrome)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
