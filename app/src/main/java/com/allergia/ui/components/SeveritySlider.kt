package com.allergia.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SeverityRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(120.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            val labels = listOf("Нет", "Слабо", "Умер.", "Сильно")
            val colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                Color(0xFFFFE082),
                Color(0xFFFFA726),
                Color(0xFFE53935)
            )
            labels.forEachIndexed { index, lbl ->
                FilterChip(
                    selected = value == index,
                    onClick = { onValueChange(index) },
                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors[index],
                        selectedLabelColor = if (index == 3) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AllergenicityBadge(score: Float, modifier: Modifier = Modifier) {
    val (color, label) = when {
        score >= 0.75f -> Color(0xFFE53935) to "Высокий"
        score >= 0.5f -> Color(0xFFFFA726) to "Средний"
        score >= 0.25f -> Color(0xFFFFEE58) to "Низкий"
        else -> Color(0xFF66BB6A) to "Мин."
    }
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = "${(score * 100).toInt()}% $label",
            style = MaterialTheme.typography.labelSmall,
            color = if (score >= 0.75f) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
