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


actual internal fun List<Any?>.nullified(): List<Any?> = map { if(it == KSONObject.NULL) null else it }

actual internal inline
fun <reified T, reified R> MutableCollection<T>.mapMutable(transform: (T) -> R): MutableCollection<R>
    = mutableMapTo(ArrayList(), transform)

actual internal inline fun <reified T, reified R, reified C: MutableCollection<R>>
    MutableCollection<T>.mutableMapTo(other: C, transform: (T) -> R): C = mapTo(other) { transform(it) }

// Exception tools
actual internal inline fun require(condition: Boolean, lazy: () -> String) { if(!condition) throw KSONException(lazy()) }
actual internal inline fun denyIf(condition: Boolean, lazy: () -> String) { if(condition) throw KSONException(lazy()) }

// KSONTokener syntax errors
internal fun KSONTokener.syntaxError(message: String) = KSONException(message + toString())
internal fun KSONTokener.syntaxError(message: String, cause: Throwable) = KSONException(message + toString(), cause)

// JVM Utils
internal fun systemProperty(key: String): String? = System.getProperty(key)