package com.example.ecolim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ecolim.data.preferences.ServerConfigManager
import com.example.ecolim.ui.gallery.GalleryViewModel
import com.example.ecolim.ui.home.HomeViewModel
import com.example.ecolim.ui.slideshow.SlideshowViewModel

class ViewModelFactory(private val serverConfigManager: ServerConfigManager) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(serverConfigManager) as T
            GalleryViewModel::class.java -> GalleryViewModel(serverConfigManager) as T
            SlideshowViewModel::class.java -> SlideshowViewModel(serverConfigManager) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}