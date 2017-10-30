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
package me.kgustave.kson.json

import me.kgustave.kson.KSONException
import me.kgustave.kson.KSONObject
import me.kgustave.kson.KSONSerializer
import org.json.JSONObject
import kotlin.reflect.KClass

inline val <reified T: JSONObject> T.kson: KSONObject
    inline get() = KSONObject(toMap())

inline val KSONObject.json: JSONObject
    inline get() = JSONObject(toString())

@Throws(KSONException::class, NoSuchElementException::class)
inline fun <reified T: Any> JSONObject.construct(cla: KClass<T> = T::class) = KSONSerializer.construct(cla, this.kson)

@Throws(KSONException::class, NoSuchElementException::class)
inline fun <reified T: Any> JSONObject.construct(cla: Class<T> = T::class.java) = KSONSerializer.construct(cla, this.kson)