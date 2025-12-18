package com.jordankurtz.logger

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoggerTest {

    private lateinit var mockWriter: MockLogWriter

    @BeforeTest
    fun setup() {
        mockWriter = MockLogWriter()
        Logger.addWriter(mockWriter)
    }

    @AfterTest
    fun tearDown() {
        Logger.clearWriters()
    }

    @Test
    fun `v should log verbose message`() {
        val message = "verbose message"
        Logger.v(message)

        assertEquals(Logger.VERBOSE, mockWriter.lastPriority)
        assertEquals(message, mockWriter.lastMessage)
        assertNotNull(mockWriter.lastTag)
        assertNull(mockWriter.lastThrowable)
    }

    @Test
    fun `d should log debug message`() {
        val message = "debug message"
        Logger.d(message)

        assertEquals(Logger.DEBUG, mockWriter.lastPriority)
        assertEquals(message, mockWriter.lastMessage)
        assertNotNull(mockWriter.lastTag)
        assertNull(mockWriter.lastThrowable)
    }

    @Test
    fun `i should log info message`() {
        val message = "info message"
        Logger.i(message)

        assertEquals(Logger.INFO, mockWriter.lastPriority)
        assertEquals(message, mockWriter.lastMessage)
        assertNotNull(mockWriter.lastTag)
        assertNull(mockWriter.lastThrowable)
    }

    @Test
    fun `w should log warn message`() {
        val message = "warn message"
        Logger.w(message)

        assertEquals(Logger.WARN, mockWriter.lastPriority)
        assertEquals(message, mockWriter.lastMessage)
        assertNotNull(mockWriter.lastTag)
        assertNull(mockWriter.lastThrowable)
    }

    @Test
    fun `e should log error message`() {
        val message = "error message"
        Logger.e(message)

        assertEquals(Logger.ERROR, mockWriter.lastPriority)
        assertEquals(message, mockWriter.lastMessage)
        assertNotNull(mockWriter.lastTag)
        assertNull(mockWriter.lastThrowable)
    }

    @Test
    fun `e should log throwable`() {
        val throwable = RuntimeException("test exception")
        Logger.e(throwable)

        assertEquals(Logger.ERROR, mockWriter.lastPriority)
        assertEquals("", mockWriter.lastMessage)
        assertEquals(throwable, mockWriter.lastThrowable)
        assertNotNull(mockWriter.lastTag)
    }

    @Test
    fun `e should log message and throwable`() {
        val message = "error with throwable"
        val throwable = RuntimeException("test exception")
        Logger.e(message, throwable)

        assertEquals(Logger.ERROR, mockWriter.lastPriority)
        assertEquals(message, mockWriter.lastMessage)
        assertEquals(throwable, mockWriter.lastThrowable)
        assertNotNull(mockWriter.lastTag)
    }

    private class MockLogWriter : LogWriter {
        var lastPriority: Int? = null
        var lastTag: String? = null
        var lastMessage: String? = null
        var lastThrowable: Throwable? = null

        override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
            this.lastPriority = priority
            this.lastTag = tag
            this.lastMessage = message
            this.lastThrowable = throwable
        }
    }
}
