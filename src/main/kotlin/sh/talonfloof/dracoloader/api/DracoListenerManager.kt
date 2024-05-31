package sh.talonfloof.dracoloader.api


object DracoListenerManager {
    private var frozen: Boolean = false
    private var commonListeners: HashMap<Class<*>, MutableList<Any>> = HashMap()
    private var listeners: HashMap<Class<*>, MutableList<Any>> = HashMap()
    private var orderedListeners: HashMap<Class<*>, MutableList<Any>> = HashMap()

    @JvmStatic
    fun getListeners(listenerInterface: Class<*>): MutableList<*>? {
        if(!frozen) {
            if (orderedListeners[listenerInterface] == null) {
                val l = mutableListOf<Any>()
                if (commonListeners[listenerInterface] != null) {
                    l.addAll(commonListeners[listenerInterface]!!)
                }
                if (listeners[listenerInterface] != null) {
                    l.addAll(listeners[listenerInterface]!!)
                }
                orderedListeners[listenerInterface] = l
            }
            return orderedListeners[listenerInterface]
        } else {
            throw RuntimeException("Attempted to get listeners with the interface ${listenerInterface.name} before the listener manager was frozen!")
        }
    }
    @JvmStatic
    fun addListener(clazz: Any, isCommon: Boolean) {
        if(!frozen) {
            for (i in clazz.javaClass.interfaces.iterator()) {
                if (isCommon) {
                    if (!commonListeners.containsKey(i)) {
                        commonListeners[i] = ArrayList()
                    }
                    commonListeners[i]?.add(clazz)
                } else {
                    if (!listeners.containsKey(i)) {
                        listeners[i] = ArrayList()
                    }
                    listeners[i]?.add(clazz)
                }
            }
        } else {
            throw RuntimeException("Attempted to register a listener after the listener manager is frozen!")
        }
    }

    @JvmStatic
    fun freeze() {
        if(!frozen) {
            frozen = true
        }
    }
}