package com.oriteam.ori.ui.app.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/** Title and subtitle of the login screen. */
@Composable
internal fun LoginGreeting() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(OriSpacing.ExtraSmall))
        ReturningUserSubtitle()
    }
}

@Composable
private fun ReturningUserSubtitle() {
    val prefix = stringResource(R.string.login_subtitle_returning_prefix)
    val appName = stringResource(R.string.app_name)
    val suffix = stringResource(R.string.login_subtitle_returning_suffix)
    val emphasisColor = MaterialTheme.colorScheme.onBackground
    Text(
        text = buildAnnotatedString {
            append(prefix)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = emphasisColor)) { append(appName) }
            append(suffix)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Friendly card at the top of the form. */
@Composable
internal fun WelcomeCard() {
    OriCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.mao_acenando),
                contentDescription = stringResource(R.string.login_welcome_icon_description),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(OriSpacing.Medium))
            Column {
                Text(
                    text = stringResource(R.string.login_welcome_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(OriSpacing.ExtraSmall))
                Text(
                    text = stringResource(R.string.login_welcome_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
