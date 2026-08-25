package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.FarmGreenSecondary

@Composable
fun SimpleLineChart(
    title: String,
    dataPoints: List<Pair<Int, Double>>, // ageDays to Value
    unit: String = "",
    lineColor: Color = FarmGreenPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (dataPoints.isNotEmpty()) {
                    Text(
                        text = "Terakhir: ${dataPoints.lastOrNull()?.second?.toInt() ?: 0} $unit",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = lineColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada data grafik", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                val maxVal = (dataPoints.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
                val minVal = (dataPoints.minOfOrNull { it.second } ?: 0.0).coerceAtLeast(0.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = height * (i.toFloat() / gridLines)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw Line Path
                    val path = Path()
                    dataPoints.forEachIndexed { index, point ->
                        val x = index * stepX
                        val range = (maxVal - minVal).coerceAtLeast(1.0)
                        val normalizedY = ((point.second - minVal) / range).toFloat()
                        val y = height - (normalizedY * height * 0.85f) - 10f

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }

                        // Draw point circle
                        drawCircle(
                            color = lineColor,
                            radius = 3.5f,
                            center = Offset(x, y)
                        )
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    title: String,
    dataPoints: List<Pair<String, Double>>, // label to Value
    unit: String = "",
    barColor: Color = Color(0xFFE53935),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                val total = dataPoints.sumOf { it.second }
                Text(
                    text = "Total: ${total.toInt()} $unit",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = barColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                val maxVal = (dataPoints.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val count = dataPoints.size
                    val barWidth = (width / count.coerceAtLeast(1)) * 0.65f
                    val slotWidth = width / count.coerceAtLeast(1)

                    dataPoints.forEachIndexed { index, point ->
                        val barHeight = ((point.second / maxVal) * height * 0.85f).toFloat().coerceAtLeast(4f)
                        val x = (index * slotWidth) + (slotWidth - barWidth) / 2f
                        val y = height - barHeight

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }
    }
}
