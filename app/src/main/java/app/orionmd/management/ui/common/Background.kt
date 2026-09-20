package app.orionmd.management.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.orionmd.management.R

/**
 * The app-wide translucent branding watermark, painted once behind the whole nav host. Every
 * screen on top of this MUST use a transparent Scaffold/surface container color, or it will
 * paint over the watermark and hide it (see app.orionmd.management.ui.common.transparentScaffoldColors).
 */
@Composable
fun RentalsBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.tab_watermark),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Crop,
            alpha = 1f // the PNG itself is pre-baked to ~6% opacity so text on top stays readable
        )
        content()
    }
}
