package moe.giga.oncilla.core.encoding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BEncodingTest {

    @Test
    fun testEncodingInteger() {
        val value = BEncoding.Type.BEInteger(BigInteger.valueOf(1234567890))

        val stream = ByteArrayOutputStream()
        value.writeTo(stream)

        assertEquals("i1234567890e", stream.toString())
    }

    @Test
    fun testDecodingInteger() {
        val stream = "i1234567890e".byteInputStream()

        assertEquals(BEncoding.Type.BEInteger(BigInteger.valueOf(1234567890)), BEncoding.parse(stream))
    }

    @Test
    fun testEncodingList() {
        val value = BEncoding.Type.BEList(
                listOf(
                        BEncoding.Type.BEString("some string1:2#3"),
                        BEncoding.Type.BEInteger(BigInteger.valueOf(1234567890)),
                        BEncoding.Type.BEMap(mapOf())
                )
        )

        val stream = ByteArrayOutputStream()
        value.writeTo(stream)

        assertEquals("l16:some string1:2#3i1234567890edee", stream.toString())
    }

    @Test
    fun testDecodingList() {
        val value = BEncoding.Type.BEList(
                listOf(
                        BEncoding.Type.BEString("some string1:2#3"),
                        BEncoding.Type.BEInteger(BigInteger.valueOf(1234567890)),
                        BEncoding.Type.BEMap(mapOf())
                )
        )

        val stream = "l16:some string1:2#3i1234567890edee".byteInputStream()

        assertEquals(value, BEncoding.parse(stream))
    }

    @Test
    fun testEncodingMap() {
        val value = BEncoding.Type.BEMap(
                mapOf(
                        "4:list" to BEncoding.Type.BEList(
                                listOf(
                                        BEncoding.Type.BEString("some string1:2#3"),
                                        BEncoding.Type.BEInteger(BigInteger.valueOf(1234567890)),
                                        BEncoding.Type.BEMap(mapOf())
                                )
                        ),
                        "key1" to BEncoding.Type.BEString("some string1:2#3"),
                        "key2" to BEncoding.Type.BEMap(mapOf())
                )
        )

        val stream = ByteArrayOutputStream()
        value.writeTo(stream)

        assertEquals("d6:4:listl16:some string1:2#3i1234567890edee4:key116:some string1:2#34:key2dee", stream.toString())
    }

    @Test
    fun testDecodingMap() {
        val value = BEncoding.Type.BEMap(
                mapOf(
                        "4:list" to BEncoding.Type.BEList(
                                listOf(
                                        BEncoding.Type.BEString("some string1:2#3"),
                                        BEncoding.Type.BEInteger(BigInteger.valueOf(1234567890)),
                                        BEncoding.Type.BEMap(mapOf())
                                )
                        ),
                        "key1" to BEncoding.Type.BEString("some string1:2#3"),
                        "key2" to BEncoding.Type.BEMap(mapOf())
                )
        )

        val stream = "d6:4:listl16:some string1:2#3i1234567890edee4:key116:some string1:2#34:key2dee".byteInputStream()

        assertEquals(value, BEncoding.parse(stream))
    }

    @Test
    fun testEncodingString() {
        val value = BEncoding.Type.BEString("some string")

        val stream = ByteArrayOutputStream()
        value.writeTo(stream)

        assertEquals("11:some string", stream.toString())
    }

    @Test
    fun testDecodingString() {
        val stream = "11:some string".byteInputStream()

        assertEquals(BEncoding.Type.BEString("some string"), BEncoding.parse(stream))
    }
}