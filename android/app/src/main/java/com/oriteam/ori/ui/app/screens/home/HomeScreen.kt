package com.oriteam.ori.ui.app.screens.home

import android.provider.SyncStateContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.oriteam.ori.OriPreview
import com.oriteam.ori.ui.app.screens.components.UiPaddingBox

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel(), onNavigateToRegister : ()-> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onNavigateToRegister)
}

@OriPreview
@Composable
fun HomeScreen(state : HomeUiState = HomeUiState(), onNavigateToRegister: () -> Unit = {}){
    UiPaddingBox {
        Column {
            Row {
            Text(text = state.helloWorld)
            }
            Row {
            Button(onClick = onNavigateToRegister) { }
            }
        }
    }
}