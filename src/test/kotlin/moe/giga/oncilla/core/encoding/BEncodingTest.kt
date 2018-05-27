package moe.giga.oncilla.core.encoding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PushbackInputStream
import java.math.BigInteger
import java.util.*


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
        val value = BEncoding.Type.BEString("some strîng")

        val stream = ByteArrayOutputStream()
        value.writeTo(stream)

        assertEquals("12:some strîng", stream.toString())
    }

    @Test
    fun testDecodingString() {
        val stream = "12:some strîng".byteInputStream()

        assertEquals(BEncoding.Type.BEString("some strîng"), BEncoding.parse(stream))
    }

    @Test
    fun testHashCodeString() {
        val a = BEncoding.Type.BEString("some strîng")
        val b = BEncoding.Type.BEString("some strîng@24")
        val c = BEncoding.Type.BEString("some strîng")
        assertEquals(a.hashCode(), c.hashCode())
        assertNotEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a.hashCode(), Arrays.hashCode("some strîng".toByteArray()))
    }

    @Test
    fun testInvalidMap() {
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("d4:teste".byteInputStream()) }
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("di123ee".byteInputStream()) }
    }

    @Test
    fun testInvalidInteger() {
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("iasdfe".byteInputStream()) }
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("i123ffe".byteInputStream()) }
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("i123".byteInputStream()) }

        assertThrows<BEncoding.DecoderException> { BEncoding.parse("i1+1".byteInputStream()) }
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("i1-1".byteInputStream()) }
    }

    @Test
    fun testInvalidList() {
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("li123".byteInputStream()) }
    }

    @Test
    fun testInvalidString() {
        assertThrows<BEncoding.DecoderException> { BEncoding.parse("12:asdf".byteInputStream()) }
        assertThrows<BEncoding.DecoderException> { BEncoding.TypeReader.BEString.readFrom(PushbackInputStream("g:asdf".byteInputStream())) }
    }

    @Test
    fun testTorrentRead() {
        val classLoader = javaClass.classLoader
        val file = File(classLoader.getResource("big-buck-bunny.torrent").file)
        val torrentData = BEncoding.parse(file.inputStream())

        val stream = ByteArrayOutputStream()
        torrentData.writeTo(stream)

        assertEquals(file.readText(), stream.toString())
    }
}