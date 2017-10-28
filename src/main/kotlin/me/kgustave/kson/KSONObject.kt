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

import me.kgustave.kson.annotation.KSON
import me.kgustave.kson.annotation.KSONConstructor
import me.kgustave.kson.annotation.KSONValue
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

/**
 * Java Script Object Notation based
 * @author Kaidan Gustave
 */
class KSONObject @Throws(KSONException::class)
constructor(map: Map<String?, Any?> = HashMap()): MutableMap<String, Any?> {
    companion object {
        /**
         * Represents a `null` value in either a [KSONObject] or [KSONArray].
         *
         * These are handled internally to avoid using roundabout and elaborate
         * processes to deal with null, safety, as well as to mimic more closely
         * org.json, which this library descends from.
         *
         * On user and developer end, this is not exposed or provided when getting
         * values from this API, and simply a `null` value will be provided if at
         * a location in a KSONObject or KSONArray an instance of this is located.
         */
        class Null internal constructor() {
            override fun toString() = "null"
            override fun equals(other: Any?): Boolean = other == null || other is Null
            override fun hashCode() = javaClass.hashCode()
        }

        @JvmField val NULL = Null()

        @[JvmStatic Throws(IOException::class)]
        internal fun indent(writer: Writer, indent: Int) {
            var i = 0
            while(i < indent) {
                writer.write(" ")
                i += 1
            }
        }

        @JvmStatic
        fun quote(string: String): String {
            val sw = StringWriter()
            return synchronized(sw.buffer) {
                try {
                    quote(string, sw).toString()
                } catch (ignored: IOException) {
                    // will never happen - we are writing to a string writer
                    ""
                }
            }
        }

        @[JvmStatic Throws(IOException::class)]
        fun quote(string: String?, w: Writer): Writer {
            if(string == null || string.isEmpty()) {
                w.write("\"\"")
                return w
            }

            var b: Char
            var c: Char = 0.toChar()

            w.write("\"")
            for(i in 0 until string.length) {
                b = c
                c = string[i]

                when(c) {
                    '\\', '"' -> {
                        w.write("\\")
                        w.write(c.toInt())
                    }

                    '/' -> {
                        if(b == '<')
                            w.write("\\")
                        w.write(c.toInt())
                    }

                    '\b' -> w.write("\\b")
                    '\t' -> w.write("\\t")
                    '\n' -> w.write("\\n")
                    '\u000C' -> w.write("\\f") // Apparent kotlin doesn't have \f
                    '\r' -> w.write("\\r")
                    else -> if(c < ' ' || c in '\u0080'..'\u00a0' || c in '\u2000'..'\u2100') {
                        w.write("\\u")
                        val hhhh = Integer.toHexString(c.toInt())
                        w.write("0000", 0, 4 - hhhh.length)
                        w.write(hhhh)
                    } else {
                        w.write(c.toInt())
                    }
                }
            }
            w.write("\"")
            return w
        }

        @[JvmStatic Throws(KSONException::class, IOException::class)]
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
                is Map<*, *> -> KSONObject(value).write(writer, indentFactor, indent)
                is Collection<*> -> KSONArray(value).write(writer, indentFactor, indent)
                is Array<*> ->  KSONArray(value).write(writer, indentFactor, indent)
                else -> quote(value.toString(), writer)
            }
            return writer
        }

        @[JvmStatic Throws(KSONException::class)]
        fun numberToString(number: Number?): String {
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

        @[JvmStatic Throws(KSONException::class)]
        fun testValidity(o: Any?) {
            if(o != null) {
                if(o is Double && (o.isInfinite() || o.isNaN()))
                    throw KSONException("JSON does not allow non-finite numbers.")
                else if(o is Float && (o.isInfinite() || o.isNaN()))
                    throw KSONException("JSON does not allow non-finite numbers.")
            }
        }

        @JvmStatic
        fun stringToValue(string: String): Any {
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
                    if(string.indexOf('.') > -1 || string.indexOf('e') > -1 ||
                       string.indexOf('E') > -1 || "-0" == string) {
                        val d = string.toDouble()
                        if(!d.isInfinite() && !d.isNaN())
                            return d
                    } else {
                        val myLong = string.toLong()
                        if(string == myLong.toString())
                            return if(myLong == myLong.toInt().toLong()) myLong.toInt() else myLong
                    }
                } catch (ignore: Exception) {}
            }

            return string
        }

        @JvmStatic
        fun wrap(obj: Any?): Any {
            try {
                return when(obj) {
                    null -> NULL

                    NULL == obj, is KSONObject, is KSONArray, is KSONString,
                    is Byte, is Char, is Short, is Int, is Long, is Boolean, is Float,
                    is Double, is String, is BigInteger, is BigDecimal, is Enum<*> -> obj

                    is Array<*> -> KSONArray(obj)
                    is Collection<*> -> KSONArray(obj)
                    is Map<*,*> -> KSONObject(obj)

                    else -> {
                        return if(obj::class.findAnnotation<KSON>() != null) {
                            KSONObject(obj, metaWrap = true)
                        } else {
                            val pack = obj.javaClass.`package`
                            val packName = if(pack != null) pack.name else ""

                            if(packName.startsWith("java.") ||
                               packName.startsWith("javax.") ||
                               packName.startsWith("kotlin.") ||
                               obj.javaClass.classLoader == null) {
                                obj.toString()
                            } else KSONObject(obj, metaWrap = false)
                        }
                    }
                }
            } catch (e: Exception) {
                throw KSONException("Failed to wrap $obj.", e)
            }
        }
    }

    // We copy the baseMap so that end developers don't
    // either on accident or on purpose modify it without permission.
    private val map: MutableMap<String, Any?> = map.run baseMap@ {
        val copyMap = HashMap<String, Any?>()

        if(this@baseMap.isEmpty())
            return@baseMap copyMap

        for((key, value) in this@baseMap) {
            if(key == null || value == null)
                throw KSONException("Null key or value.")
            copyMap.put(key, value)
        }

        return@baseMap copyMap
    }

    override val size: Int by this.map
    override val entries: MutableSet<MutableMap.MutableEntry<String, Any?>> by this.map
    override val keys: MutableSet<String> by this.map
    override val values: MutableCollection<Any?> by this.map

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
    constructor(@Language("JSON") ksonString: String): this(KSONTokener(ksonString))

    /**
     * Creates a new [KSONObject] using the provided [object][any].
     *
     * This constructor will take a different approach to wrapping
     * the the object depending on the value of [metaWrap].
     *
     * 1) If [metaWrap] is `true`, the constructor will use annotations
     *    provided in the [annotations][me.kgustave.kson.annotation]
     *    package to [metaWrap][KSONObject.metaWrap] the object.
     *
     * 2) If [metaWrap] is `false`, the constructor will populate
     *    the internal map for this KSONObject using [populateMap].
     *
     * Default [metaWrap] is `false`.
     *
     * @param  any
     *         The [Any] to use.
     * @param  metaWrap
     *         Whether or not this will use meta-annotations on the
     *         provided [any] to create the KSONObject.
     *
     * @throws KSONException
     *         If:
     *         1) A member property annotated with @[KSONValue]
     *            is not `public` *and* [metaWrap] is `true`.
     *         2) An error is thrown while [wrapping][wrap] this.
     */
    @[JvmOverloads Throws(KSONException::class)]
    constructor(any: Any, metaWrap: Boolean = false): this() {
        if(metaWrap) {
            metaWrap(any)
        } else {
            populateMap(any)
        }
    }

    @Throws(KSONException::class)
    operator fun plusAssign(pair: Pair<String, Any?>) { put(pair) }

    @Throws(KSONException::class)
    operator fun plusAssign(kson: KSONObject) {
        for((key, value) in kson)
            put(key, value)
    }

    @Throws(KSONException::class)
    operator fun plus(pair: Pair<String, Any?>): KSONObject = put(pair)

    @Throws(KSONException::class)
    operator fun plus(kson: KSONObject): KSONObject {
        for((key, value) in kson)
            put(key, value)
        return this
    }

    @Throws(KSONException::class)
    operator fun set(key: String, value: Any?): KSONObject = put(key, value)

    @Throws(KSONException::class)
    fun put(pair: Pair<String, Any?>) = put(pair.first, pair.second)

    @Throws(KSONException::class)
    fun putOnce(key: String?, value: Any?): KSONObject {
        if(key != null && value != null) {
            if(containsKey(key))
                throw KSONException("Duplicate key \"" + key + "\"")
            put(key, value)
        }
        return this
    }

    @Throws(KSONException::class)
    override fun put(key: String, value: Any?): KSONObject {
        if(value != null) {
            testValidity(value)
            map.put(key, value)
        } else map.remove(key)
        return this
    }

    @Throws(KSONException::class)
    override fun putAll(from: Map<out String, Any?>) {
        for((key, value) in from)
            put(key, value)
    }

    override fun containsKey(key: String) = map.contains(key)

    override fun containsValue(value: Any?) = map.containsValue(value)

    @Throws(KSONException::class)
    override operator fun get(key: String) = map[key]?.takeIf { NULL != it }
                                             ?: throw KSONException("KSONObject[${quote(key)}] not found.")
    override fun isEmpty() = map.isEmpty()

    override fun clear() = map.clear()

    override fun remove(key: String) = put(key, null)

    override fun toString() = toString(0)

    inline fun <reified T> opt(key: String, defaultValue: T): T {
        return when(defaultValue) {
            is String -> this[key]
            is KSONObject -> this[key]
            is KSONArray -> this[key]
            is Int -> this[key]
            is Long -> this[key]
            is Short -> this[key]
            is Char -> this[key]
            is Byte -> this[key]
            is Float -> this[key]
            is Double -> this[key]

            else -> throw KSONException("${T::class} is not an opt-able type!")
        } as? T ?: defaultValue
    }

    inline fun <reified T> only() = filter {
        it.value is T
    }.map {
        Entry(this, it.key, it.value ?: NULL)
    }.iterator()

    @Throws(KSONException::class)
    private fun populateMap(bean: Any) {
        // Property getters
        for(property in bean::class.memberProperties) {
            try {
                // Public property
                if(property.visibility == KVisibility.PUBLIC) {
                    val name = property.name
                    val result = property.getter.call(bean)

                    map.put(name, wrap(result))
                }
            } catch(e: Exception) {}
        }

        // Function Members
        for(function in bean::class.memberFunctions) {
            try {
                // Public function
                if(function.visibility == KVisibility.PUBLIC) {
                    val functionKSON = KSONObject()
                    val paramArray = KSONArray().apply {
                        function.parameters.forEach {
                            val name = it.name ?: return@forEach
                            val paramKSON = KSONObject().put(name, it.type.classifier)

                            if(it.isOptional)
                                paramKSON.put("optional", true)

                            if(!paramKSON.isEmpty())
                                put(paramKSON)
                        }
                    }
                    if(paramArray.isNotEmpty())
                        functionKSON["parameters"] = paramArray
                    else
                        functionKSON["value"] = function.call(bean)
                    map.put(function.name, functionKSON)
                }
            } catch(e: Exception) {}
        }
    }

    @Throws(KSONException::class)
    private fun metaWrap(any: Any) {
        val cla = any::class
        cla.memberProperties.forEach {
            val annotation = it.findAnnotation<KSONValue>() ?: return@forEach
            val getter = it.getter
            if(getter.visibility != KVisibility.PUBLIC)
                throw KSONException("Property $it has a non-public getter.")
            put(annotation.value, wrap(getter.call(any)))
        }

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
    fun toString(indentFactor: Int): String {
        val w = StringWriter()
        return synchronized(w.buffer) { write(w, indentFactor, 0).toString() }
    }

    @Throws(KSONException::class)
    fun write(writer: Writer, indentFactor: Int, indent: Int): Writer {
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
     * Meta-constructs a new instance of [T] using annotations
     * in the [kson annotations][me.kgustave.kson.annotation]
     * package.
     *
     * The specified type must have at least one constructor
     * annotated with @[KSONConstructor], the arguments of which
     * should correspond to the parameters of the constructor
     * in the order they are provided in the constructor.
     *
     * Example:
     * ```kotlin
     * fun main(args: Array<String>) {
     *     val kson = KSONObject()
     *     kson["name"] = "Shengaero"
     *     kson["age"] = 17
     *
     *     println(kson.construct<Person>().introduction())
     * }
     *
     * data class Person @KSONConstructor("name", "age")
     * constructor(val name: String, val age: Int) {
     *     fun introduction(): String = "Hello, my name is $name, I am $age years old."
     * }
     *
     * // Hello, my name is Shengaero, I am 17 years old.
     * ```
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
     * @return A new, never-null instance of T using the calling KSONObject
     */
    @Throws(KSONException::class, NoSuchElementException::class)
    inline fun <reified T: Any> construct() = construct(T::class)

    /**
     * An overload for integration with native and other jvm languages
     * where [construct] cannot be called.
     *
     * @param  cla
     *         The [KClass] of object to construct using this
     *         KSONObject. Should be annotated with [@KSON][KSON].
     *
     * @throws KSONException
     *         If an exception is thrown while calling the constructor.
     * @throws NoSuchElementException
     *         If no constructor is annotated with @[KSONConstructor].
     *
     * @see    construct
     *
     * @return A new, never-null instance of [cla] using the calling
     *         KSONObject.
     */
    @Throws(KSONException::class, NoSuchElementException::class)
    fun <T: Any> construct(cla: Class<T>) = construct(cla.kotlin)

    /**
     * Constructs an instance of [cla] using this KSONObject.
     *
     * @param  cla
     *         The [KClass] of object to construct using this
     *         KSONObject. Should be annotated with [@KSON][KSON].
     *
     * @throws KSONException
     *         If an exception is thrown while calling the constructor.
     * @throws NoSuchElementException
     *         If no constructor is annotated with @[KSONConstructor].
     *
     * @return A new, never-null instance of [cla] using the calling
     *         KSONObject.
     */
    @Throws(KSONException::class, NoSuchElementException::class)
    fun <T: Any> construct(cla: KClass<T>): T {
        return cla.constructors.filter {
            // Constructor not annotated
            val conAnn = it.findAnnotation<KSONConstructor>() ?: return@filter false

            if(it.visibility != KVisibility.PUBLIC)
                throw KSONException("Constructor annotated with @KSONConstructor is not public")

            val keyParams = conAnn.value

            if(keyParams.isEmpty() || it.parameters.size != keyParams.size) return@filter false

            return@filter keyParams.all { containsKey(it) }
        }.first().run {
            val conAnn = findAnnotation<KSONConstructor>()!!

            try {
                call(*conAnn.value.map { get(it) }.toTypedArray())
            } catch(e: Exception) {
                throw KSONException("Failed to instantiate $cla.",e)
            }
        }
    }

    /**
     * Simplified implementation of [MutableMap.MutableEntry] for handling
     * key-value entries a [KSONObject].
     *
     * Modification of these simultaneously modifies the placement value in
     * the [KSONObject] the originate from, regardless of mutability.
     */
    class Entry(
        private val kson: KSONObject,
        override val key: String,
        value: Any
    ) : MutableMap.MutableEntry<String, Any?> {
        override var value: Any = value
            private set

        override fun setValue(newValue: Any?): Any? {
            val old = value.takeIf { it != NULL }
            value = newValue ?: NULL
            kson.put(key, value)
            return old
        }
    }
}

inline val <reified T: Map<String?, *>> T.toKSON: KSONObject
    /**
     * Creates a new KSONObject using the receiving [Map].
     * This is a shortcut for [KSONObject()][KSONObject].
     *
     * @throws KSONException
     *         If one is thrown while creating the KSONObject.
     *
     * @return A new KSONObject from the Map.
     */
    @Throws(KSONException::class)
    inline get() = KSONObject(this)