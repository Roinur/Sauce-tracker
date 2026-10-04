package com.roinur.saucetracker.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roinur.saucetracker.R

@Composable
internal fun ChapterListHeader(
    count: Int,
    loading: Boolean = false,
    ascending: Boolean,
    onToggleOrder: () -> Unit,
    enabled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.titleSmall
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (loading) "Chapters" else "Chapters ($count)", Modifier.weight(1f),
            style = style, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onToggleOrder, enabled = enabled && !loading && count > 1,
            modifier = Modifier.size(40.dp).semantics {
                stateDescription = if (ascending) "Oldest first" else "Newest first"
            }) {
            Icon(painterResource(R.drawable.ic_chapter_sort_24),
                contentDescription = if (ascending) "Show newest chapters first" else "Show oldest chapters first",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}
