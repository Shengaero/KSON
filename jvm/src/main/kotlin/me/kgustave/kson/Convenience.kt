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
@file:[JvmName("KSONJvmConvenience") Suppress("Unused")]
package me.kgustave.kson

import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files.*
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.*

// File Handling
@Throws(IOException::class)
fun readKSONObject(vararg files: String, charset: Charset = Charsets.UTF_8,
                   startInRunningDirectory: Boolean = false): KSONObject {
    require(files.isNotEmpty()) { "No path was specified!" }

    val file = if(startInRunningDirectory) {
        val userDir = systemProperty("user.dir")
                      ?: throw KSONException("Could not retrieve user.dir from system properties.")
        Paths.get(userDir, *files).toFile()
    } else {
        Paths.get(files[0], *if(files.size == 1) emptyArray() else files.copyOfRange(1, files.size)).toFile()!!
    }!!

    return readKSONObject(file, charset)
}

@Throws(IOException::class)
fun readKSONObject(file: File, charset: Charset = Charsets.UTF_8) = readKSONObject(file.reader(charset))

@Throws(IOException::class)
fun readKSONObject(reader: InputStreamReader) = KSONObject(KSONTokener(reader))

@Throws(IOException::class)
fun readKSONArray(vararg files: String, charset: Charset = Charsets.UTF_8,
                  startInRunningDirectory: Boolean = false): KSONArray {
    require(files.isNotEmpty()) { "No path was specified!" }

    val file = if(startInRunningDirectory) {
        val userDir = systemProperty("user.dir")
                      ?: throw KSONException("Could not retrieve user.dir from system properties.")
        Paths.get(userDir, *files).toFile()
    } else {
        Paths.get(files[0], *if(files.size == 1) emptyArray() else files.copyOfRange(1, files.size)).toFile()!!
    }!!

    return readKSONArray(file, charset)
}

@Throws(IOException::class)
fun readKSONArray(file: File, charset: Charset = Charsets.UTF_8) = readKSONArray(file.reader(charset))

@Throws(IOException::class)
fun readKSONArray(reader: InputStreamReader) = KSONArray(KSONTokener(reader))

@Throws(IOException::class)
fun writeKSON(obj: KSONObject, file: File, indentFactor: Int = 0, charset: Charset = Charsets.UTF_8,
              createIfNotPresent: Boolean = false, truncateExisting: Boolean = true) {

    if(!file.exists() && createIfNotPresent)
        file.createNewFile()

    write(file.toPath(),
          obj.toString(indentFactor).toByteArray(charset),
          *if(truncateExisting) arrayOf(WRITE, TRUNCATE_EXISTING) else arrayOf(WRITE))
}