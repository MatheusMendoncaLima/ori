package com.oriteam.ori.ui.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Width of the area from which list and detail are shown side by side. */
private val TwoPaneMinWidth = 720.dp

/**
 * List and detail decided by the space this area receives, not by the window, so it also works inside
 * the detail pane of another list (the routine of a person inside "Acompanhados"). The caller keeps what
 * is chosen, so it can also open or close the detail itself (after saving, after deleting).
 *
 * - Wide area: both side by side; with nothing chosen ([hasDetail] false), the detail shows [emptyDetail].
 * - Narrow area: the list, or the detail alone; the system back gesture calls [onBack].
 *
 * [OriListDetailLayout] does the same for a whole screen, where the window decides and the hinge of a
 * foldable is avoided.
 */
@Composable
fun AreaListDetailLayout(
    hasDetail: Boolean,
    onBack: () -> Unit,
    list: @Composable (Modifier) -> Unit,
    detail: @Composable (onNavigateBack: (() -> Unit)?) -> Unit,
    emptyDetail: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when {
            maxWidth >= TwoPaneMinWidth -> Row(modifier = Modifier.fillMaxSize()) {
                list(Modifier.weight(1f))
                VerticalDivider()
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Beside the list there is nowhere to go back to.
                    if (hasDetail) detail(null) else emptyDetail()
                }
            }
            !hasDetail -> list(Modifier.fillMaxSize())
            else -> {
                BackHandler(onBack = onBack)
                detail(onBack)
            }
        }
    }
}
