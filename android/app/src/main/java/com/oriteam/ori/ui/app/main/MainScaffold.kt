package com.oriteam.ori.ui.app.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.adaptive.NavigationLayout
import com.oriteam.ori.ui.app.screens.aac.AacRoute
import com.oriteam.ori.ui.app.screens.account.ProfileActions
import com.oriteam.ori.ui.app.screens.account.ProfileRoute
import com.oriteam.ori.ui.app.screens.reports.ReportsRoute
import com.oriteam.ori.ui.app.screens.routine.RoutineRoute
import com.oriteam.ori.ui.app.screens.routine.TasksRoute
import com.oriteam.ori.ui.app.screens.today.TodayRoute
import com.oriteam.ori.ui.components.navigation.NavigationDestination
import com.oriteam.ori.ui.components.navigation.OriNavigationScaffold
import com.oriteam.ori.ui.components.navigation.SubscreenHost
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriAppColors
import com.oriteam.ori.ui.theme.fadeThrough

@Composable
fun MainScaffold(
    profile: UserProfile,
    startArea: StartArea,
    profileActions: ProfileActions,
    areaRequest: AreaRequest? = null
) {
    OriAppColors { MainScreens(profile, startArea, profileActions, areaRequest) }
}

@Composable
private fun MainScreens(
    profile: UserProfile,
    startArea: StartArea,
    profileActions: ProfileActions,
    areaRequest: AreaRequest?
) {
    // Only the first value counts: changing the preference does not move someone out of the screen.
    var selectedName by rememberSaveable { mutableStateOf(startDestinationFor(startArea).name) }
    val homeSubscreen = rememberOpenSubscreen<HomeSubscreen>()
    LaunchedEffect(areaRequest) {
        val request = areaRequest ?: return@LaunchedEffect
        selectedName = startDestinationFor(request.area).name
        homeSubscreen.close()
    }
    val selected = MainDestination.valueOf(selectedName)
    val motion = LocalOriAppearance.current.motion
    val navigationItems = MainDestination.entries.map { destination ->
        NavigationDestination(
            key = destination.name,
            label = stringResource(destination.labelRes),
            icon = destination.icon
        )
    }
    val closeSubscreen = homeSubscreen::close

    OriNavigationScaffold(
        destinations = navigationItems,
        selectedKey = selected.name,
        onDestinationSelected = {
            selectedName = it.key
            homeSubscreen.close()
        }
    ) {
        // A bottom bar already stands above the system bar. With a rail or a drawer (tablets) nothing does:
        // the content keeps clear of the taskbar itself, or a button at its bottom ends up half under it.
        val clearSides = if (LocalWindowLayout.current.navigationLayout == NavigationLayout.BOTTOM_BAR) {
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        } else {
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(clearSides))
        ) {
            // The four areas are side by side, not one inside the other: the old one fades, the new one grows in.
            // Each one keeps its state (scroll, what was open) while another one is shown.
            val areaStates = rememberSaveableStateHolder()
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeThrough(motion).using(null) },
                label = "MainArea"
            ) { area ->
                areaStates.SaveableStateProvider(area.name) {
                    when (area) {
                        MainDestination.HOME -> SubscreenHost(homeSubscreen) { current ->
                            when (current) {
                                null -> TodayRoute(
                                    profile = profile,
                                    onOpenRoutine = { selectedName = MainDestination.ROUTINE.name },
                                    onOpenCommunication = { selectedName = MainDestination.COMMUNICATE.name },
                                    onOpenSubscreen = homeSubscreen::open
                                )
                                HomeSubscreen.TASKS -> TasksRoute(
                                    ownerUid = profile.uid,
                                    onNavigateBack = closeSubscreen
                                )
                                HomeSubscreen.REPORTS ->
                                    ReportsRoute(ownerUid = profile.uid, onNavigateBack = closeSubscreen)
                            }
                        }
                        MainDestination.ROUTINE -> RoutineRoute(
                            ownerUid = profile.uid,
                            viewer = RoutineViewer.Owner,
                            title = stringResource(R.string.main_my_routine)
                        )
                        MainDestination.COMMUNICATE -> AacRoute(ownerUid = profile.uid)
                        MainDestination.PROFILE -> ProfileRoute(profile = profile, actions = profileActions)
                    }
                }
            }
        }
    }
}
