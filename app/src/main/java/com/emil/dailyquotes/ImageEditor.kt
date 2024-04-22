package com.emil.dailyquotes

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Picture
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.emil.dailyquotes.room.Quote

@Composable
fun ImageEditor(
    modifier: Modifier = Modifier,
    context: Context,
    quote: Quote,
    isFullscreen: Boolean = true,
    optionsVisible: Boolean = true,
    onExpandOptions: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val picture = remember { Picture() }

    val density = LocalDensity.current

    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var padding by remember { mutableFloatStateOf(24f) }
    var size by remember { mutableFloatStateOf(1f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var colorIntensity by remember { mutableFloatStateOf(1f) }

    val brush = Brush.linearGradient(colors = listOf(Color.Red, Color.Magenta, Color.Blue))

    val alphaMatrix = ColorMatrix(
        values = floatArrayOf(
            1f, 0f, 0f, 1f - colorIntensity, 0f,
            0f, 1f, 0f, 1f - colorIntensity, 0f,
            0f, 0f, 1f, 1f - colorIntensity, 0f,
            0f, 0f, 0f, alpha, 0f
        )
    )

    var showOptions by remember { mutableStateOf(optionsVisible) }

    val paddingModifier = if (isFullscreen) {
        modifier.safeDrawingPadding()
    } else {
        modifier.navigationBarsPadding()
    }

    Column(
        modifier = paddingModifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isFullscreen) {
            Spacer(modifier = Modifier.weight(1f))
        }
        Card(
            colors = CardDefaults.elevatedCardColors()
                .copy(containerColor = Color.Transparent),
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = containerWidth.dp, vertical = 0.dp)
                .drawWithCache {
                    val width = this.size.width.toInt()
                    val height = this.size.height.toInt()
                    onDrawWithContent {
                        val pictureCanvas = Canvas(
                            picture.beginRecording(
                                width, height
                            )
                        )
                        draw(this, this.layoutDirection, pictureCanvas, this.size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawPicture(picture)
                        }
                    }
                }
        ) {
            Box {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize(),
                    onDraw = {
                        drawRect(brush = brush, colorFilter = ColorFilter.colorMatrix(alphaMatrix))
                    }
                )
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density * size,
                    )
                ) {
                    QuoteCard(
                        modifier = Modifier
                            .padding(padding.dp)
                            .padding(vertical = containerHeight.dp),
                        quote = quote,
                        textStyle = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
        AnimatedContent(targetState = showOptions, label = "") { visible ->
            if (visible) {
                ImageOptions(
                    containerWidth = containerWidth,
                    padding = padding,
                    size = size,
                    colorIntensity = colorIntensity,
                    alpha = alpha,
                    setContainerWidth = { containerWidth = 100f - it },
                    setPadding = { padding = it },
                    setSize = { size = it },
                    setColorIntensity = { colorIntensity = it },
                    setAlpha = { alpha = it }
                )
            } else {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showOptions = true
                        onExpandOptions()
                    }
                ) {
                    Text(text = "Customize")
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalButton(
                //modifier = Modifier.weight(1f),
                onClick = {
                    mainActivity?.hideShareSheet()
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "\"" + quote.quote.trim() + "\""
                    )
                    mainActivity?.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Share quote via..."
                        )
                    )
                    onShare()
                }
            ) {
                Text(text = "Share text")
            }
            Button(
                modifier = Modifier
                    .weight(2f),
                onClick = {
                    val imageUri = getUriForBitmap(
                        context = context,
                        bitmap = pictureToBitmap(picture, Color.Transparent.toArgb())
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "image/png"
                        clipData = ClipData.newRawUri(null, imageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(
                            Intent.EXTRA_STREAM,
                            imageUri
                        )
                    }
                    shareIntent.clipData = ClipData.newRawUri("quote", imageUri)
                    mainActivity?.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Share quote via..."
                        )
                    )
                    onShare()
                }
            ) {
                Text(text = "Share")
            }
        }
    }
}

@Composable
fun ImageOptions(
    containerWidth: Float,
    padding: Float,
    size: Float,
    colorIntensity: Float,
    alpha: Float,
    setContainerWidth: (Float) -> Unit,
    setPadding: (Float) -> Unit,
    setSize: (Float) -> Unit,
    setColorIntensity: (Float) -> Unit,
    setAlpha: (Float) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OptionSlider(
            title = "Container width",
            value = 100f - containerWidth,
            setValue = setContainerWidth,
            valueRange = 0f..100f
        )
        OptionSlider(
            title = "Padding",
            value = padding,
            setValue = setPadding,
            valueRange = 0f..100f
        )
        OptionSlider(
            title = "Size",
            value = size,
            setValue = setSize,
            valueRange = .5f..1.2f
        )
        OptionSlider(
            title = "Color intensity",
            value = colorIntensity,
            setValue = setColorIntensity,
            valueRange = 0f..1f
        )
        OptionSlider(
            title = "Opacity",
            value = alpha,
            setValue = setAlpha,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun OptionSlider(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    setValue: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = setValue, valueRange = valueRange)
    }
}

@Composable
private fun ColorPicker(
    modifier: Modifier = Modifier,
    title: String,
    colors: List<Color>,
    selectedIndex: Int,
    setSelectedColor: (Int) -> Unit
) {
    Column {
        Text(text = title)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (index in 0..colors.lastIndex) {
                Card(
                    colors = CardDefaults.elevatedCardColors().copy(containerColor = colors[index]),
                    border = if (selectedIndex == index) BorderStroke(
                        2.dp,
                        CardDefaults.elevatedCardColors().contentColor
                    ) else null,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable {
                            setSelectedColor(index)
                        }
                ) {}
            }
        }
    }
}