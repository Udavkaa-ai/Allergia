package com.allergia.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allergia.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PhotoArchiveViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _photos = MutableStateFlow<List<File>>(emptyList())
    val photos: StateFlow<List<File>> = _photos.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _photos.value = ImageUtils.listFoodPhotos(context)
        }
    }

    fun deletePhoto(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            file.delete()
            _photos.value = ImageUtils.listFoodPhotos(context)
        }
    }
}
