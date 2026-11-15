package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.contentLanguage
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.list.rememberGridReorderState
import com.oriteam.ori.ui.components.motion.itemAnimation
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val PhraseMinWidth = 150.dp
private const val SAVED_PHRASE_EMOJI = "⭐"
private const val SAVED_HEADER_KEY = "saved-header"

/** The floating "Parar" with its margins. */
private val StopRoom = OriDimensions.ControlMinHeight + OriSpacing.Small * 2

/**
 * "Frases prontas", what the screen opens with: one tap says a whole sentence, with nothing to
 * build. They come with the app and need neither network nor storage, so they work on the very first
 * opening, even if the person's own saved phrases fail to load. The picture helps who does not read.
 */
@Composable
fun AacPhrasesPane(
    saved: ContentState<List<SavedMessage>>,
    speechState: SpeechState,
    /** The text and its language: a saved phrase's, or null (the app's) for a ready one. */
    onSay: (text: String, language: String?) -> Unit,
    onStop: () -> Unit,
    onDeleteSaved: (SavedMessage) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onChangeEmoji: (SavedMessage) -> Unit = {},
    /** The phrases that come with the app, in the order the person chose. */
    ready: List<ReadyPhrase> = readyPhrases,
    /** The phrase and the place it goes to among the saved ones; null when the order cannot be changed here. */
    onMoveSaved: ((messageId: String, toIndex: Int) -> Unit)? = null,
    /** The same for the phrases that come with the app, among themselves. */
    onMoveReady: ((phraseId: String, toIndex: Int) -> Unit)? = null
) {
    val fontScale = LocalDensity.current.fontScale
    val motion = LocalOriAppearance.current.motion
    val gridState = rememberLazyGridState()
    // The saved phrases and the ready ones are moved each among themselves, by a drag of their own.
    val drag = rememberGridReorderState<SavedMessage>(gridState) { it.id }
    val readyDrag = rememberGridReorderState(gridState, ::readyKey)
    val stored = (saved as? ContentState.Content)?.value.orEmpty()
    // While a phrase is dragged the list shows the order being built; otherwise, as read.
    val shown = drag.shown(stored)
    val shownReady = readyDrag.shown(ready)
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(PhraseMinWidth * fontScale),
                horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
                // Room for "Parar" at the end, always: it floats over the grid instead of shrinking it.
                contentPadding = PaddingValues(bottom = StopRoom),
                modifier = Modifier.weight(1f)
            ) {
                when (saved) {
                    is ContentState.Content -> {
                        item(key = SAVED_HEADER_KEY, span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(
                                text = stringResource(R.string.aac_saved_messages),
                                modifier = Modifier.itemAnimation(this, motion)
                            )
                        }
                        items(shown, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { message ->
                            // The phrase carried or sliding into its place is moved by the drag alone. Both go on
                            // the card itself, the item of the grid: above the other items, and one node to read.
                            val isMoving = drag.isMoving(message)
                            val placement = if (isMoving) {
                                Modifier
                            } else {
                                Modifier.itemAnimation(this, motion)
                            }
                            val moves = if (onMoveSaved == null) {
                                Modifier
                            } else {
                                Modifier.movable(message, message.text, shown, stored, drag) { moved, toIndex ->
                                    onMoveSaved(moved.id, toIndex)
                                }
                            }
                            PhraseTile(
                                modifier = placement.then(moves),
                                text = message.text,
                                emoji = message.emoji ?: SAVED_PHRASE_EMOJI,
                                onClick = { onSay(message.text, message.contentLanguage) },
                                trailing = {
                                    ChangeEmoji(message, onChangeEmoji)
                                    DeleteSaved(message, onDeleteSaved)
                                }
                            )
                        }
                    }
                    is ContentState.Error -> item(span = { GridItemSpan(maxLineSpan) }) { SavedError(saved, onRetry) }
                    ContentState.Empty, ContentState.Loading -> Unit
                }
                // The saved phrases arrive a moment after the screen opens and go above these: they slide down
                // instead of jumping.
                items(shownReady, key = ::readyKey) { phrase ->
                    val text = stringResource(phrase.textRes)
                    val placement = if (readyDrag.isMoving(phrase)) Modifier else Modifier.itemAnimation(this, motion)
                    val moves = if (onMoveReady == null) {
                        Modifier
                    } else {
                        Modifier.movable(phrase, text, shownReady, ready, readyDrag) { moved, toIndex ->
                            onMoveReady(moved.id, toIndex)
                        }
                    }
                    PhraseTile(
                        modifier = placement.then(moves),
                        text = text,
                        emoji = phrase.emoji,
                        onClick = { onSay(text, null) }
                    )
                }
                if (saved == ContentState.Empty) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.aac_saved_empty_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = OriSpacing.Small)
                        )
                    }
                }
            }
            if (speechState == SpeechState.UNAVAILABLE) {
                OriNotice(
                    stringResource(R.string.aac_speech_unavailable),
                    isLive = true
                )
            }
        }
        if (speechState == SpeechState.SPEAKING) StopSpeaking(onStop, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ChangeEmoji(message: SavedMessage, onChange: (SavedMessage) -> Unit) {
    IconButton(onClick = { onChange(message) }) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.aac_saved_emoji_change, message.text)
        )
    }
}

@Composable
private fun DeleteSaved(message: SavedMessage, onDelete: (SavedMessage) -> Unit) {
    IconButton(onClick = { onDelete(message) }) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.aac_delete_saved_message, message.text)
        )
    }
}

/** The failure stays visible, and the ready-made phrases below it keep working. */
@Composable
private fun SavedError(error: ContentState.Error, onRetry: () -> Unit) {
    Column {
        Text(text = stringResource(error.messageRes), style = MaterialTheme.typography.bodyMedium)
        if (error.canRetry) {
            OriTextButton(text = stringResource(R.string.action_retry), onClick = onRetry)
        }
    }
}

@Composable
private fun StopSpeaking(onStop: () -> Unit, modifier: Modifier) {
    OriPrimaryButton(
        text = stringResource(R.string.aac_stop),
        onClick = onStop,
        // As wide as a form: across a tablet it was a bar, not a button.
        modifier = modifier
            .padding(vertical = OriSpacing.Small)
            .widthIn(max = OriDimensions.FormMaxWidth)
            .fillMaxWidth()
    )
}

/** The key of a ready phrase in the grid, apart from the saved ones (their ids never have a colon). */
private fun readyKey(phrase: ReadyPhrase): Any = "ready:${phrase.id}"
