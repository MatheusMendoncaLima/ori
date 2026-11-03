package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

private val MessagePictureSize = 28.dp

/**
 * The message being built, kept small because the symbols are the main part of the screen. Tapping
 * a piece removes it, and the "+" at its end opens the field to type a word that has no symbol.
 * Screen readers announce changes to the message (live region), so the person hears each symbol.
 */
@Composable
fun AacMessageBar(
    message: List<AacSymbolUiModel>,
    speechState: SpeechState,
    isFull: Boolean,
    actions: AacMessageBarActions,
    modifier: Modifier = Modifier
) {
    var isWriting by rememberSaveable { mutableStateOf(false) }
    var typedText by rememberSaveable { mutableStateOf("") }
    OriCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MessageLine(message = message, onRemovePart = actions.onRemovePart, modifier = Modifier.weight(1f))
            WriteToggle(isWriting = isWriting, onToggle = { isWriting = !isWriting })
        }
        if (isFull) OriNotice(stringResource(R.string.aac_message_full), isLive = true)
        if (speechState == SpeechState.UNAVAILABLE) {
            OriNotice(stringResource(R.string.aac_speech_unavailable), isLive = true)
        }
        if (isWriting) {
            TypedTextInput(
                text = typedText,
                onTextChange = { typedText = it },
                onAdd = actions.onTyped,
                onSaveWord = actions.onSaveWord,
                isEnabled = !isFull,
                modifier = Modifier.padding(top = OriSpacing.Small)
            )
        }
    }
}

@Composable
private fun MessageLine(message: List<AacSymbolUiModel>, onRemovePart: (index: Int) -> Unit, modifier: Modifier) {
    val messageText = message.flatMap { listOfNotNull(it.connector, it.label) }.joinToString(" ")
    val messageDescription = stringResource(R.string.aac_message_description, messageText)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .heightIn(min = OriDimensions.MinTouchTarget)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                if (message.isNotEmpty()) contentDescription = messageDescription
            }
    ) {
        if (message.isEmpty()) {
            Text(
                text = stringResource(R.string.aac_message_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            MessagePieces(message = message, onRemovePart = onRemovePart)
        }
    }
}

/**
 * Above the symbols the message keeps a single line, showing the newest piece: if it grew, the
 * symbols would move under the finger while the person builds the sentence. "…" marks the words
 * scrolled away. Beside the symbols
 * (landscape phone) nothing moves, so it wraps and stays fully visible.
 */
@Composable
private fun MessagePieces(message: List<AacSymbolUiModel>, onRemovePart: (index: Int) -> Unit) {
    val spacing = Arrangement.spacedBy(OriSpacing.Small)
    val pieces: @Composable () -> Unit = {
        message.forEachIndexed { index, part ->
            part.connector?.let { ConnectorWord(it) }
            MessagePiece(part, onClick = { onRemovePart(index) })
        }
    }
    if (LocalWindowLayout.current.prefersHorizontalArrangement) {
        FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) { pieces() }
    } else {
        val scrollState = rememberScrollState()
        val animatesScrolling = LocalOriAppearance.current.motion.animatesScrolling
        LaunchedEffect(scrollState, animatesScrolling) {
            // maxValue changes only after a new piece is measured; MAX_VALUE means "not measured yet".
            snapshotFlow { scrollState.maxValue }
                .filter { it != Int.MAX_VALUE }
                .collectLatest { if (animatesScrolling) scrollState.animateScrollTo(it) else scrollState.scrollTo(it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Earlier words are off to the left: "…" says so. "Mostrar" and screen readers give the whole message.
            if (scrollState.canScrollBackward) {
                Text(
                    text = "…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = OriSpacing.ExtraSmall)
                        .semantics { hideFromAccessibility() }
                )
            }
            Row(
                horizontalArrangement = spacing,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(scrollState)
            ) {
                pieces()
            }
        }
    }
}

/** The "+" of the wireframe: a word without a symbol can always be typed. */
@Composable
private fun WriteToggle(isWriting: Boolean, onToggle: () -> Unit) {
    val writingState = stringResource(if (isWriting) R.string.aac_write_open else R.string.aac_write_closed)
    val label = stringResource(R.string.aac_type_label)
    IconButton(
        onClick = onToggle,
        modifier = Modifier.semantics {
            contentDescription = label
            stateDescription = writingState
        }
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
    }
}

/** A word the app added, without a box: it was not tapped, so there is nothing to remove. */
@Composable
private fun ConnectorWord(word: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = OriDimensions.MinTouchTarget)) {
        Text(
            text = word,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessagePiece(part: AacSymbolUiModel, onClick: () -> Unit) {
    val removeLabel = stringResource(R.string.aac_remove_symbol, part.label)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .heightIn(min = OriDimensions.MinTouchTarget)
            .clearAndSetSemantics {
                contentDescription = removeLabel
                role = Role.Button
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            modifier = Modifier.padding(horizontal = OriSpacing.Small, vertical = OriSpacing.ExtraSmall)
        ) {
            part.image?.let { SymbolPicture(image = it, size = MessagePictureSize) }
            Text(text = part.label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
