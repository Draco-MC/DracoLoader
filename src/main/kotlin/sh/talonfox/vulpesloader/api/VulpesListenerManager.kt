package sh.talonfox.vulpesloader.api

import net.minecraft.launchwrapper.Launch


object VulpesListenerManager {
    private var listeners: HashMap<Class<*>, MutableList<Class<*>>> = HashMap()

    @JvmStatic
    fun getListeners(listenerInterface: Class<*>): MutableList<*>? {
        return listeners[listenerInterface];
    }

    @JvmStatic
    fun addListener(clazz: Class<*>) {
        for(i in clazz.interfaces.iterator()) {
            var found = false;
            for(j in listeners.keys.toList()) {
                if(i.name == j.name) {
                    listeners[j]?.add(clazz);
                    found = true;
                    break;
                }
            }
            if(!found) {
                listeners[i] = ArrayList();
                listeners[i]?.add(clazz);
            }
        }
    }
}