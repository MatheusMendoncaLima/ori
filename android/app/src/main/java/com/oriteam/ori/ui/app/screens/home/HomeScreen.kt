
package com.oriteam.ori.ui.app.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.OriPreview
import com.oriteam.ori.R
import com.oriteam.ori.ui.app.screens.login.LoginRoute
import com.oriteam.ori.ui.theme.NunitoFontFamily
import com.oriteam.ori.ui.theme.NunitoSansFontFamily
import com.oriteam.ori.ui.theme.OriBackground
import com.oriteam.ori.ui.theme.OriDarkBlue
import com.oriteam.ori.ui.theme.OriDecorationArcThick
import com.oriteam.ori.ui.theme.OriDecorationArcThin
import com.oriteam.ori.ui.theme.OriIndicatorInactive
import com.oriteam.ori.ui.theme.OriSloganBlue
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE = 0
private const val LOGIN_PAGE = 1
private const val REGISTER_PAGE = 2
private const val PAGE_COUNT = 3

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onNavigateToRegister = onNavigateToRegister
    )
}

@OriPreview
@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    onNavigateToRegister: () -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = ONBOARDING_PAGE
    ) {
        PAGE_COUNT
    }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->

            when (page) {
                ONBOARDING_PAGE -> {
                    OnboardingHomePage()
                }

                LOGIN_PAGE -> {
                    LoginRoute(
                        onLoginSuccess = {
                        },
                        onNavigateToRegister = {
                            scope.launch {
                                pagerState.animateScrollToPage(REGISTER_PAGE)
                            }

                            onNavigateToRegister()
                        }
                    )
                }

                REGISTER_PAGE -> {
                    PlaceholderPage(
                        pageIndex = page
                    )
                }
            }
        }

        AnimatedPagerIndicator(
            pagerState = pagerState,
            pageCount = PAGE_COUNT,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        )
    }
}
@Composable
private fun OnboardingHomePage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriBackground)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val thinPath = Path().apply {
                moveTo(
                    width * 0.51f,
                    height
                )

                quadraticTo(
                    width * 0.74f,
                    height * 0.84f,
                    width,
                    height * 0.78f
                )
            }

            drawPath(
                path = thinPath,
                color = OriDecorationArcThin,
                style = Stroke(
                    width = 2.dp.toPx()
                )
            )
            val thickPath = Path().apply {
                moveTo(
                    width * 0.60f,
                    height + 20.dp.toPx()
                )

                quadraticTo(
                    width * 0.80f,
                    height * 0.87f,
                    width + 20.dp.toPx(),
                    height * 0.81f
                )
            }

            drawPath(
                path = thickPath,
                color = OriDecorationArcThick,
                style = Stroke(
                    width = 28.dp.toPx()
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.ori
                    ),
                    contentDescription = "Logo Ori",
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "ori",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 82.sp,
                    color = OriDarkBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "organizar • comunicar • cuidar",
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = OriSloganBlue,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp
                )
            }

            Text(
                text = "Seu Apoio. Sempre.",
                fontFamily = NunitoSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = OriDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    bottom = 20.dp
                )
            )
        }
    }
}

@Composable
private fun PlaceholderPage(
    pageIndex: Int
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tela $pageIndex - Registro",
            fontFamily = NunitoSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = OriDarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}
@Composable
fun AnimatedPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { iteration ->

            val isSelected =
                pagerState.currentPage == iteration

            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) {
                    OriDarkBlue
                } else {
                    OriIndicatorInactive
                },
                animationSpec = tween(
                    durationMillis = 300
                ),
                label = "IndicatorColor"
            )

            val animatedWidth by animateDpAsState(
                targetValue = if (isSelected) {
                    18.dp
                } else {
                    8.dp
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "IndicatorWidth"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(animatedWidth)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}