package com.allergia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val severityColors = listOf(
    Color(0xFF9E9E9E), // 0 — Нет
    Color(0xFFFFE082), // 1 — Слабо
    Color(0xFFFFA726), // 2 — Умеренно
    Color(0xFFE53935)  // 3 — Сильно
)
private val severityLabels = listOf("Нет", "Слабо", "Умеренно", "Сильно")

/**
 * One severity row: label | ● ● ● ● | current-level text
 */
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
        // Label — fixed width
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(110.dp),
            maxLines = 1
        )

        // 4 coloured dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            severityColors.forEachIndexed { index, color ->
                val selected = value == index
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (selected) color else color.copy(alpha = 0.22f))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) color.copy(alpha = 0.8f) else color.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onValueChange(index) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Selected level name
        Text(
            text = severityLabels[value],
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (value > 0) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (value > 0) severityColors[value] else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
fun AllergenicityBadge(score: Float, modifier: Modifier = Modifier) {
    val (color, label) = when {
        score >= 0.75f -> Color(0xFFE53935) to "Высокий"
        score >= 0.5f  -> Color(0xFFFFA726) to "Средний"
        score >= 0.25f -> Color(0xFFFFEE58) to "Низкий"
        else           -> Color(0xFF66BB6A) to "Мин."
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
