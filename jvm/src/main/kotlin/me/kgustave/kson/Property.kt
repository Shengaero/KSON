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
@file:[Suppress("Unused") JvmName("Property")]
package me.kgustave.kson

import java.util.Properties

fun Properties.toKSONObject(): KSONObject {
    val kson = KSONObject()
    if(isNotEmpty()) {
        for((name, value) in this) {
            // Smart cast just to be safe
            if(name is String && value is String)
                kson[name] = value
        }
    }
    return kson
}

fun KSONObject.toProperties(): Properties {
    val props = Properties()
    for((key, value) in only<String>())
        props.put(key, value)
    return props
}