package com.oriteam.ori.ui.app.screens.carerecipients

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.app.screens.aac.AacOrganizeRoute
import com.oriteam.ori.ui.app.screens.routine.RoutineRoute
import com.oriteam.ori.ui.components.text.Text

private const val ROUTINE_TAB = 0
private const val COMMUNICATION_TAB = 1

/**
 * What a caregiver sees about one person. The communication tab exists only when the person
 * allowed it (`manageCommunication`); the rules deny the data otherwise, the tab only avoids
 * offering something that would fail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareRecipientDetail(link: CareLink, onNavigateBack: (() -> Unit)?) {
    var selectedTab by rememberSaveable(link.id) { mutableIntStateOf(ROUTINE_TAB) }
    val showsCommunication = link.permissions.manageCommunication
    val tab = if (showsCommunication) selectedTab else ROUTINE_TAB

    Column(modifier = Modifier.fillMaxSize()) {
        if (showsCommunication) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == ROUTINE_TAB,
                    onClick = { selectedTab = ROUTINE_TAB },
                    text = { Text(stringResource(R.string.care_recipient_tab_routine)) }
                )
                Tab(
                    selected = tab == COMMUNICATION_TAB,
                    onClick = { selectedTab = COMMUNICATION_TAB },
                    text = { Text(stringResource(R.string.care_recipient_tab_communication)) }
                )
            }
        }
        if (tab == COMMUNICATION_TAB) {
            AacOrganizeRoute(
                ownerUid = link.primaryUserUid,
                viewer = AacViewer.Caregiver(link.permissions),
                title = link.primaryUserName,
                onNavigateBack = onNavigateBack
            )
        } else {
            RoutineRoute(
                ownerUid = link.primaryUserUid,
                viewer = RoutineViewer.Caregiver(link.permissions),
                title = link.primaryUserName,
                onNavigateBack = onNavigateBack
            )
        }
    }
}
