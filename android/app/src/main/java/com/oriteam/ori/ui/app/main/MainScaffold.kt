package com.oriteam.ori.ui.app.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.app.screens.aac.AacRoute
import com.oriteam.ori.ui.app.screens.account.ProfileActions
import com.oriteam.ori.ui.app.screens.account.ProfileRoute
import com.oriteam.ori.ui.app.screens.reports.ReportsRoute
import com.oriteam.ori.ui.app.screens.routine.RoutineRoute
import com.oriteam.ori.ui.app.screens.routine.TasksRoute
import com.oriteam.ori.ui.app.screens.today.TodayRoute
import com.oriteam.ori.ui.components.navigation.NavigationDestination
import com.oriteam.ori.ui.components.navigation.OriNavigationScaffold
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen

@Composable
fun MainScaffold(profile: UserProfile, startArea: StartArea, profileActions: ProfileActions) {
    // Only the first value counts: changing the preference does not move someone out of the screen.
    var selectedName by rememberSaveable { mutableStateOf(startDestinationFor(startArea).name) }
    val homeSubscreen = rememberOpenSubscreen<HomeSubscreen>()
    val selected = MainDestination.valueOf(selectedName)
    val subscreen = homeSubscreen.current.takeIf { selected == MainDestination.HOME }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            when (selected) {
                MainDestination.HOME -> {
                    when (subscreen) {
                        null -> TodayRoute(
                            profile = profile,
                            onOpenRoutine = { selectedName = MainDestination.ROUTINE.name },
                            onOpenCommunication = { selectedName = MainDestination.COMMUNICATE.name },
                            onOpenSubscreen = homeSubscreen::open
                        )
                        HomeSubscreen.TASKS -> TasksRoute(ownerUid = profile.uid, onNavigateBack = closeSubscreen)
                        HomeSubscreen.REPORTS -> ReportsRoute(ownerUid = profile.uid, onNavigateBack = closeSubscreen)
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
