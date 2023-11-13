package com.github.michaelbull.result.suspend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

@RestrictsSuspension
public sealed interface ResultBindingScope<E> {
    public suspend fun <V> Result<V, E>.bind(): V
}

private class ResultBindingScopeImpl<V, E>(private val block: suspend ResultBindingScope<E>.() -> V) : ResultBindingScope<E>, Continuation<V> {
    private var finalResult: Result<V, E>? = null

    fun runBlock(): Result<V, E> {
        val continuation = this.block.createCoroutineUnintercepted(this, this)
        continuation.resume(Unit)
        return finalResult!!
    }
    override suspend fun <V> Result<V, E>.bind(): V {
        return suspendCoroutineUninterceptedOrReturn {
            when (this) {
                is Ok -> this.value
                is Err -> {
                    finalResult = this
                    COROUTINE_SUSPENDED
                }
            }
        }
    }

    override val context: CoroutineContext = EmptyCoroutineContext

    override fun resumeWith(result: kotlin.Result<V>) {
        finalResult = Ok(result.getOrThrow())
    }
}

public fun <V, E> binding(block: suspend ResultBindingScope<E>.() -> V): Result<V, E> {
    return ResultBindingScopeImpl(block).runBlock()
}
