@file:Suppress("ForbiddenImport") // The one place allowed to use the Material Text directly.

package com.oriteam.ori.ui.components.text

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.oriteam.ori.ui.theme.LocalOriAppearance
import androidx.compose.material3.Text as MaterialText

/**
 * Every text of the app goes through here, which is what makes the "Aa" choice work on all
 * screens, menus and dialogs without each one having to remember it. Detekt forbids importing
 * the Material Text elsewhere. Only what is drawn changes: stored and spoken values stay as typed.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = if (LocalOriAppearance.current.isUppercase) text.uppercase() else text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/** Styled text. Styles are kept as long as uppercasing keeps the length, which holds for Portuguese. */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = if (LocalOriAppearance.current.isUppercase) text.uppercasePreservingStyles() else text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = style
    )
}

internal fun AnnotatedString.uppercasePreservingStyles(): AnnotatedString {
    val upper = text.uppercase()
    return if (upper.length == text.length) {
        AnnotatedString(upper, spanStyles = spanStyles, paragraphStyles = paragraphStyles)
    } else {
        AnnotatedString(upper)
    }
}
