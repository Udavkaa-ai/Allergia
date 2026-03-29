package com.allergia.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.allergia.ui.viewmodels.PhotoArchiveViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoArchiveScreen(
    onBack: () -> Unit,
    viewModel: PhotoArchiveViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsState()
    var fullscreenPhoto by remember { mutableStateOf<File?>(null) }
    var photoToDelete by remember { mutableStateOf<File?>(null) }

    // Group photos by date (from filename food_YYYYMMDD_...)
    val byDate = remember(photos) {
        photos.groupBy { file ->
            val name = file.nameWithoutExtension  // food_YYYYMMDD_HHmmss
            val parts = name.split("_")
            if (parts.size >= 2) {
                runCatching {
                    val raw = parts[1]  // YYYYMMDD
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(raw)
                        ?.let { SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(it) }
                        ?: raw
                }.getOrDefault(parts[1])
            } else file.name
        }.entries.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фотоархив еды") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))
                    Text("Нет сохранённых фотографий", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("Фото сохраняются при распознавании еды", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                byDate.forEach { (date, files) ->
                    item(key = "header_$date", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(files, key = { it.absolutePath }) { file ->
                        PhotoThumb(
                            file = file,
                            onClick = { fullscreenPhoto = file },
                            onLongClick = { photoToDelete = file }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen viewer
    fullscreenPhoto?.let { file ->
        FullscreenPhotoDialog(
            file = file,
            onDismiss = { fullscreenPhoto = null },
            onDelete = {
                fullscreenPhoto = null
                viewModel.deletePhoto(file)
            }
        )
    }

    // Delete confirmation
    photoToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Удалить фото?") },
            text = { Text("Файл будет удалён без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePhoto(file); photoToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Удалить")
                }
            },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text("Отмена") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumb(
    file: File,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenPhotoDialog(
    file: File,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(file).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, "Удалить", tint = Color.White)
                }
                FilledIconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, "Закрыть", tint = Color.White)
                }
            }
        }
    }
}
