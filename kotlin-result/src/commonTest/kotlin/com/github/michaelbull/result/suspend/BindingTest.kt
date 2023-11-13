package com.github.michaelbull.result.suspend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class BindingTest {

    object BindingError

    @Test
    fun returnsOkIfAllBindsSuccessful() {
        fun provideX(): Result<Int, BindingError> = Ok(1)
        fun provideY(): Result<Int, BindingError> = Ok(2)

        val result = binding<Int, BindingError> {
            val x = provideX().bind()
            val y = provideY().bind()
            x + y
        }

        assertTrue(result is Ok)
        assertEquals(3, result.value)
    }

    @Test
    fun returnsOkIfAllBindsOfDifferentTypeAreSuccessful() {
        fun provideX(): Result<String, BindingError> = Ok("1")
        fun provideY(x: Int): Result<Int, BindingError> = Ok(x + 2)

        val result = binding<Int, BindingError> {
            val x = provideX().bind()
            val y = provideY(x.toInt()).bind()
            y
        }

        assertTrue(result is Ok)
        assertEquals(3, result.value)
    }

    @Test
    fun returnsFirstErrIfBindingFailed() {
        fun provideX(): Result<Int, BindingError> = Ok(1)
        fun provideY(): Result<Int, BindingError> = Err(BindingError)
        fun provideZ(): Result<Int, BindingError> = Ok(2)

        val result = binding<Int, BindingError> {
            val x = provideX().bind()
            val y = provideY().bind()
            val z = provideZ().bind()
            x + y + z
        }

        assertTrue(result is Err)
        assertEquals(BindingError, result.error)
    }

    @Test
    fun returnsFirstErrIfBindingsOfDifferentTypesFailed() {
        fun provideX(): Result<Int, BindingError> = Ok(1)
        fun provideY(): Result<String, BindingError> = Err(BindingError)
        fun provideZ(): Result<Int, BindingError> = Ok(2)

        val result = binding<Int, BindingError> {
            val x = provideX().bind()
            val y = provideY().bind()
            val z = provideZ().bind()
            x + y.toInt() + z
        }

        assertTrue(result is Err)
        assertEquals(BindingError, result.error)
    }

    private class TestError : RuntimeException()
    @Test
    fun failsSuccessfullyIfExceptionIsThrown() {
        fun provideX(): Result<Int, BindingError> = Ok(1)
        kotlin.runCatching {
            binding<Int, BindingError> {
                provideX().bind()
                throw TestError()
            }
        }.onSuccess { fail("Should've caught exception") }
            .onFailure {
                assertIs<TestError>(it)
            }
    }
}
