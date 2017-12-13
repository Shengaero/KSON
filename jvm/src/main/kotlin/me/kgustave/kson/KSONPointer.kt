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
@file:Suppress("Unused")
package me.kgustave.kson

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * @author Kaidan Gustave
 */
@Suppress("MemberVisibilityCanPrivate")
actual class KSONPointer {
    companion object {
        private val ENCODING = "UTF-8"
        private val SEPARATOR_REGEX = Regex("/")

        private fun unescape(token: String) = token
            .replace("~1", "/")
            .replace("~0", "~")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

        private fun escape(token: String) = token
            .replace("~", "~0")
            .replace("/", "~1")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }

    actual val refTokens: List<String>

    actual constructor(refTokens: List<String>) {
        this.refTokens = refTokens.run copyList@ {
            val list = ArrayList<String>()

            if(isEmpty())
                return@copyList list

            list += this

            return@copyList list
        }
    }

    @Throws(KSONException::class)
    actual constructor(pointer: String) {
        refTokens = if(pointer.isEmpty() || pointer == "#") {
            emptyList()
        } else {
            when {
                pointer.startsWith("#/") -> try {
                    URLDecoder.decode(pointer.substring(2), ENCODING)
                } catch (e: UnsupportedEncodingException) {
                    throw KSONException(e)
                }

                pointer.startsWith("/") -> pointer.substring(1)

                else -> throw KSONException("A KSONPointer string should start with '/' or '#/'")
            }.split(SEPARATOR_REGEX).dropLastWhile { it.isEmpty() }.map { unescape(it) }
        }
    }

    @Suppress("RemoveExplicitTypeArguments")
    @Throws(KSONException::class)
    actual fun queryFrom(document: Any): Any? {
        require(document is KSONObject || document is KSONArray) {
            "Queried document must be a KSONObject or KSONArray!"
        }
        if(refTokens.isEmpty())
            return document

        var current: Any? = document
        for(token in refTokens) {
            current = when(current) {
                is KSONObject -> current.opt<Any>(unescape(token))
                is KSONArray  -> readByIndexToken(current, token)
                else -> throw KSONException("Value [%s] is not a KSONArray or KSONObject so its key '%s' cannot be resolved".format(current, token))
            }
        }
        return current
    }

    @Throws(KSONException::class)
    private fun readByIndexToken(current: KSONArray, indexToken: String): Any {
        try {
            val index = indexToken.toInt()
            require(index < current.size) {
                "Index %d is out of bounds - the array has %d elements".format(index, current.size)
            }

            return current[index]
        } catch (e: NumberFormatException) {
            throw KSONException("%s is not a KSONArray index".format(indexToken), e)
        }
    }

    @Throws(KSONException::class)
    actual fun toURIFragment(): String {
        try {
            return buildString {
                append("#")
                for(token in refTokens)
                    append('/').append(URLEncoder.encode(token, ENCODING))
            }
        } catch (e: UnsupportedEncodingException) {
            throw KSONException(e)
        }
    }

    actual override fun toString() = buildString {
        for(token in refTokens)
            append('/').append(escape(token))
    }

    actual class Builder: Appendable {
        private val refTokens = ArrayList<String>()

        actual override fun append(csq: CharSequence?): Builder {
            require(csq != null) { "Token cannot be null" }
            require(csq is String) { "Token must be a string literal" }
            refTokens.add(csq!! as String)
            return this
        }

        actual override fun append(csq: CharSequence?, start: Int, end: Int) = append(csq?.subSequence(start, end))
        actual override fun append(c: Char) = if(c.isDigit()) append(c.toInt()) else append(c.toString())
        actual fun append(arrayIndex: Int) = append(arrayIndex.toString())

        actual operator fun div(csq: CharSequence?) = append(csq)
        actual operator fun div(index: Int) = append(index)
        actual operator fun get(index: Int) = append(index)

        actual operator fun String.div(csq: CharSequence?) = append(this).append(csq)
        actual operator fun Int.div(csq: CharSequence?) = append(this).append(csq)
        actual operator fun String.div(index: Int) = append(this).append(index)

        actual fun build() = KSONPointer(refTokens)
    }
}