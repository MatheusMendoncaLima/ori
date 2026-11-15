package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.WordClass
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.theme.OriSpacing

private val wordClassNames = listOf(
    null to R.string.aac_word_class_auto,
    WordClass.PERSON to R.string.aac_word_class_person,
    WordClass.ACTION to R.string.aac_word_class_action,
    WordClass.FEELING to R.string.aac_word_class_feeling,
    WordClass.SOCIAL to R.string.aac_word_class_social,
    WordClass.OTHER to R.string.aac_word_class_other
)

/**
 * What the word is, for a word the grammar does not know: a name ("Ana" agrees as "ela": "Ana quer água"), a new
 * action ("skate"), how one feels ("animado"). "Automático" leaves it to the grammar's tables, which know the
 * common words. The choice also gives the tile the color of its kind, unless a color was chosen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordClassChoice(selected: WordClass?, onSelect: (WordClass?) -> Unit) {
    Column {
        SectionHeader(text = stringResource(R.string.aac_word_class))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)
        ) {
            wordClassNames.forEach { (wordClass, nameRes) ->
                OriChoiceChip(
                    label = stringResource(nameRes),
                    isSelected = wordClass == selected,
                    onClick = { onSelect(wordClass) }
                )
            }
        }
        OriNotice(stringResource(R.string.aac_word_class_help))
    }
}
