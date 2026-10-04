package com.roinur.saucetracker.feature.qr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import com.roinur.saucetracker.feature.experimentalgallery.ExperimentalGalleryApp
import com.roinur.saucetracker.feature.experimentalgallery.GalleryToolScaffold
import com.roinur.saucetracker.feature.browser.RemoteThumbnail
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.roinur.saucetracker.core.preferences.KEY_INCOGNITO_MODE_ENABLED
import com.roinur.saucetracker.core.preferences.SaucePreferences
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.profile.ProfileEntryState
import com.roinur.saucetracker.data.profile.ProfileStore
import com.roinur.saucetracker.data.source.ProfileSourceEntry
import com.roinur.saucetracker.data.source.SourceEntryStore
import com.roinur.saucetracker.data.source.SourceRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

class QrShareActivity : ComponentActivity() {
    private lateinit var preferences: SaucePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = SaucePreferences.from(this)
        if (preferences.boolean(KEY_INCOGNITO_MODE_ENABLED)) {
            finish()
            return
        }
        setContent { ExperimentalGalleryApp { QrShareScreen(::finish, ::shareBitmap) } }
    }

    override fun onResume() {
        super.onResume()
        if (::preferences.isInitialized && preferences.boolean(KEY_INCOGNITO_MODE_ENABLED)) finish()
    }

    private fun shareBitmap(bitmap: Bitmap) {
        val directory = File(cacheDir, "qr-share").apply { mkdirs() }
        val target = File(directory, "sauce-tracker-qr.png")
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", target)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share Sauce Tracker QR"))
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, QrShareActivity::class.java)
    }
}

private data class ImportSummary(var added: Int = 0, var alreadyPresent: Int = 0, var updated: Int = 0, var unsupported: Int = 0, var failed: Int = 0) {
    override fun toString(): String = "Added $added · already present $alreadyPresent · updated $updated · unsupported $unsupported · failed $failed"
}

