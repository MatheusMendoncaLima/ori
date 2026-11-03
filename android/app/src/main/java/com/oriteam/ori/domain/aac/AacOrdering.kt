package com.oriteam.ori.domain.aac

/**
 * Moves the item at [from] to [to] and returns the new order. Out-of-range indexes return the
 * order unchanged, so a stale tap (the list changed meanwhile) does nothing.
 */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val result = toMutableList()
    result.add(to, result.removeAt(from))
    return result
}

/**
 * Positions to write so that [newOrder] (ids) is stored as 0, 1, 2…. Only items whose position
 * changes are returned, which keeps a move to a couple of writes and also repairs ties left by
 * two people adding symbols at the same time.
 */
fun positionChanges(currentPositions: Map<String, Int>, newOrder: List<String>): Map<String, Int> =
    newOrder.withIndex()
        .filter { (index, id) -> currentPositions[id] != index }
        .associate { (index, id) -> id to index }
