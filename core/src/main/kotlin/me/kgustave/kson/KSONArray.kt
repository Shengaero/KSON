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
@file:Suppress("unused")
package me.kgustave.kson

/**
 * @author Kaidan Gustave
 */
expect class KSONArray constructor(list: MutableList<Any?> = ArrayList()) : Collection<Any?> {

    constructor(source: String)
    constructor(collection: Collection<*>)
    constructor(array: Array<*>)
    constructor(initialSize: Int, init: (Int) -> Any?)

    operator fun Any?.component1(): Int
    operator fun Any?.component2(): Any?

    operator fun get(index: Int): Any
    operator fun set(index: Int, element: Any?): KSONArray

    fun put(value: Any?): KSONArray
    fun put(index: Int, value: Any?): KSONArray

    inline fun <reified T> get(index: Int): T
    inline fun <reified T> opt(index: Int): T?
    fun isNull(index: Int): Boolean

    fun toKSONObject(function: (Int, Any?) -> String): KSONObject

    override fun toString(): String
    fun toString(indentFactor: Int): String
}