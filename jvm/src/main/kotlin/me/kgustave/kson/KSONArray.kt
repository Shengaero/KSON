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
@file:Suppress("Unused", "MemberVisibilityCanBePrivate")
package me.kgustave.kson

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.function.Consumer
import kotlin.reflect.KClass

/**
 * @author Kaidan Gustave
 */
@Suppress("MemberVisibilityCanPrivate")
actual class KSONArray
actual constructor(private val list: MutableList<Any?> = ArrayList()): Collection<Any?> by list
{
    @Throws(KSONException::class)
    constructor(x: KSONTokener): this() {
        if(x.nextClean() != '[')
            throw x.syntaxError("A JSONArray text must start with '['")
        if(x.nextClean() != ']') {
            x.back()
            while(true) {
                if(x.nextClean() == ',') {
                    x.back()
                    list.add(KSONObject.NULL)
                } else {
                    x.back()
                    list.add(x.nextValue() ?: throw KSONException("Tokener returned a null value!"))
                }

                when(x.nextClean()) {
                    ',' -> {
                        if(x.nextClean() == ']')
                            return
                        x.back()
                    }
                    ']' -> return
                    else -> throw x.syntaxError("Expected a ',' or ']'")
                }
            }
        }
    }

    @Throws(KSONException::class)
    actual constructor(source: String): this(KSONTokener(source))

    actual constructor(collection: Collection<*>): this() {
        collection.mapTo(list) { KSONObject.wrap(it) }
    }

    actual constructor(array: Array<*>): this() {
        array.mapTo(list) { KSONObject.wrap(it) }
    }

    actual constructor(initialSize: Int, init: (Int) -> Any?): this(Array(initialSize, init))

    // Requires kotlin.reflect
    @Throws(KSONException::class)
    constructor(eClass: KClass<Enum<*>>): this() {
        require(try { Class.forName("kotlin.reflect.jvm.reflectLambdaKt"); true } catch(e: Exception) { false }) {
            "Kotlin reflect not in classpath!"
        }
        eClass.java.enumConstants.forEach { put(it) }
    }

    actual operator fun Any?.component1(): Int = indexOf(this)
    actual operator fun Any?.component2(): Any? = this

    @JvmName("getOperator")
    @Throws(KSONException::class, IndexOutOfBoundsException::class)
    actual operator fun get(index: Int): Any {
        val size = size
        if(index !in 0 until size)
            throw IndexOutOfBoundsException("Index specified not in bounds 0 - $size")
        return list[index]?.takeIf { it != KSONObject.NULL } ?: throw KSONException("KSONArray[$index] is null.")
    }

    @Throws(KSONException::class)
    actual operator fun set(index: Int, element: Any?): KSONArray {
        KSONObject.testValidity(element)
        require(index > 0) { "KSONArray[$index] not found." }
        if(index < size) {
            list[index] = element
        } else {
            while(index != size) {
                put(KSONObject.NULL)
            }
            put(element)
        }
        return this
    }

    @Throws(KSONException::class)
    actual fun put(value: Any?): KSONArray {
        when(value) {
            null -> list.add(value)

            KSONObject.NULL, is KSONObject, is KSONArray, is KSONString,
            is Byte, is Char, is Short, is Int, is Long, is Boolean, is Float,
            is Double, is String, is BigInteger, is BigDecimal, is Enum<*> -> list.add(value)

            is Array<*> -> list.add(KSONArray(value))
            is Collection<*> -> list.add(KSONArray(value))
            is Map<*,*> -> list.add(KSONObject(value))
        }
        list.add(value)
        return this
    }

    @Throws(KSONException::class)
    actual fun put(index: Int, value: Any?): KSONArray {
        this[index] = value
        return this
    }

    @Throws(KSONException::class, IndexOutOfBoundsException::class)
    actual inline fun <reified T> get(index: Int): T =
        opt(index) ?: throw KSONException("KSONArray[$index] is not of type ${T::class}.")

    @Throws(IndexOutOfBoundsException::class)
    actual inline fun <reified T> opt(index: Int): T? = this[index] as? T

    @Throws(IndexOutOfBoundsException::class)
    actual fun isNull(index: Int): Boolean = KSONObject.NULL == opt(index)

    actual fun toKSONObject(function: (Int, Any?) -> String): KSONObject {
        val kson = KSONObject()
        for((index, element) in this)
            kson[function(index, element)] = element
        return kson
    }

    override fun forEach(action: Consumer<in Any?>) = list.nullified().forEach(action)
    override fun spliterator() = list.nullified().spliterator()
    override fun iterator() = list.nullified().iterator()
    override fun stream() = list.nullified().stream()
    override fun parallelStream() = list.nullified().parallelStream()
    override fun isEmpty() = list.nullified().none { it != null }

    @Throws(KSONException::class)
    actual override fun toString(): String = toString(0)

    @Throws(KSONException::class)
    actual fun toString(indentFactor: Int): String {
        return buildString { append(this, indentFactor, 0) }
    }

    @Throws(KSONException::class)
    fun append(builder: StringBuilder, indentFactor: Int, indent: Int): StringBuilder {
        try {
            var commanate = false
            val length = size
            builder.append("[")

            if (length == 1) {
                KSONObject.appendValue(builder, list[0], indentFactor, indent)
            } else if (length != 0) {
                val newindent = indent + indentFactor
                var i = 0
                while (i < length) {
                    if (commanate) {
                        builder.append(",")
                    }
                    if (indentFactor > 0) {
                        builder.append("\n")
                    }
                    KSONObject.indent(builder, newindent)
                    KSONObject.appendValue(builder, list[i], indentFactor, newindent)
                    commanate = true
                    i += 1
                }
                if (indentFactor > 0) {
                    builder.append("\n")
                }

                KSONObject.indent(builder, indent)
            }
            builder.append("]")
            return builder
        } catch (e: Exception) {
            throw KSONException(e)
        }
    }
}