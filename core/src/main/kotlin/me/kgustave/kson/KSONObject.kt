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

import kotlin.jvm.JvmField

/**
 * @author Kaidan Gustave
 */
expect class KSONObject
constructor(map: Map<String, Any?> = HashMap()): MutableMap<String, Any?> {

    companion object {
        @JvmField val NULL: Null
    }

    constructor(ksonString: String)

    operator fun set(key: String, value: Any?): KSONObject
    operator fun plusAssign(pair: Pair<String, Any?>)
    operator fun plusAssign(kson: KSONObject)
    operator fun plus(pair: Pair<String, Any?>): KSONObject
    operator fun plus(kson: KSONObject): KSONObject
    fun put(pair: Pair<String, Any?>): KSONObject
    fun put(from: Map<String, Any?>): KSONObject
    fun putOnce(key: String?, value: Any?): KSONObject

    // MutableMap#put and MutableMap#remove overrides
    // should force KSONObject to be returned
    override fun put(key: String, value: Any?): KSONObject
    override fun remove(key: String): KSONObject

    // Override the get operator to prevent null
    // returns with KSONObject#get
    override operator fun get(key: String): Any

    fun isNull(key: String): Boolean

    inline fun <reified T: Any> opt(key: String): T?
    inline fun <reified T> opt(key: String, defaultValue: T): T

    infix fun query(pointer: String): Any?
    infix fun query(pointer: KSONPointer): Any?
    inline infix fun query(pointer: KSONPointer.Builder.() -> Unit): Any?

    infix fun String.to(value: Any?): KSONObject

    fun toString(indentFactor: Int): String
    override fun toString(): String

    class Null internal constructor() {
        override fun toString(): String
        override fun equals(other: Any?): Boolean
        override fun hashCode(): Int
    }
}