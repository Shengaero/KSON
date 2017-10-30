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
 * An internal exception used when errors occur, usually due to invalid or improper
 * arguments.
 *
 * Not all functions in the library use this exception, for example [KSONArray.get]
 * might throw a [IndexOutOfBoundsException] if the index provided is out of bounds.
 *
 * Typically the conditions this is used under are when an error *must* be thrown,
 * either due to catching one that occurred internally or because external interactions
 * are invalid, are when errors that will be thrown do not appropriately describe the
 * error on their own.
 *
 * @author Kaidan Gustave
 */
class KSONException(override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)
{
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}