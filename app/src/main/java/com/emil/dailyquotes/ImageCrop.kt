package com.emil.dailyquotes

import android.graphics.Picture
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage

@Composable
fun ImageCrop(
    image: Uri,
    hideCrop: () -> Unit,
    setImage: (ImageBitmap) -> Unit,
    saveImage: (ImageBitmap) -> Unit
) {
    BackHandler {
        hideCrop()
    }

    val picture = remember {
        Picture()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Adjust image",
                style = MaterialTheme.typography.headlineMedium
            )
            ElevatedCard(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors()
                    .copy(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Box(
                    modifier = Modifier
                ) {
                    ZoomableGlideImage(
                        modifier = Modifier
                            .padding(2.dp)
                            .fillMaxSize()
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

                                        /**
                                         * Constantly setting the image here and then displaying it
                                         * on the same screen (in [EditProfile]) is required because
                                         * otherwise the view will remain empty and cropping doesn't
                                         * work (I don't know why).
                                         */
                                        setImage(pictureToBitmap(picture, Color.Black.toArgb()).asImageBitmap())
                                    }
                                }
                            }
                            .clip(RoundedCornerShape(22.dp)),
                        model = image,
                        contentDescription = "selected image",
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        Row {
            TextButton(onClick = {
                hideCrop()
            }) {
                Text(text = "Cancel")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                saveImage(
                    pictureToBitmap(picture, Color.Black.toArgb()).asImageBitmap()
                )
                hideCrop()
            }) {
                Text(text = "Continue")
            }
        }
    }
}