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

/**
 * Marks a KSON library entity to signify the usage is not
 * intended for Java.
 *
 * This might not always be that the element doesn't *ever*
 * work when used in Java context, but that precise and
 * intended functionality cannot be guaranteed from native.
 *
 * @author Kaidan Gustave
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
internal annotation class KotlinOnly

internal fun List<Any?>.nullified() = map { if(it == KSONObject.NULL) null else it }

internal inline fun <reified T, reified R> MutableCollection<T>.mapMutable(transform: (T) -> R): MutableCollection<R>
    = mutableMapTo(ArrayList(), transform)

internal inline fun <reified T, reified R, reified C: MutableCollection<R>>
    MutableCollection<T>.mutableMapTo(other: C, transform: (T) -> R) = mapTo(other) { transform(it) }

// Exception tools
internal inline fun require(condition: Boolean, lazy: () -> String) { if(!condition) throw KSONException(lazy()) }
internal inline fun denyIf(condition: Boolean, lazy: () -> String) { if(condition) throw KSONException(lazy()) }

// KSONTokener syntax errors
internal fun KSONTokener.syntaxError(message: String) = KSONException(message + toString())
internal fun KSONTokener.syntaxError(message: String, cause: Throwable) = KSONException(message + toString(), cause)

// Other Utils
internal fun systemProperty(key: String): String? = System.getProperty(key)