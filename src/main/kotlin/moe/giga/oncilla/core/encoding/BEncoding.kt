package moe.giga.oncilla.core.encoding

import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.math.BigInteger
import java.util.*
import kotlin.text.Charsets.UTF_8

object BEncoding {
    const val END_SUFFIX = 'e'

    const val INTEGER_PREFIX = 'i'
    const val LIST_PREFIX = 'l'
    const val MAP_PREFIX = 'd'

    class DecoderException(message: String) : RuntimeException(message)

    internal abstract class TypeReader {
        object BEInteger : TypeReader() {
            override fun readFrom(input: PushbackInputStream): Type.BEInteger {
                val sb = StringBuilder()
                loop@ while (true) {
                    val c = input.read()
                    when {
                        Character.isDigit(c) || sb.isEmpty() && c.toChar() == '-' -> sb.append(c.toChar())
                        c.toChar() == END_SUFFIX -> break@loop
                        else -> throw DecoderException("Unexpected token while reading integer: ${c.toChar()}")
                    }
                }

                return Type.BEInteger(BigInteger(sb.toString()))
            }
        }

        object BEList : TypeReader() {
            override fun readFrom(input: PushbackInputStream): Type {
                val list = mutableListOf<Type>()
                while (true) {
                    val c = input.read()
                    if (c.toChar() == END_SUFFIX) {
                        break
                    }
                    input.unread(c)
                    list.add(parse(input))
                }
                return Type.BEList(list)
            }
        }

        object BEMap : TypeReader() {
            override fun readFrom(input: PushbackInputStream): Type {
                val map = mutableMapOf<String, Type>()
                while (true) {
                    val c = input.read()
                    if (c.toChar() == END_SUFFIX) {
                        break
                    }
                    input.unread(c)
                    val key = parse(input)
                    if (key !is Type.BEString) {
                        throw DecoderException("Map keys must be a string found ${key.type}")
                    }
                    val value = parse(input)
                    map[key.string] = value
                }
                return Type.BEMap(map)
            }
        }

        object BEString : TypeReader() {
            override fun readFrom(input: PushbackInputStream): Type {
                val sb = StringBuilder()
                loop@ while (true) {
                    val c = input.read().toChar()
                    when {
                        c == ':' -> break@loop
                        Character.isDigit(c) -> sb.append(c)
                        else -> throw DecoderException("String had non-integer length value")
                    }
                }

                val len = sb.toString().toInt()

                val byteArray = ByteArray(len)
                val readLen = input.read(byteArray)
                if (readLen != len) throw DecoderException("String had mismatched length expected $len got $readLen")
                return Type.BEString(byteArray)
            }
        }

        abstract fun readFrom(input: PushbackInputStream): Type
    }

    sealed class Type {
        data class BEInteger(val integer: BigInteger) : Type() {
            override val type: String = "integer"

            override fun writeTo(out: OutputStream) {
                out.write(INTEGER_PREFIX)
                out.write(integer.toString())
                out.write(END_SUFFIX)
            }
        }

        data class BEList(val list: List<Type>) : Type() {
            override val type: String = "list"

            override fun writeTo(out: OutputStream) {
                out.write(LIST_PREFIX)
                list.forEach { it.writeTo(out) }
                out.write(END_SUFFIX)
            }
        }

        data class BEMap(val map: Map<String, Type>) : Type() {
            override val type: String = "map"

            override fun writeTo(out: OutputStream) {
                out.write(MAP_PREFIX)
                map.toSortedMap().forEach {
                    BEString(it.key).writeTo(out)
                    it.value.writeTo(out)
                }
                out.write(END_SUFFIX)
            }
        }

        data class BEString(val content: ByteArray) : Type() {
            override val type: String = "string"

            constructor(s: String) : this(s.toByteArray())

            override fun writeTo(out: OutputStream) {
                out.write(content.size.toString())
                out.write(':')
                out.write(content)
            }

            // Overridden due to content being an array
            override fun equals(other: Any?) =
                    this === other || (other is BEString && Arrays.equals(content, other.content))

            // Overridden due to content being an array
            override fun hashCode(): Int = 31 * Arrays.hashCode(content)

            val string by lazy { String(content, UTF_8) }
        }

        abstract val type: String
        abstract fun writeTo(out: OutputStream)
    }


    fun parse(input: PushbackInputStream): Type {
        val c = input.read()
        return when (c.toChar()) {
            INTEGER_PREFIX -> TypeReader.BEInteger.readFrom(input)
            LIST_PREFIX -> TypeReader.BEList.readFrom(input)
            MAP_PREFIX -> TypeReader.BEMap.readFrom(input)
            else -> {
                if (!Character.isDigit(c)) {
                    throw DecoderException("Invalid type prefix: $c")
                }
                input.unread(c)
                TypeReader.BEString.readFrom(input)
            }
        }
    }

    fun parse(input: InputStream) = parse(PushbackInputStream(input))

    private fun OutputStream.write(b: Char) = this.write(b.toInt())
    private fun OutputStream.write(s: String) = this.write(s.toByteArray())
}