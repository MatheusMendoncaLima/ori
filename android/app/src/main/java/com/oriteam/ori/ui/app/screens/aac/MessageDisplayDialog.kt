package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.adaptive.WindowWidthClass
import com.oriteam.ori.ui.components.dialog.DialogPlacement
import com.oriteam.ori.ui.components.dialog.OriDialog
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * The message in very large letters, filling the screen, to show to someone. It is the fallback
 * when there is no voice, and it also helps in noisy places. The message sits in the middle of the
 * screen, larger where there is room; a long one scrolls. "Fechar" keeps the width of a form.
 */
@Composable
fun MessageDisplayDialog(text: String, onDismiss: () -> Unit) {
    val style = if (LocalWindowLayout.current.widthClass == WindowWidthClass.COMPACT) {
        MaterialTheme.typography.displayMedium
    } else {
        MaterialTheme.typography.displayLarge
    }
    OriDialog(
        onDismissRequest = onDismiss,
        paneTitle = stringResource(R.string.aac_show),
        placement = DialogPlacement.FULL_SCREEN
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(OriSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(OriSpacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // At least as tall as the free space, so a short message is centered and a long one scrolls.
                    val visibleHeight = maxHeight
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(
                            text = text,
                            style = style,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = visibleHeight)
                                .wrapContentHeight(Alignment.CenterVertically)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
                OriPrimaryButton(
                    text = stringResource(R.string.aac_display_close),
                    onClick = onDismiss,
                    modifier = Modifier.widthIn(max = OriDimensions.FormMaxWidth)
                )
            }
        }
    }
}
