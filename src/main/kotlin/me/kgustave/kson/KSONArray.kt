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
package me.kgustave.kson

import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.function.Consumer
import kotlin.reflect.KClass

/**
 * @author Kaidan Gustave
 */
@Suppress("MemberVisibilityCanPrivate", "Unused")
class KSONArray(private val list: MutableList<Any?> = ArrayList()): Collection<Any?> by list
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
    constructor(source: String): this(KSONTokener(source))

    constructor(collection: Collection<*>): this() {
        collection.mapTo(list) { KSONObject.wrap(it) }
    }

    constructor(array: Array<*>): this() {
        array.mapTo(list) { KSONObject.wrap(it) }
    }

    // Requires kotlin.reflect
    constructor(eClass: KClass<Enum<*>>): this() {
        require(try {Class.forName("kotlin.reflect.jvm.reflectLambdaKt"); true} catch(e: Exception) {false})
        { "Kotlin reflect not in classpath!" }
        eClass.java.enumConstants.forEach { put(it) }
    }

    operator fun Any?.component1(): Int = indexOf(this)
    operator fun Any?.component2(): Any? = this

    @JvmName("getOperator")
    @Throws(KSONException::class, IndexOutOfBoundsException::class)
    operator fun get(index: Int): Any {
        val size = size
        if(index !in 0 until size)
            throw IndexOutOfBoundsException("Index specified not in bounds 0 - $size")
        return list[index]?.takeIf { it != KSONObject.NULL } ?: throw KSONException("KSONArray[$index] is null.")
    }

    @Throws(KSONException::class)
    operator fun set(index: Int, element: Any?): KSONArray {
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

    fun put(value: Any?): KSONArray {
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
    fun put(index: Int, value: Any?): KSONArray {
        this[index] = value
        return this
    }

    @Throws(KSONException::class, IndexOutOfBoundsException::class)
    inline fun <reified T> get(index: Int): T {
        val value = this[index]
        return when(value) {
            is Number ->
                // So a bit of a fun fact:
                // There's no way for us to safely figure out what kind of Number
                // the value might be using smart-cast, because trying to compare
                // KClasses is not currently supported, nor is it resource efficient.
                // So what do you do when you need to figure out what subtype of a
                // supertype a reified type argument is? You take default values of
                // each subtype, check if their the type parameter, and then safely
                // cast them to the specific type argument
                when {
                    1 is T             -> value.toInt() as T
                    1L is T            -> value.toLong() as T
                    1.toShort() is T   -> value.toShort() as T
                    1.0 is T           -> value.toDouble() as T
                    1.0.toFloat() is T -> value.toFloat() as T
                    1.toByte() is T    -> value.toByte() as T
                    ' ' is T           -> value.toChar() as T

                    else -> throw KSONException("KSONArray[$index] is not of type ${T::class}.")
                }

            else -> value as? T ?: throw KSONException("KSONArray[$index] is not of type ${T::class}.")
        }
    }

    inline fun <reified T> opt(index: Int): T? {
        if(size !in 0 until size) {
            return null
        } else {
            val value = try {
                this[index].run{ if("" !is T) this@run else this@run.toString() }
            } catch(e: KSONException) {
                return if("" is T) "null" as T else null
            }

            return if(value is Number) {
                when {
                    1 is T             -> value.toInt() as T
                    1L is T            -> value.toLong() as T
                    1.toShort() is T   -> value.toShort() as T
                    1.0 is T           -> value.toDouble() as T
                    1.0.toFloat() is T -> value.toFloat() as T
                    1.toByte() is T    -> value.toByte() as T
                    ' ' is T           -> value.toChar() as T
                    else -> null
                }
            } else {
                value as? T
            }
        }
    }

    fun isNull(index: Int): Boolean = KSONObject.NULL == opt(index)

    fun toKSONObject(function: (Int, Any?) -> String): KSONObject {
        val kson = KSONObject()
        for((index, element) in this)
            kson.put(function(index, element), element)
        return kson
    }

    override fun forEach(action: Consumer<in Any?>) = list.nullified().forEach(action)
    override fun spliterator() = list.nullified().spliterator()
    override fun iterator() = list.nullified().iterator()
    override fun stream() = list.nullified().stream()
    override fun parallelStream() = list.nullified().parallelStream()
    override fun isEmpty() = list.nullified().none { it != null }

    @Throws(KSONException::class)
    override fun toString(): String = toString(0)

    @Throws(KSONException::class)
    fun toString(indentFactor: Int): String {
        val w = StringWriter()
        return synchronized(w.buffer) { write(w, indentFactor, 0).toString() }
    }

    @Throws(KSONException::class)
    fun write(writer: Writer, indentFactor: Int, indent: Int): Writer {
        try {
            var commanate = false
            val length = size
            writer.write("[")

            if (length == 1) {
                KSONObject.writeValue(writer, list[0], indentFactor, indent)
            } else if (length != 0) {
                val newindent = indent + indentFactor
                var i = 0
                while (i < length) {
                    if (commanate) {
                        writer.write(",")
                    }
                    if (indentFactor > 0) {
                        writer.write("\n")
                    }
                    KSONObject.indent(writer, newindent)
                    KSONObject.writeValue(writer, list[i], indentFactor, newindent)
                    commanate = true
                    i += 1
                }
                if (indentFactor > 0) {
                    writer.write("\n")
                }

                KSONObject.indent(writer, indent)
            }
            writer.write("]")
            return writer
        } catch (e: IOException) {
            throw KSONException(e)
        }
    }
}

private fun List<Any?>.nullified() = map { if(it == KSONObject.NULL) null else it }