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

package sh.talonfox.vulpesloader.api

import net.minecraft.launchwrapper.Launch
import sh.talonfox.vulpesloader.mod.VulpesModLoader

object VulpesEntrypointExecutor {
    fun executeEntrypoint(name: String) {
        if(!name.contains(":") || name.startsWith(":") || name.endsWith(":")) {
            println("A mod attempted to load an invalid entrypoint named: \"$name\"!")
            return
        }
        VulpesModLoader.Mods.forEach { (id, mod) ->
            if(mod.getEntrypoints() != null) {
                val entrypoints = mod.getEntrypoints()!!
                if(entrypoints.has(name)) {
                    val className = entrypoints.get(name).asString
                    try {
                        val clazz: Class<*> = Launch.classLoader.findClass(className)
                        var hasEntrypointInterface = false
                        for(i in clazz.interfaces.iterator()) {
                            if(i.name == IEntrypoint::javaClass.name) {
                                hasEntrypointInterface = true
                                break
                            }
                        }
                        if(!hasEntrypointInterface) {
                            println("\"$className\" in Mod \"$name\" doesn't implement IEntrypoint, skipping.")
                        } else {
                            clazz.getMethod("enter").invoke(null,null)
                        }
                    } catch(e: ClassNotFoundException) {
                        println("Mod \"$id\" linked entrypoint \"$name\" to non-existent class \"$className\", skipping")
                    } catch(e: NoSuchMethodException) {
                        println("Mod \"$id\" somehow doesn't have the enter method in class \"$className\" even though interface IEntrypoint requires it?!\n(Potential VulpesLoader Bug!)")
                    }
                }
            }
        }
    }
}