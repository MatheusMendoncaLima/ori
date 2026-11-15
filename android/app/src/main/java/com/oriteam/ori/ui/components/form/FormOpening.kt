package com.oriteam.ori.ui.components.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.UUID

/**
 * Names one opening of a form. A form's ViewModel outlives the form, so its saved draft may have been left
 * by an earlier opening closed with the system "back", which does not pass through Cancelar. The ViewModel
 * compares this name to its draft's: the same name is the same opening, back after a rotation, the system
 * closing the app or a visit to another area, and keeps its draft; a new name starts the form over.
 */
@Composable
fun rememberFormOpening(): String = rememberSaveable { UUID.randomUUID().toString() }
