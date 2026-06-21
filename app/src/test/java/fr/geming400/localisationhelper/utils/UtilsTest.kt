package fr.geming400.localisationhelper.utils

import org.junit.Assert
import org.junit.Test

class UtilsTest {
    fun testXor(content: String, key: String) {
        val xoredContent = Utils.cyclicXor(content.toByteArray(), key.toByteArray())

        // A xor operation is reversible
        // which mean that re-xoring the output with the same
        // parameters should yield the original
        // content input
        Assert.assertArrayEquals(content.toByteArray(), Utils.cyclicXor(xoredContent, key.toByteArray()))
    }

    @Test
    fun cyclicXorStringTest() {
        testXor("Hi this is a very cool string", "Example key")
        testXor("Hi this is a very cool string", Utils.hashString("SHA-256", "abcde".toByteArray()))
    }
}