/*
 * Copyright 2022 Vulpes
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

package sh.talonfox.vulpesloader.mod

import com.google.gson.JsonObject

class VulpesMod {
    private var id: String? = null
    private var name: String? = null
    private var authors: String? = null
    private var version: String? = null
    private var description: String? = null
    private var mixin: String? = null
    private var entrypoints: JsonObject? = null

    fun getID(): String? = id

    fun getName(): String? = name

    fun getAuthors(): String? = authors

    fun getVersion(): String? = version

    fun getDescription(): String? = description

    fun getMixin(): String? = mixin

    fun getEntrypoints(): JsonObject? = entrypoints

    fun setID(value: String?) {
        if(id == null) {
            id = value
        }
    }

    fun setName(value: String?) {
        if(name == null) {
            name = value
        }
    }

    fun setAuthors(value: String?) {
        if(authors == null) {
            authors = value
        }
    }

    fun setVersion(value: String?) {
        if(version == null) {
            version = value
        }
    }

    fun setDescription(value: String?) {
        if(description == null) {
            description = value
        }
    }

    fun setMixin(value: String?) {
        if(mixin == null) {
            mixin = value
        }
    }

    fun setEntrypoints(value: JsonObject?) {
        if(entrypoints == null) {
            entrypoints = value
        }
    }
}