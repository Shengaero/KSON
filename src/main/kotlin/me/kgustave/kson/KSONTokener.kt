/*
 * Copyright 2017 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("MemberVisibilityCanPrivate", "Unused")
package me.kgustave.kson

import java.io.*

class KSONTokener(reader: Reader) : AutoCloseable by reader {

    constructor(inputStream: InputStream) : this(InputStreamReader(inputStream))
    constructor(string: String) : this(StringReader(string))

    companion object {
        fun dehexchar(c: Char) = when (c) {
            in '0'..'9' -> c - '0'
            in 'A'..'F' -> c.toInt() - ('A'.toInt() - 10)
            in 'a'..'f' -> c.toInt() - ('a'.toInt() - 10)
            else -> -1
        }
    }

    private val reader: Reader = if(reader.markSupported()) reader else BufferedReader(reader)
    private var character: Long = 1
    private var eof: Boolean = false
    private var index: Long = 0
    private var line: Long = 1
    private var previous: Char = 0.toChar()
    private var usePrevious: Boolean = false

    val isAtEnd: Boolean
        get() = this.eof && !this.usePrevious

    val hasMore: Boolean
        @Throws(KSONException::class)
        get() {
            next()
            return if(isAtEnd)
                false
            else {
                back()
                true
            }
        }

    @Throws(KSONException::class)
    fun back() {
        denyIf(this.usePrevious) { "Stepping back two steps is not supported" }

        index -= 1
        character -= 1
        usePrevious = true
        eof = false
    }

    @Throws(KSONException::class)
    operator fun next(): Char {
        var c: Int
        if (usePrevious) {
            usePrevious = false
            c = previous.toInt()
        } else {
            try {
                c = reader.read()
            } catch (exception: IOException) {
                throw KSONException(exception)
            }

            if(c <= 0) { // End of stream
                eof = true
                c = 0
            }
        }

        index += 1

        when {
            previous == '\r' -> {
                line += 1
                character = (if (c == '\n'.toInt()) 0 else 1).toLong()
            }

            c == '\n'.toInt() -> {
                line += 1
                character = 0
            }

            else -> character += 1
        }

        previous = c.toChar()

        return previous
    }

    @Throws(KSONException::class)
    fun next(c: Char): Char {
        val n = next()
        if(n != c)
            throw syntaxError("Expected '$c' and instead saw '$n'")
        return n
    }

    @Throws(KSONException::class)
    fun next(n: Int): String {
        if(n == 0) return ""
        val chars = CharArray(n)
        var pos = 0

        while(pos < n) {
            chars[pos] = next()
            if(isAtEnd) throw syntaxError("Substring bounds error")
            pos += 1
        }

        return String(chars)
    }

    @Throws(KSONException::class)
    fun nextClean(): Char {
        var c: Char
        while(true) {
            c = next()
            if(c.toInt() == 0 || c > ' ')
                return c
        }
    }

    @Throws(KSONException::class)
    fun nextString(quote: Char): String {
        var c: Char
        val sb = StringBuilder()

        while(true) {
            c = next()
            when(c) {
                0.toChar(), '\n', '\r' -> throw syntaxError("Unterminated string")

                '\\' -> {
                    c = next()
                    when(c) {
                        'b' -> sb.append('\b')
                        't' -> sb.append('\t')
                        'n' -> sb.append('\n')
                        'f' -> sb.append('\u000C') // Escape for \f in kotlin isn't supported
                        'r' -> sb.append('\r')

                        'u' -> try {
                            sb.append(Integer.parseInt(next(4), 16).toChar())
                        } catch (e: NumberFormatException) {
                            throw syntaxError("Illegal escape.", e)
                        }

                        '"', '\'', '\\', '/' -> sb.append(c)

                        else -> throw syntaxError("Illegal escape.")
                    }
                }

                else -> {
                    if(c == quote)
                        return sb.toString()
                    sb.append(c)
                }
            }
        }
    }

    @Throws(KSONException::class)
    fun nextTo(delimiter: Char): String {
        val sb = StringBuilder()
        var c: Char
        while(true) {
            c = next()

            if(c == delimiter || c.toInt() == 0 || c == '\n' || c == '\r') {
                if(c.toInt() != 0)
                    back()
                return sb.toString().trim()
            }

            sb.append(c)
        }
    }

    @Throws(KSONException::class)
    fun nextTo(delimiters: String): String {
        val sb = StringBuilder()
        var c: Char
        while(true) {
            c = next()

            if(delimiters.indexOf(c) >= 0 || c.toInt() == 0 || c == '\n' || c == '\r') {
                if(c.toInt() != 0)
                    back()
                return sb.toString().trim()
            }

            sb.append(c)
        }
    }

    @Throws(KSONException::class)
    fun nextValue(): Any? {
        var c = nextClean()
        val string: String

        when (c) {
            '"', '\'' -> return nextString(c)

            '{' -> {
                back()
                return KSONObject(this)
            }

            '[' -> {
                back()
                return KSONObject(this)
            }
        }

        val sb = StringBuilder()
        while(c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c)
            c = next()
        }

        back()

        string = sb.toString().trim()

        if("" == string)
            throw syntaxError("Missing value")

        return KSONObject.stringToValue(string)
    }

    @Throws(KSONException::class)
    infix fun skipTo(to: Char): Char {
        var c: Char
        try {
            val startIndex = index
            val startCharacter = character
            val startLine = line

            reader.mark(1000000)

            do {
                c = this.next()
                if(c.toInt() == 0) {
                    reader.reset()
                    index = startIndex
                    character = startCharacter
                    line = startLine
                    return c
                }
            } while (c != to)

        } catch (exception: IOException) {
            throw KSONException(exception)
        }

        back()
        return c
    }

    override fun toString() = " at " + this.index + " [character " + this.character + " line " + this.line + "]"
}

internal fun KSONTokener.syntaxError(message: String) = KSONException(message + toString())
internal fun KSONTokener.syntaxError(message: String, cause: Throwable) = KSONException(message + toString(), cause)