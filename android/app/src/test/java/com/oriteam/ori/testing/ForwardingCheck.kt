package com.oriteam.ori.testing

import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.DeleteScope
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.time.LocalDate
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Checks that [forwarderAround] passes **every** member of [contract] to the implementation it wraps,
 * also the members that have a default body in the interface. A forgotten one compiles and runs that
 * default, which calls other members: nothing fails, it only does something else (one query per day
 * instead of one for the whole period, for example).
 *
 * Each member is called with sample arguments, and the wrapped implementation must receive that same
 * member first. [owner] is passed wherever the contract asks for text, so it also chooses the route of
 * the `Routed*` repositories.
 */
suspend fun <T : Any> assertForwardsEveryMember(contract: Class<T>, owner: String, forwarderAround: (T) -> T) {
    contract.methods.filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }.forEach { member ->
        val received = mutableListOf<String>()
        val forwarder = forwarderAround(recordingStub(contract, received))
        val result = member.callWithSamples(forwarder, owner)
        // A flow may start its reading only when collected (the shared readings do).
        if (result is Flow<*>) result.first()
        assertEquals("${forwarder.javaClass.simpleName}.${member.name}", member.name, received.firstOrNull())
    }
}

/** An implementation of [contract] that nobody should call: the other route of a `Routed*` repository. */
fun <T : Any> unusedStub(contract: Class<T>): T = stub(contract) { member ->
    throw AssertionError("${member.name} went to the wrong place")
}

private fun <T : Any> recordingStub(contract: Class<T>, received: MutableList<String>): T =
    stub(contract) { member -> received += member.name }

/** Answers every member with a value of the right shape; a failure is a valid answer for all of them. */
private fun <T : Any> stub(contract: Class<T>, onCall: (Method) -> Unit): T {
    val answer = DataResult.Failure(DataError.UNKNOWN)
    val proxy = Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { self, member, _ ->
        when (member.name) {
            "toString" -> "stub of ${contract.simpleName}"
            "hashCode" -> System.identityHashCode(self)
            "equals" -> false
            else -> {
                onCall(member)
                when (member.returnType) {
                    Flow::class.java -> flowOf(answer)
                    String::class.java -> "new-id"
                    else -> answer
                }
            }
        }
    }
    return contract.cast(proxy)
}

private suspend fun Method.callWithSamples(target: Any, owner: String): Any? {
    val isSuspending = parameterTypes.lastOrNull() == Continuation::class.java
    val types = if (isSuspending) genericParameterTypes.dropLast(1) else genericParameterTypes.toList()
    val arguments = types.map { sampleOf(it, owner) }
    return try {
        if (isSuspending) {
            suspendCoroutineUninterceptedOrReturn { continuation ->
                invoke(target, *(arguments + continuation).toTypedArray())
            }
        } else {
            invoke(target, *arguments.toTypedArray())
        }
    } catch (exception: InvocationTargetException) {
        throw exception.cause ?: exception
    }
}

private fun sampleOf(type: Type, owner: String): Any = when {
    type is ParameterizedType && type.rawType == List::class.java ->
        listOf(sampleOf(type.actualTypeArguments.first(), owner))
    type is ParameterizedType && type.rawType == Map::class.java -> mapOf(owner to 0)
    else -> samplesFor(owner)[type] ?: error("No sample for $type: add one to samplesFor")
}

/** One value for each parameter type of the contracts; [owner] wherever they ask for text. */
private fun samplesFor(owner: String): Map<Type, Any> = mapOf(
    String::class.java to owner,
    Integer.TYPE to 0,
    Int::class.javaObjectType to 0,
    java.lang.Boolean.TYPE to true,
    LocalDate::class.java to DAY,
    RoutineViewer::class.java to RoutineViewer.Owner,
    RoutineTask::class.java to activity("a"),
    ActivityDraft::class.java to ActivityDraft(
        title = "Água",
        date = DAY,
        scheduledTime = null,
        description = null,
        ownerCanComplete = true,
        isHiddenFromCaregiver = false
    ),
    RepeatRule::class.java to RepeatRule.EveryDay,
    DeleteScope::class.java to DeleteScope.ALL_DAYS,
    SymbolDraft::class.java to SymbolDraft("Água", phrase = null, image = null)
)
