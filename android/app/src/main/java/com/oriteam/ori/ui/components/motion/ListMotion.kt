package com.oriteam.ori.ui.components.motion

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.theme.OriMotion

/*
 * Items of the lazy lists and grids fade in and out, and the others slide to their new places (a board
 * moved up, an activity added): the eye follows what changed instead of finding a new list. The items need
 * a stable key. Reduced motion keeps only the fade; with animation off the list just changes.
 *
 * When the whole list is replaced (another day, another board), give the list a `key` of its own: a new
 * list is shown at once, instead of every item fading out and in together.
 */

/** For an item of a `LazyColumn` or `LazyRow`; [scope] is the item's scope. */
fun Modifier.itemAnimation(scope: LazyItemScope, motion: OriMotion): Modifier = with(scope) {
    this@itemAnimation.animateItem(motion.itemFade(), motion.itemPlacement(), motion.itemFade())
}

/** For an item of a `LazyVerticalGrid`; [scope] is the item's scope. */
fun Modifier.itemAnimation(scope: LazyGridItemScope, motion: OriMotion): Modifier = with(scope) {
    this@itemAnimation.animateItem(motion.itemFade(), motion.itemPlacement(), motion.itemFade())
}
