/* Version Museum is deferred beyond 2.0. Keep the implementation for a later release.
package com.roinur.saucetracker.feature.experimentalgallery

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roinur.saucetracker.DashboardMetricGlyph
import com.roinur.saucetracker.DashboardMetricGlyphIcon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class MuseumVersion(val version: String, val date: String, val caption: String, val screenshots: List<String>)

class VersionMuseumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExperimentalGalleryApp { VersionMuseum(::finish) } }
    }
}

@Composable
private fun VersionMuseum(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versions = remember {
        val root = JSONObject(context.assets.open("version-museum.json").bufferedReader().use { it.readText() })
        val array = root.getJSONArray("versions")
        List(array.length()) { index ->
            val item = array.getJSONObject(index); val images = item.getJSONArray("screenshots")
            MuseumVersion(item.getString("version"), item.getString("date"), item.getString("caption"), List(images.length()) { images.getString(it) })
        }
    }
    var expanded by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    GalleryToolScaffold("Version Museum", onClose) { insets ->
    LazyColumn(Modifier.fillMaxSize().padding(insets).padding(horizontal = 12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetricGlyphIcon(DashboardMetricGlyph.HISTORY, Modifier.size(40.dp))
                    Text("From 1.0 to 2.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("A look back at Sauce Tracker. Tap a version to explore its screenshots.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("The moon / sun / auto selector has stayed with us. Today's Legacy mode represents roughly the 1.5 era, not an exact 1.0 reconstruction.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(versions, key = { it.version }) { version ->
            val open = expanded == version.version
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().clickable { expanded = if (open) null else version.version }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(version.version, Modifier.padding(14.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(version.caption, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(version.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (open) "−" else "+", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                }
                if (open) version.screenshots.forEach { path ->
                    MuseumAssetImage(path, Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(16.dp)).clickable { selectedImage = path }, fullResolution = false)
                }
            }
        }
    }
    }
    selectedImage?.let { path ->
        Dialog(onDismissRequest = { selectedImage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxWidth(0.94f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { selectedImage = null }, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                    MuseumAssetImage(path, Modifier.fillMaxWidth().weight(1f, fill = false).clip(RoundedCornerShape(16.dp)), fullResolution = true)
                }
            }
        }
    }
}

@Composable
private fun MuseumAssetImage(path: String, modifier: Modifier, fullResolution: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, path, fullResolution) {
        value = withContext(Dispatchers.IO) { runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = if (fullResolution) 1 else 4 }) } }.getOrNull() }
    }
    bitmap?.let { Image(it.asImageBitmap(), contentDescription = "Historical Sauce Tracker screenshot", modifier = modifier, contentScale = ContentScale.Fit) }
}
*/
