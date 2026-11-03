package com.oriteam.ori.ui.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * The pill tabs of the wireframes ("Pendentes / Concluídas / Todas"). When enlarged text does not fit,
 * a tab moves to the next line instead of breaking words, and no tab is ever hidden off the screen.
 */
@Composable
fun <T> OriFilterTabs(
    tabs: List<OriFilterTab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        tabs.forEach { tab ->
            OriChoiceChip(
                label = tab.label,
                isSelected = tab.value == selected,
                onClick = { onSelect(tab.value) },
                role = Role.Tab
            )
        }
    }
}
