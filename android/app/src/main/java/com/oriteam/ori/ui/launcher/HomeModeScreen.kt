package com.oriteam.ori.ui.launcher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** "Tela inicial" in the settings of ORI: what ORI Home is, and the official way to turn it on or off. */
@Composable
fun HomeModeScreen(onNavigateBack: () -> Unit, viewModel: HomeModeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onRequestResult()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeModeViewModel.Event.RequestHome -> request.launch(homeRequestIntent(context))
                is HomeModeViewModel.Event.OpenPreviousLauncher -> openPreviousLauncher(context, event.packageName)
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(
            maxWidth = OriDimensions.FormMaxWidth,
            title = stringResource(R.string.home_mode_title),
            onNavigateBack = onNavigateBack
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                OriCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_mode_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(OriSpacing.Small))
                    Text(
                        text = stringResource(if (state.isOriHome) R.string.home_mode_on else R.string.home_mode_off),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FormMessage(
                    messageRes = state.messageRes,
                    type = FormMessageType.SUCCESS.takeIf {
                        state.isOriHome
                    } ?: FormMessageType.ERROR
                )
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                if (state.isOriHome) {
                    OriTextButton(text = stringResource(R.string.launcher_leave), onClick = viewModel::onDeactivate)
                } else {
                    OriPrimaryButton(
                        text = stringResource(R.string.home_mode_activate),
                        onClick = viewModel::onActivate
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                Text(
                    text = stringResource(R.string.home_mode_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
