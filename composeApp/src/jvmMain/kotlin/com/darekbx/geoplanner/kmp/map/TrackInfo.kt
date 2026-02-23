package com.darekbx.geoplanner.kmp.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TOP_PADDING = 10
private const val START_PADDING = 110F

@Composable
fun TrackInfo(highlighted: Highlighted) {
    Column(Modifier.fillMaxWidth().height(200.dp).background(Color.White)) {
        val (track, points) = highlighted
        Row(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "From: ",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                formatTimestampToDateTime(track.start_timestamp),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                ", to: ",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                formatTimestampToDateTime(track.end_timestamp ?: 0),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                ", distance: ",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = "${((track.distance ?: 0.0) / 1000.0).toInt()}km",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                fontSize = 11.sp,
            )
            Text(
                ", ID: ",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                "${track.local_id}",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 6.sp
            )
        }

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight().weight(1F)) {
            ChartView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .weight(0.5F)
                    .padding(8.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                data = points.map { SpeedUtils.msToKm(it.speed) },
                unit = "km\\h"
            )
            Spacer(Modifier.width(8.dp))
            ChartView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .weight(0.5F)
                    .padding(8.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                data = points.map { it.altitude.toFloat() }, unit = "m"
            )
        }
    }
}


@Composable
fun ChartView(modifier: Modifier = Modifier, data: List<Float>, unit: String) {
    val textMeasurer = rememberTextMeasurer()
    val color = MaterialTheme.colors.error
    Canvas(modifier = modifier) {
        drawEntries(this, color, data, unit, textMeasurer)
    }
}

fun drawEntries(
    canvas: DrawScope,
    color: Color,
    values: List<Float>,
    unit: String,
    textMeasurer: TextMeasurer
) {
    val height = canvas.size.height - TOP_PADDING
    val count = values.count()
    val maxValue = values.max()
    val minValue = values.min()
    val avgValue = values.average().toFloat()
    val widthRatio = (canvas.size.width - START_PADDING) / (count - 1).toFloat()
    val heightRatio = height / (maxValue - minValue)
    var p0 = Offset(START_PADDING, TOP_PADDING + height - ((values.first() - minValue) * heightRatio))
    val maxPosition = height - ((maxValue - minValue) * heightRatio)
    val avgPosition = height - ((avgValue - minValue) * heightRatio)

    var start = START_PADDING

    drawGuide(maxPosition, canvas, textMeasurer, maxValue, unit)
    drawGuide(avgPosition, canvas, textMeasurer, avgValue, unit)

    values
        .drop(1)
        .forEach { entry ->
            val p1 = Offset(start + widthRatio, TOP_PADDING + height - ((entry - minValue) * heightRatio))
            canvas.drawLine(color, p0, p1, strokeWidth = 3F)
            p0 = p1
            start += widthRatio
        }
}

private fun drawGuide(
    maxPosition: Float,
    canvas: DrawScope,
    textMeasurer: TextMeasurer,
    maxValue: Float,
    unit: String
) {
    val lineP0 = Offset(START_PADDING, TOP_PADDING + maxPosition)
    val lineP1 = Offset(canvas.size.width, TOP_PADDING + maxPosition)
    canvas.drawLine(Color.LightGray, lineP0, lineP1, strokeWidth = 0.75F)
    canvas.drawText(
        textMeasurer = textMeasurer,
        text = "%.1f$unit".format(maxValue),
        topLeft = Offset(2F, lineP0.y - 6F),
        style = TextStyle(Color.DarkGray, fontSize = 8.sp),
    )
}
