package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.adaptive.windowLayoutFor
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.OriTheme

/** Above the symbols, the message never takes more than this share of the height. */
private const val MESSAGE_BAR_MAX_HEIGHT_SHARE = 0.5f

/**
 * A communication board: the message being built ([messageBar]), what to choose from ([content]:
 * symbols or saved messages) and the actions on the message, within reach of the thumb ([actions]).
 *
 * In short, wide windows (landscape phone) the message moves to a side column so the symbols
 * keep most of the height; otherwise it stays above them. With enlarged text or the text field
 * open the bar may need more room than it can have: it scrolls, so the symbols never disappear.
 */
@Composable
fun AacBoardLayout(
    messageBar: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    if (LocalWindowLayout.current.prefersHorizontalArrangement) {
        Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(OriSpacing.Medium)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // The keyboard would cover the text field here: the column shrinks and scrolls to it.
                    .windowInsetsPadding(WindowInsets.ime)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)
            ) {
                messageBar(Modifier.fillMaxWidth())
                CenteredActions(actions)
            }
            content(Modifier.weight(2f))
        }
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val maxBarHeight = maxHeight * MESSAGE_BAR_MAX_HEIGHT_SHARE
            Column(modifier = Modifier.fillMaxSize()) {
                messageBar(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxBarHeight)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                content(Modifier.weight(1f))
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                CenteredActions(actions)
                Spacer(modifier = Modifier.height(OriSpacing.Small))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun AacBoardLandscapePreview() {
    val symbols = listOf("Eu" to "🙋", "Quero" to "👉", "Água" to "💧", "Comer" to "🍽️", "Ajuda" to "🆘").map {
        AacSymbolUiModel(id = it.first, label = it.first, image = SymbolImageUi.Emoji(it.second))
    }
    OriTheme(windowLayout = windowLayoutFor(widthDp = 800, heightDp = 360)) {
        AacBoardLayout(
            messageBar = {
                AacMessageBar(
                    message = symbols.take(2),
                    speechState = SpeechState.READY,
                    isFull = false,
                    actions = AacMessageBarActions.None,
                    modifier = it
                )
            },
            content = {
                AacBoardGrid(
                    symbols = symbols,
                    preferences = AacPreferences(),
                    onSymbolClick = {},
                    onClickLabel = "",
                    modifier = it
                )
            },
            modifier = Modifier.size(800.dp, 360.dp)
        )
    }
}

/** The actions keep a comfortable width and stay in the middle, instead of hugging the left of a wide screen. */
@Composable
private fun CenteredActions(actions: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { actions() }
}
