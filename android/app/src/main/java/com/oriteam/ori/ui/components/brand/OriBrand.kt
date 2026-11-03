package com.oriteam.ori.ui.components.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Logo, name and slogan of ORI. [logoHeight] lets short windows use a smaller logo instead of
 * pushing the rest of the screen out of view.
 */
@Composable
fun OriBrandBlock(modifier: Modifier = Modifier, logoHeight: Dp = 200.dp) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.ori),
            contentDescription = stringResource(R.string.onboarding_logo_description),
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(logoHeight),
            contentScale = ContentScale.Fit
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        Text(
            text = stringResource(R.string.onboarding_slogan),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            letterSpacing = 0.2.sp
        )
    }
}

/** The two soft arcs in the lower right corner of the brand screens. Purely decorative. */
@Composable
fun OriDecorativeArcs(modifier: Modifier = Modifier) {
    if (!LocalOriAppearance.current.showsDecoration) return
    val colors = LocalOriColors.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val thinPath = Path().apply {
            moveTo(width * 0.51f, height)
            quadraticTo(width * 0.74f, height * 0.84f, width, height * 0.78f)
        }
        drawPath(path = thinPath, color = colors.arcThin, style = Stroke(width = 2.dp.toPx()))

        val thickPath = Path().apply {
            moveTo(width * 0.60f, height + 20.dp.toPx())
            quadraticTo(width * 0.80f, height * 0.87f, width + 20.dp.toPx(), height * 0.81f)
        }
        drawPath(path = thickPath, color = colors.arcThick, style = Stroke(width = 28.dp.toPx()))
    }
}
