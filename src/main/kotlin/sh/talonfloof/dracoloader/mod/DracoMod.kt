package sh.talonfloof.dracoloader.mod

import com.google.gson.JsonArray

class DracoMod {
    private var id: String? = null
    private var name: String? = null
    private var authors: String? = null
    private var version: String? = null
    private var description: String? = null
    private var mixin: String? = null
    private var listeners: JsonArray? = null

    fun getID(): String? = id

    fun getName(): String? = name

    fun getAuthors(): String? = authors

    fun getVersion(): String? = version

    fun getDescription(): String? = description

    fun getMixin(): String? = mixin

    fun getListeners(): JsonArray? = listeners

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

    fun setListeners(value: JsonArray?) {
        if(listeners == null) {
            listeners = value
        }
    }
}