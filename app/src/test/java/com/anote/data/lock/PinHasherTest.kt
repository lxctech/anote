package com.anote.data.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun verifyMatchesHashedPin() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.verify("1234", salt, hash))
    }

    @Test
    fun wrongPinIsRejected() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertFalse(PinHasher.verify("9999", salt, hash))
    }

    @Test
    fun differentSaltProducesDifferentHash() {
        val pin = "123456"
        val first = PinHasher.hash(pin, PinHasher.newSalt())
        val second = PinHasher.hash(pin, PinHasher.newSalt())
        assertFalse(first.contentEquals(second))
    }
}
