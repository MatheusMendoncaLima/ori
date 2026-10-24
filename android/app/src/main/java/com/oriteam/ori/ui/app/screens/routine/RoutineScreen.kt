package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.OriPreview
@Composable
fun RoutineRoute(
    viewModel: RoutineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoutineScreen(state = state)
}
@OriPreview
@Composable
fun RoutineScreen(state: RoutineUiState = RoutineUiState()) {
    Text(text = "Routine Screen")
}