@Composable
private fun QrShareScreen(onClose: () -> Unit, onShare: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { SauceTrackerDatabase(context.applicationContext) }
    val profileStore = remember { ProfileStore(database) }
    val sourceStore = remember { SourceEntryStore(database) }
    val registry = remember { SourceRegistry.createDefault() }
    val profileId = remember { profileStore.activeProfileId() }
    var library by remember { mutableStateOf<List<ProfileSourceEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var generated by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<QrSharePackage?>(null) }
    var applySharedStatus by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Select up to 10 entries, or scan a shared QR.") }
    var cameraPending by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var knownEntries by remember { mutableStateOf<Map<String, ProfileSourceEntry>>(emptyMap()) }
    var visibleLimit by remember { mutableStateOf(100) }
    var hasMoreEntries by remember { mutableStateOf(false) }
    val cameraUri = remember {
        val directory = File(context.cacheDir, "qr-camera").apply { mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.files", File(directory, "captured-qr.jpg"))
    }

    fun acceptBitmap(bitmap: Bitmap?) {
        if (bitmap == null) { status = "No QR image received."; return }
        scope.launch {
        when (val decoded = withContext(Dispatchers.Default) { decodeQrBitmap(bitmap) }) {
            is QrDecodeResult.Invalid -> { preview = null; status = decoded.reason }
            is QrDecodeResult.Success -> { preview = decoded.value; generated = null; status = "Review ${decoded.value.entries.size} entries before importing." }
        }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (!captured) acceptBitmap(null) else scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeBoundedBitmap(context, cameraUri) }
            acceptBitmap(bitmap)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && cameraPending) camera.launch(cameraUri) else if (!granted) status = "Camera permission was not granted. You can still choose a saved image."
        cameraPending = false
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { uri?.let { decodeBoundedBitmap(context, it) } }
            acceptBitmap(bitmap)
        }
    }

    LaunchedEffect(profileId, query, visibleLimit) {
        kotlinx.coroutines.delay(180)
        val loaded = withContext(Dispatchers.IO) { sourceStore.allEntries(profileId, text = query, maxEntries = visibleLimit + 1) }
        hasMoreEntries = loaded.size > visibleLimit
        library = loaded.take(visibleLimit)
        knownEntries = knownEntries.filterKeys { it in selected } + library.associateBy { it.entry.key.storageKey }
    }

    GalleryToolScaffold("QR Share", onClose) { insets ->
    LazyColumn(Modifier.fillMaxSize().padding(insets).padding(horizontal = 12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Share a little collection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Up to 10 entries. Read status, ratings, pins and local tags are included — share only what you want others to see.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(cameraUri)
                    else { cameraPending = true; permission.launch(Manifest.permission.CAMERA) }
                }, modifier = Modifier.weight(1f)) { Text("Camera") }
                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Saved image") }
            }
        }
        if (preview != null) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Import preview", style = MaterialTheme.typography.titleLarge)
                        preview!!.entries.forEach { shared ->
                            val supported = registry.adapter(shared.key.sourceId) != null
                            Text("${shared.key.sourceId.value} ${shared.key.displayId} · ${shared.previewTitle.ifBlank { "Untitled" }}${if (supported) "" else " · unsupported"}")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = applySharedStatus, onCheckedChange = { applySharedStatus = it })
                            Text("Apply shared read, rating and pin", Modifier.padding(start = 8.dp))
                        }
                        Button(onClick = {
                            val packageToImport = preview ?: return@Button
                            scope.launch {
                                busy = true
                                status = "Importing…"
                                val summary = withContext(Dispatchers.IO) { importPackage(packageToImport, applySharedStatus, profileId, profileStore, sourceStore, registry) }
                                val loaded = withContext(Dispatchers.IO) { sourceStore.allEntries(profileId, text = query, maxEntries = visibleLimit + 1) }
                                hasMoreEntries = loaded.size > visibleLimit
                                library = loaded.take(visibleLimit)
                                knownEntries = library.associateBy { it.entry.key.storageKey }
                                selected = emptySet(); preview = null; status = summary.toString()
                                busy = false
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Importing…" else "Import approved entries") }
                        OutlinedButton(onClick = { preview = null }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                    }
                }
            }
        } else {
            item {
                Button(onClick = {
                    val chosen = selected.mapNotNull { knownEntries[it] }
                    scope.launch {
                        runCatching {
                            val shared = withContext(Dispatchers.IO) {
                                QrSharePackage(chosen.map { item -> QrSharedEntry(item.entry.key, item.entry.canonicalUrl, item.entry.title, item.state.isRead, item.state.rating, item.state.pinned, sourceStore.localTags(profileId, item.entry.key)) })
                            }
                            withContext(Dispatchers.Default) { generateQrBitmap(QrShareCodec.encode(shared)) }
                        }.onSuccess { generated = it; status = "QR ready. Share or save it through Android's share sheet." }
                            .onFailure { status = it.message ?: "Could not create QR." }
                    }
                }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Create QR (${selected.size}/10)") }
            }
            generated?.let { bitmap ->
                item {
                    Image(bitmap.asImageBitmap(), "Generated Sauce Tracker QR", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    Button(onClick = { onShare(bitmap) }, modifier = Modifier.fillMaxWidth()) { Text("Share QR image") }
                }
            }
            item { OutlinedTextField(value = query, onValueChange = { query = it; visibleLimit = 100 }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(24.dp), placeholder = { Text("Search entries, tags or artists…") }) }
            items(library, key = { it.entry.key.storageKey }) { item ->
                val key = item.entry.key.storageKey
                val checked = key in selected
                Card(Modifier.fillMaxWidth().clickable(enabled = checked || selected.size < QrShareCodec.MAX_ENTRIES) {
                    selected = if (checked) selected - key else selected + key
                    generated = null
                }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, onCheckedChange = null)
                        RemoteThumbnail(urls = listOf(item.entry.thumbnailUrl), contentDescription = item.entry.title, onClick = {
                            if (checked || selected.size < QrShareCodec.MAX_ENTRIES) { selected = if (checked) selected - key else selected + key; generated = null }
                        }, modifier = Modifier.size(52.dp, 68.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(item.entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (item.entry.key.sourceId.value == "mangadex") "MangaDex" else "NHentai ${item.entry.key.displayId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (hasMoreEntries) item {
                OutlinedButton(onClick = { visibleLimit += 100 }, modifier = Modifier.fillMaxWidth()) { Text("Show more entries") }
            }
        }
    }
    }
}

private fun importPackage(value: QrSharePackage, applyStatus: Boolean, profileId: String, profiles: ProfileStore, store: SourceEntryStore, registry: SourceRegistry): ImportSummary {
    val summary = ImportSummary()
    val allowed = profiles.profile(profileId)?.sourceIds.orEmpty()
    value.entries.forEach { shared ->
        val adapter = registry.adapter(shared.key.sourceId)
        if (adapter == null || shared.key.sourceId !in allowed) { summary.unsupported += 1; return@forEach }
        runCatching {
            val previous = profiles.state(profileId, shared.key)
            if (previous != null) {
                if (applyStatus) {
                    store.updateState(profileId, shared.key, previous.isRead || shared.read, if (previous.rating > 0) previous.rating else shared.rating, previous.pinned || shared.pinned)
                    summary.updated += 1
                } else summary.alreadyPresent += 1
                store.mergeLocalTags(profileId, shared.key, shared.localTags)
            } else {
                val entry = store.entry(shared.key) ?: adapter.fetchEntry(shared.key.remoteId)
                val now = Instant.now().toString()
                val incoming = if (applyStatus) ProfileEntryState(profileId, shared.key.sourceId, shared.key.remoteId, shared.read, shared.rating, shared.pinned, 0, "", now, now) else null
                store.upsert(entry, profileId, incoming)
                store.mergeLocalTags(profileId, shared.key, shared.localTags)
                summary.added += 1
            }
        }.onFailure { summary.failed += 1 }
    }
    return summary
}

private fun generateQrBitmap(content: String, size: Int = 1080): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(com.google.zxing.EncodeHintType.MARGIN to 2))
    val pixels = IntArray(size * size)
    for (y in 0 until size) for (x in 0 until size) pixels[y * size + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, size, 0, 0, size, size) }
}

private fun decodeQrBitmap(bitmap: Bitmap): QrDecodeResult = runCatching {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    val text = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))).text
    QrShareCodec.decode(text)
}.getOrElse { QrDecodeResult.Invalid("No valid Sauce Tracker QR was found in the image.") }

private fun decodeBoundedBitmap(context: Context, uri: android.net.Uri, maxDimension: Int = 2400): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxDimension * 2 || bounds.outHeight / sample > maxDimension * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}
