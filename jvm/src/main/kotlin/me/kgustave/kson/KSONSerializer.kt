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
@file:Suppress("MemberVisibilityCanPrivate")
package me.kgustave.kson

import me.kgustave.kson.annotation.KSONConstructor
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * Object that contains methods relating to serializing and de-serializing
 * class instances using [KSONObjects][KSONObject].
 *
 * These functions are done through application of annotations found in the
 * [annotations package][me.kgustave.kson.annotation] of the KSON library.
 *
 * All functions in this unit require **kotlin-reflect** in the classpath,
 * as their abilities depend heavily on metadata and function calls only
 * available through reflection.
 *
 * All functions require the specification of a [Class] or [KClass] to
 * instantiate. This specified type must have at least one constructor
 * annotated with @[KSONConstructor], the arguments of which should
 * correspond to the parameters of the constructor in the order they are
 * provided in the constructor.
 *
 * Example:
 * ```kotlin
 * fun main(args: Array<String>) {
 *     val kson = KSONObject()
 *     kson["name"] = "Shengaero"
 *     kson["age"] = 17
 *
 *     println(KSONSerializer.construct<Person>(kson).introduction())
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
 * @author Kaidan Gustave
 */
@SinceKotlin("1.1")
object KSONSerializer {
    /**
     * Meta-constructs a new instance of [T] using annotations
     * in the [kson annotations][me.kgustave.kson.annotation]
     * package.
     *
     * @param  T
     *         The type of object to construct using this KSONObject.
     *         Should be annotated with @[KSONConstructor].
     *
     * @param  kson
     *         The [KSONObject] construct [T] with.
     *
     * @throws KSONException
     *         If an exception is thrown while calling the constructor.
     * @throws NoSuchElementException
     *         If no constructor is annotated with @[KSONConstructor].
     *
     * @return A new, never-null instance of T using the calling KSONObject
     */
    @Throws(KSONException::class, NoSuchElementException::class)
    inline fun <reified T: Any> construct(kson: KSONObject) = construct(T::class, kson)

    /**
     * An overload for integration with native and other jvm languages
     * where [construct] cannot be called.
     *
     * @param  cla
     *         The [KClass] of object to construct using this
     *         KSONObject. Should be annotated with [@KSON][KSON].
     * @param  kson
     *         The [KSONObject] construct [T] with.
     *
     * @throws KSONException
     *         If an exception is thrown while calling the constructor.
     * @throws NoSuchElementException
     *         If no constructor is annotated with @[KSONConstructor].
     *
     * @return A new, never-null instance of [cla] using the calling
     *         KSONObject.
     */
    @[JvmStatic Throws(KSONException::class, NoSuchElementException::class)]
    fun <T: Any> construct(cla: Class<T>, kson: KSONObject): T {
        require(Modifier.isPublic(cla.modifiers)) { "$cla is not public." }

        if(cla.isAnnotationPresent(KSONConstructor::class.java)) {
            val values = cla.getAnnotation(KSONConstructor::class.java).value.map { kson[it] }

            val target = cla.getConstructor(*values.map { it::class.java }.toTypedArray())
                         ?: throw KSONException("Target constructor of $cla with types specified is not present or is private.")

            try {
                return target.newInstance(values)
            } catch(e: Exception) {
                throw KSONException("Failed to instantiate $cla.",e)
            }
        }

        return cla.constructors.filter {
            // Constructor not annotated
            val conAnn = it.getAnnotation(KSONConstructor::class.java) ?: return@filter false

            val keyParams = conAnn.value

            if(keyParams.isEmpty() || it.parameters.size != keyParams.size) return@filter false

            return@filter keyParams.all { kson.containsKey(it) }
        }.first().run {
            val conAnn = getAnnotation(KSONConstructor::class.java)!!

            try {
                @Suppress("Unchecked_cast")
                newInstance(*conAnn.value.map { kson[it] }.toTypedArray()) as T
            } catch(e: Exception) {
                throw KSONException("Failed to instantiate $cla.",e)
            }
        }
    }

    /**
     * Constructs an instance of [cla] using this KSONObject.
     *
     * @param  cla
     *         The [KClass] of object to construct using this
     *         KSONObject. Should be annotated with [@KSON][KSON].
     * @param  kson
     *         The [KSONObject] construct [T] with.
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
    fun <T: Any> construct(cla: KClass<T>, kson: KSONObject): T {
        if(cla.findAnnotation<KSONConstructor>() != null) {
            val constructor = cla.primaryConstructor
                              ?: throw KSONException("$cla was annotated with @KSONConstructor, " +
                                                     "but primary constructor was null")

            if(constructor.visibility != KVisibility.PUBLIC)
                throw KSONException("Primary constructor of $cla is not public")

            return constructor.run {
                val conAnn = cla.findAnnotation<KSONConstructor>()!!

                try {
                    call(*conAnn.value.map { kson[it] }.toTypedArray())
                } catch(e: Exception) {
                    throw KSONException("Failed to instantiate $cla.",e)
                }
            }
        }

        return cla.constructors.filter {
            // Constructor not annotated
            val conAnn = it.findAnnotation<KSONConstructor>() ?: return@filter false

            if(it.visibility != KVisibility.PUBLIC)
                throw KSONException("Constructor of $cla annotated with @KSONConstructor is not public")

            val keyParams = conAnn.value

            if(keyParams.isEmpty() || it.parameters.size != keyParams.size) return@filter false

            return@filter keyParams.all { kson.containsKey(it) }
        }.first().run {
            val conAnn = findAnnotation<KSONConstructor>()!!

            try {
                call(*conAnn.value.map { kson[it] }.toTypedArray())
            } catch(e: Exception) {
                throw KSONException("Failed to instantiate $cla.",e)
            }
        }
    }
}