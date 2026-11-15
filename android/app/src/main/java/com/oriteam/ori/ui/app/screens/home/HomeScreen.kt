package com.oriteam.ori.ui.app.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.ui.app.screens.login.LoginRoute
import com.oriteam.ori.ui.app.screens.register.RegisterRoute
import com.oriteam.ori.ui.components.navigation.AnimatedPagerIndicator
import com.oriteam.ori.ui.preview.OriPreview
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.launch

/** Pages of the welcome pager, in display order. The enum order defines the page index. */
private enum class HomePage {
    ONBOARDING,
    LOGIN,
    REGISTER
}

@Composable
fun HomeRoute(
    onContinueWithoutAccount: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onTimerFinished = viewModel::onTimerFinished,
        onIntroFinished = viewModel::onIntroFinished,
        onContinueWithoutAccount = onContinueWithoutAccount
    )
}

/**
 * The whole signed-out area: onboarding, login and registration as pages of one pager.
 *
 * This pager is the only navigation before signing in, so each screen exists once. After login
 * or registration nothing is navigated here: the session (OriApp) replaces this area by itself.
 */
@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    onTimerFinished: () -> Unit = {},
    onIntroFinished: () -> Unit = {},
    onContinueWithoutAccount: () -> Unit = {}
) {
    val pages = HomePage.entries
    val pagerState = rememberPagerState(initialPage = HomePage.ONBOARDING.ordinal) { pages.size }
    val scope = rememberCoroutineScope()
    val animatesScrolling = LocalOriAppearance.current.motion.animatesScrolling
    val goTo: (HomePage) -> Unit = { page ->
        scope.launch {
            if (animatesScrolling) {
                pagerState.animateScrollToPage(
                    page.ordinal
                )
            } else {
                pagerState.scrollToPage(page.ordinal)
            }
        }
    }

    // System back returns to the onboarding page before leaving the app.
    BackHandler(enabled = pagerState.currentPage != HomePage.ONBOARDING.ordinal) { goTo(HomePage.ONBOARDING) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            when (pages[pageIndex]) {
                HomePage.ONBOARDING -> OnboardingPage(
                    showOptions = state.showOptions,
                    playsIntro = state.playsIntro,
                    onTimerFinished = onTimerFinished,
                    onIntroFinished = onIntroFinished,
                    onSignIn = { goTo(HomePage.LOGIN) },
                    onCreateAccount = { goTo(HomePage.REGISTER) },
                    onContinueWithoutAccount = onContinueWithoutAccount
                )

                HomePage.LOGIN -> LoginRoute(
                    onLoginSuccess = {},
                    onNavigateToRegister = { goTo(HomePage.REGISTER) }
                )

                HomePage.REGISTER -> RegisterRoute(
                    onRegisterSuccess = {},
                    onNavigateToLogin = { goTo(HomePage.LOGIN) }
                )
            }
        }

        AnimatedPagerIndicator(
            pagerState = pagerState,
            pageCount = pages.size,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = OriSpacing.Large)
        )
    }
}

@OriPreview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
