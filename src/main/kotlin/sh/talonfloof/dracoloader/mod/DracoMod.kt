package sh.talonfloof.dracoloader.mod

import com.google.gson.JsonArray

class DracoMod {
    private var id: String? = null
    private var name: String? = null
    private var authors: String? = null
    private var version: String? = null
    private var description: String? = null
    private var mixin: String? = null
    private var transformers: JsonArray? = null
    private var listeners: JsonArray? = null
    private var accessWidener: String? = null

    fun getID(): String? = id

    fun getName(): String? = name

    fun getAuthors(): String? = authors

    fun getVersion(): String? = version

    fun getDescription(): String? = description

    fun getMixin(): String? = mixin
    fun getTransformers(): JsonArray? = transformers

    fun getListeners(): JsonArray? = listeners

    fun getAccessWidener(): String? = accessWidener

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
    fun setTransformers(value: JsonArray?) {
        if(transformers == null) {
            transformers = value
        }
    }
    fun setListeners(value: JsonArray?) {
        if(listeners == null) {
            listeners = value
        }
    }
    fun setAccessWidener(value: String?) {
        if(accessWidener == null) {
            accessWidener = value
        }
    }
}