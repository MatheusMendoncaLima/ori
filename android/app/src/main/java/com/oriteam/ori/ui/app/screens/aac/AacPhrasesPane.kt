package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val PhraseMinWidth = 150.dp
private val PhrasePictureSize = 32.dp
private const val SAVED_PHRASE_EMOJI = "⭐"

/**
 * "Frases prontas", what the screen opens with: one tap says a whole sentence, with nothing to
 * build. They come with the app and need neither network nor storage, so they work on the very first
 * opening, even if the person's own saved phrases fail to load. The picture helps who does not read.
 */
@Composable
fun AacPhrasesPane(
    saved: ContentState<List<SavedMessage>>,
    speechState: SpeechState,
    onSay: (String) -> Unit,
    onStop: () -> Unit,
    onDeleteSaved: (SavedMessage) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    Column(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(PhraseMinWidth * fontScale),
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            modifier = Modifier.weight(1f)
        ) {
            when (saved) {
                is ContentState.Content -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(text = stringResource(R.string.aac_saved_messages))
                    }
                    items(saved.value, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { message ->
                        PhraseTile(
                            text = message.text,
                            emoji = SAVED_PHRASE_EMOJI,
                            onClick = { onSay(message.text) },
                            trailing = { DeleteSaved(message, onDeleteSaved) }
                        )
                    }
                }
                is ContentState.Error -> item(span = { GridItemSpan(maxLineSpan) }) { SavedError(saved, onRetry) }
                ContentState.Empty, ContentState.Loading -> Unit
            }
            items(readyPhrases, key = { it.textRes }) { phrase ->
                val text = stringResource(phrase.textRes)
                PhraseTile(text = text, emoji = phrase.emoji, onClick = { onSay(text) })
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
        if (speechState == SpeechState.SPEAKING) StopSpeaking(onStop)
    }
}

@Composable
private fun PhraseTile(text: String, emoji: String, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    OriCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = OriDimensions.MinTouchTarget)
        ) {
            SymbolPicture(image = SymbolImageUi.Emoji(emoji), size = PhrasePictureSize)
            Spacer(modifier = Modifier.width(OriSpacing.Small))
            Text(text = text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            trailing()
        }
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
private fun StopSpeaking(onStop: () -> Unit) {
    OriPrimaryButton(
        text = stringResource(R.string.aac_stop),
        onClick = onStop,
        modifier = Modifier
            .padding(vertical = OriSpacing.Small)
            .fillMaxWidth()
    )
}
