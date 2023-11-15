package com.github.michaelbull.result.coroutines.binding.suspend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

public sealed interface ResultSuspendableBindingScope<E> {
    public suspend fun <V> Result<V, E>.bind(): V
}

private class ResultSuspendableBindingScopeImpl<V, E>(
    override val context: CoroutineContext,
    private val block: suspend ResultSuspendableBindingScope<E>.() -> V
) : ResultSuspendableBindingScope<E>, Continuation<V> {
    private var finalContinuation: Continuation<Result<V, E>>? = null

    suspend fun runBlock(): Result<V, E> = suspendCoroutine {
        finalContinuation = it
        this.block.startCoroutine(this, this)
    }
    override suspend fun <V> Result<V, E>.bind(): V {
        return suspendCoroutineUninterceptedOrReturn {
            when (this) {
                is Ok -> this.value
                is Err -> {
                    finalContinuation!!.resume(this)
                    COROUTINE_SUSPENDED
                }
            }
        }
    }

    override fun resumeWith(result: kotlin.Result<V>) {
        result.onFailure {
            finalContinuation!!.resumeWithException(it)
        }.onSuccess {
            finalContinuation!!.resume(Ok(it))
        }
    }
}

public suspend fun <V, E> binding(block: suspend ResultSuspendableBindingScope<E>.() -> V): Result<V, E> {
    return coroutineScope {
        ResultSuspendableBindingScopeImpl(coroutineContext, block).runBlock()
    }
}
