package com.oriteam.ori.ui.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.components.motion.OriPaneTransition
import com.oriteam.ori.ui.components.navigation.LocalTopBarShowsTextCase
import com.oriteam.ori.ui.components.navigation.OriTopBarWithoutTitle
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.contentSwap

/** Width of the area from which list and detail are shown side by side. */
private val TwoPaneMinWidth = 720.dp

/**
 * False where list and detail are never side by side, whatever the width: the panel of ORI Home, a quick look
 * beside the home screen, where an empty half ("Escolha uma atividade") would only take room.
 */
val LocalAllowsTwoPanes = staticCompositionLocalOf { true }

/**
 * List and detail decided by the space this area receives, not by the window, so it also works inside
 * the detail pane of another list (the routine of a person inside "Acompanhados"). The caller keeps what
 * is chosen ([selection]), so it can also open or close the detail itself (after saving, after deleting).
 *
 * - Wide area (where [LocalAllowsTwoPanes] lets it): both side by side; with nothing chosen, the detail shows
 *   [emptyDetail]. Choosing another item cross-fades the detail.
 * - Narrow area: the list, or the detail alone, moving forward into it and back; the system back gesture
 *   calls [onBack].
 *
 * [detail] receives the selection it shows, so a detail on its way out keeps its content while it leaves.
 *
 * [OriListDetailLayout] does the same for a whole screen, where the window decides and the hinge of a
 * foldable is avoided.
 */
@Composable
fun <T : Any> AreaListDetailLayout(
    selection: T?,
    onBack: () -> Unit,
    list: @Composable (Modifier) -> Unit,
    detail: @Composable (shown: T, onNavigateBack: (() -> Unit)?) -> Unit,
    emptyDetail: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalOriAppearance.current.motion
    // In both layouts "back" closes what is open beside or over the list before leaving the area: with two
    // panes it used to leave the area at once, and an open form was lost without a word.
    BackHandler(enabled = selection != null, onBack = onBack)
    val allowsTwoPanes = LocalAllowsTwoPanes.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (allowsTwoPanes && maxWidth >= TwoPaneMinWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
                // One "Aa", at the right edge like on every screen: the detail's bar has it, the list's does not.
                CompositionLocalProvider(LocalTopBarShowsTextCase provides false) { list(Modifier.weight(1f)) }
                VerticalDivider()
                AnimatedContent(
                    targetState = selection,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    transitionSpec = { contentSwap(motion).using(null) },
                    label = "Detail"
                ) { shown ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Beside the list there is nowhere to go back to.
                        if (shown != null) {
                            detail(shown, null)
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                OriTopBarWithoutTitle()
                                emptyDetail()
                            }
                        }
                    }
                }
            }
        } else {
            OriPaneTransition(target = selection, isForward = { _, to -> to != null }, label = "ListDetail") { shown ->
                if (shown != null) detail(shown, onBack) else list(Modifier.fillMaxSize())
            }
        }
    }
}
