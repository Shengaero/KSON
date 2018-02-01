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
@file:Suppress("MemberVisibilityCanPrivate", "Unused", "MemberVisibilityCanBePrivate")
package me.kgustave.kson

import me.kgustave.kson.annotation.KSON
import me.kgustave.kson.annotation.KSONValue
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.HashMap
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

/**
 * **KSONObject**
 *
 * A JSON Object, commonly expressed and used in JavaScript.
 *
 * The basic structure of a KSONObject is a [MutableMap] that
 * pairs generic objects with [Strings][String].
 *
 * **Syntax Operators**
 *
 * KSONObjects make use of **many** of the Kotlin standard
 * operator functions to provide very neat, readable, and
 * formatted style.
 *
 * ```kotlin
 * fun syntax(kson: KSONObject) {
 *     kson["foo"] = "bar"                 // Sets a key "foo" to a value "bar"
 *
 *     val baz = kson["baz"] as Int        // You can do this
 *                                         // or
 *     val bazAgain = kson.get<Int>("baz") // you can do this
 *
 *     kson += "bal" to "fal"              // Pairs can be plusAssigned to a KSONObject
 * }
 * ```
 *
 * **Nullability**
 *
 * While certainly exposing values as nullable in some areas,
 * internally all values are treated as non-nullable.
 *
 * Developers are provided with `null` in cases where the return
 * value might not exist, or might require provision via request
 * of the developer but not be guaranteed.
 *
 * The issue with trying to manage the duality of knowing internally
 * nothing is ever `null` while still providing Kotlin standard
 * null-safety is conquered here by pseudo-implementation of MutableMap,
 * and usage of a faux-null object that is treated as `null` internally,
 * but not ever provided as a result of a function.
 *
 * Hence, in areas where data might not contain [KSONObject.NULL], the
 * actual `null` value is provided instead.
 *
 * @author Kaidan Gustave
 */
