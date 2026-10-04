package com.roinur.saucetracker.feature.experimentalgallery

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/** The same chrome as Experimental Gallery, shared by its companion tools. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryToolScaffold(title: String, onClose: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = { TextButton(onClick = onClose) { Text("Close") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            ),
            windowInsets = WindowInsets(0)
        )
    }, content = content)
}
