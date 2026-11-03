package com.oriteam.ori.ui.app.screens.account

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.profile.ProfilePhotoStore
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** The picture of whoever is using the app: what the profile shows and how it changes. */
@HiltViewModel
class ProfilePhotoViewModel @Inject constructor(private val photos: ProfilePhotoStore) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val _errorRes = MutableStateFlow<Int?>(null)

    /** Why the last picture could not be used. */
    val errorRes: StateFlow<Int?> = _errorRes.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val photoPath: StateFlow<String?> = uid.filterNotNull()
        .flatMapLatest(photos::observePhoto)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun show(profileUid: String) {
        uid.value = profileUid
    }

    fun onPhotoPicked(sourceUri: String) {
        val owner = uid.value ?: return
        _errorRes.value = null
        viewModelScope.launch {
            @StringRes val error = photos.setPhoto(owner, sourceUri)?.messageRes()
            _errorRes.value = error
        }
    }

    fun onRemovePhoto() {
        val owner = uid.value ?: return
        _errorRes.value = null
        viewModelScope.launch { photos.removePhoto(owner) }
    }
}
