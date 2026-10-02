package com.oriteam.ori.ui.app.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun UiPaddingBox(modifier : Modifier = Modifier, content : @Composable () -> Unit){
    Scaffold { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        Box(modifier = modifier) {
            content.invoke()
        }
    }
}