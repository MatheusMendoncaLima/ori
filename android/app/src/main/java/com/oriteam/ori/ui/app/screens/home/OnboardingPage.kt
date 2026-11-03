package com.oriteam.ori.ui.app.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.FillOrScrollColumn
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.brand.OriBrandBlock
import com.oriteam.ori.ui.components.brand.OriDecorativeArcs
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.delay

/** How long the tagline stays on screen before the account options appear. */
private const val TAGLINE_DURATION_MILLIS = 1_500L

/** The account options rise from the bottom of the screen. */
private const val OPTIONS_ENTER_MILLIS = 450
private const val TAGLINE_EXIT_MILLIS = 150

/** Space kept free at the bottom for the pager indicator. */
private val PagerIndicatorClearance = 48.dp

/**
 * First pager page: logo, slogan and, after [TAGLINE_DURATION_MILLIS], the account options.
 *
 * Tall windows stack brand and options; short windows (landscape phone) place them side by
 * side. Both arrangements scroll when enlarged text does not fit.
 */
@Composable
internal fun OnboardingPage(
    showOptions: Boolean,
    onTimerFinished: () -> Unit,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueWithoutAccount: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!showOptions) {
            delay(TAGLINE_DURATION_MILLIS)
            onTimerFinished()
        }
    }

    val bottomContent: @Composable () -> Unit = {
        OnboardingBottom(
            showOptions = showOptions,
            onSignIn = onSignIn,
            onCreateAccount = onCreateAccount,
            onContinueWithoutAccount = onContinueWithoutAccount
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OriDecorativeArcs()
        val contentModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(bottom = PagerIndicatorClearance)

        if (LocalWindowLayout.current.prefersHorizontalArrangement) {
            Row(modifier = contentModifier, verticalAlignment = Alignment.CenterVertically) {
                FillOrScrollColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) { OriBrandBlock(logoHeight = 96.dp) }
                FillOrScrollColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) { CenteredContent { bottomContent() } }
            }
        } else {
            FillOrScrollColumn(modifier = contentModifier, verticalArrangement = Arrangement.SpaceBetween) {
                Spacer(modifier = Modifier.height(OriSpacing.ExtraLarge))
                OriBrandBlock()
                CenteredContent { bottomContent() }
            }
        }
    }
}

@Composable
private fun OnboardingBottom(
    showOptions: Boolean,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueWithoutAccount: () -> Unit
) {
    val motion = LocalOriAppearance.current.motion
    AnimatedContent(
        targetState = showOptions,
        transitionSpec = {
            val enter = slideInVertically(motion.decorative(OPTIONS_ENTER_MILLIS)) { height -> height } +
                fadeIn(motion.decorative(OPTIONS_ENTER_MILLIS))
            (enter togetherWith fadeOut(motion.decorative(TAGLINE_EXIT_MILLIS))).using(SizeTransform(clip = false))
        },
        label = "OnboardingBottomOptions"
    ) { isShowingOptions ->
        if (isShowingOptions) {
            AccountOptions(onSignIn, onCreateAccount, onContinueWithoutAccount)
        } else {
            Text(
                text = stringResource(R.string.onboarding_tagline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = OriSpacing.Large)
            )
        }
    }
}

@Composable
private fun AccountOptions(onSignIn: () -> Unit, onCreateAccount: () -> Unit, onContinueWithoutAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = OriSpacing.Small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)
    ) {
        OriPrimaryButton(text = stringResource(R.string.onboarding_sign_in), onClick = onSignIn)
        OriPrimaryButton(text = stringResource(R.string.onboarding_create_account), onClick = onCreateAccount)
        OriSecondaryButton(
            text = stringResource(R.string.onboarding_continue_without_account),
            onClick = onContinueWithoutAccount
        )
    }
}