@SinceKotlin("1.1")
actual class KSONObject @Throws(KSONException::class)
actual constructor(map: Map<String, Any?> = HashMap()): MutableMap<String, Any?> {
    actual companion object {
        @JvmField actual val NULL = Null()

        @Throws(IOException::class)
        internal fun indent(writer: Writer, indent: Int) {
            for(i in 0 until indent) {
                writer.write(" ")
            }
        }

        @Throws(IOException::class)
        internal fun quote(string: String): String {
            return StringWriter().use { sw -> synchronized(sw.buffer) { quote(string, sw).toString() } }
        }

        @Throws(IOException::class)
        internal fun quote(string: String?, writer: Writer): Writer {
            if(string == null || string.isEmpty()) {
                writer.write("\"\"")
                return writer
            }

            var b: Char
            var c: Char = 0.toChar()

            writer.write("\"")
            for(i in 0 until string.length) {
                b = c
                c = string[i]

                when(c) {
                    '\\', '"' -> {
                        writer.write("\\")
                        writer.write(c.toInt())
                    }

                    '/' -> {
                        if(b == '<')
                            writer.write("\\")
                        writer.write(c.toInt())
                    }

                    '\b' -> writer.write("\\b")
                    '\t' -> writer.write("\\t")
                    '\n' -> writer.write("\\n")
                    '\u000C' -> writer.write("\\f") // Apparent kotlin doesn't have \f
                    '\r' -> writer.write("\\r")
                    else -> if(c < ' ' || c in '\u0080'..'\u00a0' || c in '\u2000'..'\u2100') {
                        writer.write("\\u")
                        val hhhh = Integer.toHexString(c.toInt())
                        writer.write("0000", 0, 4 - hhhh.length)
                        writer.write(hhhh)
                    } else {
                        writer.write(c.toInt())
                    }
                }
            }
            writer.write("\"")
            return writer
        }

        @Suppress("UNCHECKED_CAST")
        @Throws(KSONException::class, IOException::class)
        internal fun writeValue(writer: Writer, value: Any?, indentFactor: Int, indent: Int): Writer {
            when(value) {
                null -> writer.write("null")
                is KSONString -> {
                    val o: Any? = try {
                        value.toKSONString()
                    } catch (e: Exception) {
                        throw KSONException(e)
                    }

                    writer.write(o?.toString() ?: quote(value.toString()))
                }

                is Number -> {
                    val numberAsString = numberToString(value as Number?)
                    try {
                        BigDecimal(numberAsString) // This tests if the number is correct
                        writer.write(numberAsString)
                    } catch (ex: NumberFormatException) {
                        quote(numberAsString, writer)
                    }
                }

                is Boolean -> writer.write(value.toString())
                is Enum<*> -> writer.write(quote(value.name))
                is KSONObject -> value.write(writer, indentFactor, indent)
                is KSONArray -> value.write(writer, indentFactor, indent)
                is Map<*, *> -> KSONObject(value as Map<String, Any?>).write(writer, indentFactor, indent)
                is Collection<*> -> KSONArray(value).write(writer, indentFactor, indent)
                is Array<*> ->  KSONArray(value).write(writer, indentFactor, indent)
                else -> quote(value.toString(), writer)
            }
            return writer
        }

        @Throws(KSONException::class)
        private fun numberToString(number: Number?): String {
            if(number == null)
                throw KSONException("Null pointer")

            testValidity(number)

            var string = number.toString()
            if(string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
                while(string.endsWith("0"))
                    string = string.substring(0, string.length - 1)

                if(string.endsWith("."))
                    string = string.substring(0, string.length - 1)
            }

            return string
        }

        @Throws(KSONException::class)
        internal fun testValidity(o: Any?) {
            if(o != null) {
                if(o is Double && (o.isInfinite() || o.isNaN()))
                    throw KSONException("JSON does not allow non-finite numbers.")
                else if(o is Float && (o.isInfinite() || o.isNaN()))
                    throw KSONException("JSON does not allow non-finite numbers.")
            }
        }

        internal fun stringToValue(string: String): Any {
            if(string == "")
                return string
            if(string.equals("true", ignoreCase = true))
                return true
            if(string.equals("false", ignoreCase = true))
                return false
            if(string.equals("null", ignoreCase = true))
                return KSONObject.NULL

            val initial = string[0]
            if(initial in '0'..'9' || initial == '-') {
                try {
                    if(string.indexOf('.') > -1 ||
                       string.indexOf('e') > -1 ||
                       string.indexOf('E') > -1 ||
                       string == "-0") {
                        val d = string.toDouble()
                        if(!d.isInfinite() && !d.isNaN())
                            return d
                    } else {
                        val long = string.toLong()

                        if(string == long.toString())
                            return if(long == long.toInt().toLong()) long.toInt() else long
                    }
                } catch (ignore: Exception) {}
            }

            return string
        }

        @Throws(KSONException::class)
        internal fun valueToString(value: Any?): String {
            return when(value) {
                NULL -> "null"

                is KSONString -> try {
                    value.toKSONString()
                } catch (e: Exception) {
                    throw KSONException(e)
                }

                is Number -> {
                    val numberAsString = numberToString(value as Number?)
                    return try {
                        BigDecimal(numberAsString)
                        numberAsString
                    } catch (ex: NumberFormatException) {
                        quote(numberAsString)
                    }
                }

                is Boolean, is KSONObject, is KSONArray -> value.toString()

                is Map<*,*> -> KSONObject(value).toString()
                is Collection<*> -> KSONArray(value).toString()
                is Array<*> -> KSONArray(value).toString()
                is Enum<*> -> quote(value.name)

                else -> quote(value.toString())
            }
        }

        internal fun wrap(obj: Any?): Any {
            try {
                return when(obj) {
                    null -> NULL

                    NULL, is KSONObject, is KSONArray, is KSONString,
                    is Byte, is Char, is Short, is Int, is Long, is Boolean, is Float,
                    is Double, is String, is BigInteger, is BigDecimal, is Enum<*> -> obj

                    is Array<*> -> KSONArray(obj)
                    is Collection<*> -> KSONArray(obj)
                    is Map<*,*> -> KSONObject(obj)

                    else -> {
                        return if(obj::class.findAnnotation<KSON>() != null) {
                            KSONObject(obj)
                        } else {
                            val pack = obj.javaClass.`package`
                            val packName = if(pack != null) pack.name else ""

                            if(packName.startsWith("java.") ||
                               packName.startsWith("javax.") ||
                               packName.startsWith("kotlin.") ||
                               obj.javaClass.classLoader == null) {
                                obj.toString()
                            } else KSONObject(obj)
                        }
                    }
                }
            } catch (e: Exception) {
                throw KSONException("Failed to wrap $obj.", e)
            }
        }
    }

    // We copy the baseMap so that end developers don't either
    // on accident or on purpose modify it without permission.
    private val map: MutableMap<String, Any> = map.run baseMap@ {
        val copyMap = HashMap<String, Any>()

        if(this@baseMap.isEmpty())
            return@baseMap copyMap

        for((key, value) in this@baseMap) {
            if(value == null)
                throw KSONException("Null key or value.")
            copyMap[key] = value
        }

        return@baseMap copyMap
    }

    override val size: Int
        get() = map.size
    override val entries: MutableSet<MutableMap.MutableEntry<String, Any?>>
        get() = map.entries.mutableMapTo(HashSet()) { Entry(it.key, it.value) }
    override val keys: MutableSet<String>
        get() = map.keys
    override val values: MutableCollection<Any?>
        get() = map.values.mapMutable { it }

    @Throws(KSONException::class)
    constructor(x: KSONTokener): this() {
        var c: Char
        var key: String

        if(x.nextClean() != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'")
        }

        while(true) {
            c = x.nextClean()
            when (c) {
                0.toChar() -> throw x.syntaxError("A JSONObject text must end with '}'")

                '}' -> return

                else -> {
                    x.back()
                    key = x.nextValue().toString()
                }
            }

            c = x.nextClean()
            if(c != ':')
                throw x.syntaxError("Expected a ':' after a key")
            putOnce(key, x.nextValue())

            when(x.nextClean()) {
                ';', ',' -> {
                    if(x.nextClean() == '}') return
                    x.back()
                }

                '}' -> return

                else -> throw x.syntaxError("Expected a ',' or '}'")
            }
        }
    }

    @Throws(KSONException::class)
    actual constructor(@Language("JSON") ksonString: String): this(KSONTokener(ksonString))

    /**
     * Creates a new [KSONObject] using the provided [object][any].
     *
     * This constructor will take a different approach to wrapping
     * the the object depending on the value of [metaWrap].
     *
     * 1) If the class is annotated with @[KSON], the constructor will use
     *    annotations provided in the [annotations][me.kgustave.kson.annotation]
     *    package to [metaWrap][KSONObject.metaWrap] the object.
     *
     * 2) If the class is **not** annotated with @[KSON], the constructor
     *    will populate the internal map for this KSONObject using
     *    [populateMap].
     *
     * @param  any
     *         The [Any] to use.
     *
     * @throws KSONException
     *         If:
     *         1) A member property annotated with @[KSONValue] is not
     *            `public` and the class is annotated with @[KSON].
     *         2) An error is thrown while [wrapping][wrap] this.
     */
    @Throws(KSONException::class)
    constructor(any: Any): this() {
        if(any::class.findAnnotation<KSON>() != null) {
            metaWrap(any)
        } else {
            populateMap(any)
        }
    }

    @Throws(KSONException::class)
    actual operator fun plusAssign(pair: Pair<String, Any?>) { put(pair) }

    @Throws(KSONException::class)
    actual operator fun plusAssign(kson: KSONObject) {
        for((key, value) in kson)
            put(key, value)
    }

    @Throws(KSONException::class)
    actual operator fun plus(pair: Pair<String, Any?>): KSONObject = put(pair)

    @Throws(KSONException::class)
    actual operator fun plus(kson: KSONObject): KSONObject {
        for((key, value) in kson)
            put(key, value)
        return this
    }

    @Throws(KSONException::class)
    actual operator fun set(key: String, value: Any?): KSONObject = put(key, value)

    @Throws(KSONException::class)
    actual fun put(pair: Pair<String, Any?>): KSONObject = put(pair.first, pair.second)

    @Throws(KSONException::class)
    actual fun putOnce(key: String?, value: Any?): KSONObject {
        if(key != null && value != null) {
            if(containsKey(key))
                throw KSONException("Duplicate key \"" + key + "\"")
            put(key, value)
        }
        return this
    }

    @Throws(KSONException::class)
    actual override fun put(key: String, value: Any?): KSONObject {
        testValidity(value)
        map[key] = value ?: KSONObject.NULL
        return this
    }

    @Throws(KSONException::class)
    actual fun put(from: Map<String, Any?>): KSONObject {
        putAll(from)
        return this
    }

    @Throws(KSONException::class)
    override fun putAll(from: Map<out String, Any?>) {
        for((key, value) in from)
            put(key, value)
    }

    override fun containsKey(key: String): Boolean = map.contains(key)
    override fun containsValue(value: Any?): Boolean = map.containsValue(value)

    actual fun isNull(key: String): Boolean = map[key]?.takeIf { NULL != it } === null

    @Throws(KSONException::class)
    actual override operator fun get(key: String): Any =
        map[key]?.takeIf { NULL != it } ?: throw KSONException("KSONObject[${quote(key)}] not found.")

    override fun isEmpty() = map.isEmpty()

    override fun clear() = map.clear()

    actual override fun remove(key: String): KSONObject = put(key, null)

    actual inline fun <reified T: Any> opt(key: String): T? = if(isNull(key)) null else this[key] as? T

    actual inline fun <reified T> opt(key: String, defaultValue: T): T {
        return try {
            when(defaultValue) {
                is String -> this[key].toString()
                is Char -> this[key]
                is KSONObject -> this[key]
                is KSONArray -> this[key]
                is Number -> { this[key] as Number }

                else -> throw KSONException("${T::class} is not an opt-able type!")
            } as? T ?: defaultValue
        } catch(e: KSONException) {
            return defaultValue
        }
    }

    inline fun <reified T> only() = filter { it.value is T }.map { Entry(it.key, it.value ?: NULL) }.iterator()

    actual infix fun query(pointer: String): Any? = query(KSONPointer(pointer))

    actual infix fun query(pointer: KSONPointer): Any? = pointer.queryFrom(this)

    actual inline infix fun query(pointer: KSONPointer.Builder.() -> Unit): Any? = this query pointer(pointer)

    @Throws(KSONException::class)
    actual infix fun String.to(value: Any?): KSONObject = put(this, value)

    /**
     * Meta-constructs a new instance of [T] using annotations
     * in the [kson annotations][me.kgustave.kson.annotation]
     * package.
     *
     * @param  T
     *         The type of object to construct using this KSONObject.
     *         Should be annotated with @[KSONConstructor].
     *
     * @throws KSONException
     *         If an exception is thrown while calling the constructor.
     * @throws NoSuchElementException
     *         If no constructor is annotated with @[KSONConstructor].
     *
     * @see    KSONSerializer
     *
     * @return A new, never-null instance of T using the calling KSONObject
     */
    @Throws(KSONException::class, NoSuchElementException::class)
    inline fun <reified T: Any> construct() = KSONSerializer.construct(T::class, this)

    @Throws(KSONException::class)
    private fun populateMap(bean: Any) {
        // Property getters
        bean::class.memberProperties.forEach {
            try {
                // Public property
                if(it.visibility == KVisibility.PUBLIC) {
                    map[it.name] = wrap(it.getter.call(bean))
                }
            } catch(e: Exception) {}
        }

        // Function Members
        bean::class.memberFunctions.forEach {
            try {
                // Public function
                if(it.visibility == KVisibility.PUBLIC) {
                    val functionKSON = KSONObject()
                    val paramArray = KSONArray().apply {
                        it.parameters.forEach params@ {
                            val name = it.name ?: return@params
                            val paramKSON = KSONObject().put(name, it.type.classifier)

                            if(it.isOptional) paramKSON["optional"] = true

                            if(!paramKSON.isEmpty())
                                put(paramKSON)
                        }
                    }
                    if(paramArray.isNotEmpty())
                        functionKSON["parameters"] = paramArray
                    else
                        functionKSON["value"] = it.call(bean)
                    map[it.name] = functionKSON
                }
            } catch(e: Exception) {}
        }
    }

    @Throws(KSONException::class)
    private fun metaWrap(any: Any) {
        val cla = any::class

        // Member Properties
        cla.memberProperties.forEach {
            val annotation = it.findAnnotation<KSONValue>() ?: return@forEach
            val getter = it.getter
            if(getter.visibility != KVisibility.PUBLIC)
                throw KSONException("Property $it has a non-public getter.")
            put(annotation.value, wrap(getter.call(any)))
        }

        // Member Functions
        cla.memberFunctions.forEach {
            val annotation = it.findAnnotation<KSONValue>() ?: return@forEach
            if(it.visibility != KVisibility.PUBLIC)
                throw KSONException("Function $it is non-public.")
            put(annotation.value, wrap(it.call(any)))
        }
    }

    /**
     * Generates a [String] from the current keys and values of this
     * [KSONObject] with the provided [indentFactor] as a number of spaces
     * per indent.
     *
     * @throws KSONException
     *         If an error occurs while generating the String.
     *
     * @return A string using the provided [indentFactor].
     */
    @Throws(KSONException::class)
    actual fun toString(indentFactor: Int): String {
        return StringWriter().use { sw -> synchronized(sw.buffer) { write(sw, indentFactor, 0).toString() } }
    }

    actual override fun toString() = toString(0)

    @Throws(KSONException::class)
    internal fun write(writer: Writer, indentFactor: Int, indent: Int): Writer {
        try {
            var commanate = false
            val length = size
            writer.write("{")

            if(length == 1) {
                val key = map.keys.elementAt(0)
                writer.write(quote(key))
                writer.write(":")
                if(indentFactor > 0)
                    writer.write(" ")
                writeValue(writer, map[key], indentFactor, indent)
            } else if(length != 0) {
                val newIndent = indent + indentFactor
                for(key in map.keys) {
                    if(commanate)
                        writer.write(",")
                    if(indentFactor > 0)
                        writer.write("\n")

                    indent(writer, newIndent)
                    writer.write(quote(key))
                    writer.write(":")
                    if(indentFactor > 0)
                        writer.write(" ")
                    writeValue(writer, map[key], indentFactor, newIndent)
                    commanate = true
                }
                if(indentFactor > 0)
                    writer.write("\n")
                indent(writer, indent)
            }
            writer.write("}")
            return writer
        } catch (e: IOException) {
            throw KSONException("An error occurred while writing KSONObject to string.",e)
        }
    }

    /**
     * Simplified implementation of [MutableMap.MutableEntry] for handling
     * key-value entries a [KSONObject].
     *
     * Modification of these simultaneously modifies the placement value in
     * the [KSONObject] the originate from, regardless of mutability.
     */
    inner class Entry(override val key: String, value: Any): MutableMap.MutableEntry<String, Any?> {
        override var value: Any? = value
            private set(value) { field = value ?: NULL }
            get() = if(NULL == field) null else field

        override fun setValue(newValue: Any?): Any? {
            val old = value.takeIf { NULL != it }
            value = newValue ?: NULL
            this@KSONObject[key] = newValue
            return old
        }
    }

    actual class Null internal actual constructor() {
        actual override fun toString() = "null"
        actual override fun equals(other: Any?): Boolean = other == null || other is Null
        actual override fun hashCode() = javaClass.hashCode()
    }
}