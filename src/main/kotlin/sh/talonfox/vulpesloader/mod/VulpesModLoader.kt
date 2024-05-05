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

import com.google.gson.Gson
import com.google.gson.JsonArray
import net.minecraft.launchwrapper.Launch
import org.apache.commons.io.IOUtils
import sh.talonfox.vulpesloader.LOGGER
import sh.talonfox.vulpesloader.api.VulpesListenerManager
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.jar.JarFile

object VulpesModLoader {
    private val MODS_DIRECTORY: File = File(Launch.minecraftHome, "mods")
    var Mods: MutableMap<String, VulpesMod> = mutableMapOf()
    var ModJars: MutableMap<String, URI> = mutableMapOf()
    var Mixins: MutableList<String> = mutableListOf()

    fun loadMods() {
        LOGGER.info("Attempting to discover Vulpes-compatible mods...")
        MODS_DIRECTORY.mkdir()
        var foundModYet = false
        Files.walkFileTree(MODS_DIRECTORY.toPath(), object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult? {
                if(file.toFile().name.endsWith(".jar")) {
                    try {
                        val jarFile = JarFile(file.toFile())
                        if (jarFile.getEntry("vulpes.json") != null) {
                            val info = Gson().fromJson(
                                IOUtils.toString(
                                    jarFile.getInputStream(jarFile.getJarEntry("vulpes.json")),
                                    StandardCharsets.UTF_8
                                ), VulpesMod::class.java
                            )
                            info.getID()?.let { ModJars.put(it, file.toUri()) }
                            Launch.classLoader.addURL(file.toUri().toURL())
                        } else if (jarFile.getJarEntry("optifine/OptiFineTweaker.class") != null) {
                            val modInfo = VulpesMod()
                            modInfo.setID("optifine")
                            modInfo.setName("OptiFine")
                            modInfo.setAuthors("sp614x")
                            modInfo.setDescription("Provides rendering optimizations to improve Minecraft's performance")
                            modInfo.setVersion("")
                            modInfo.setListeners(JsonArray())
                            (Launch.blackboard["TweakClasses"] as MutableList<String?>?)!!.add("optifine.OptiFineTweaker")
                            LOGGER.info("| "+modInfo.getID()+" | "+modInfo.getName()+" | "+modInfo.getAuthors()+" | "+modInfo.getVersion()+" |")
                            modInfo.getID()?.let { Mods.put(it,modInfo) }
                            modInfo.getID()?.let { ModJars.put(it, file.toUri()) }
                            Launch.classLoader.addURL(file.toUri().toURL())
                        } else {
                            LOGGER.error("Attempted to load incompatible mod, "+file.toFile().nameWithoutExtension)
                        }
                    } catch (throwable: Throwable) {
                        LOGGER.error("An exception occurred while loading mod \""+file.toFile().nameWithoutExtension+"\"\n"+throwable.toString())
                    }
                }
                return super.visitFile(file, attrs);
            }
        })
        val resources: Enumeration<URL> = Launch.classLoader.getResources("vulpes.json")
        while(resources.hasMoreElements()) {
            val url = resources.nextElement()
            val modInfo: VulpesMod =
                Gson().fromJson(IOUtils.toString(url.openStream(), StandardCharsets.UTF_8), VulpesMod::class.java)
            LOGGER.info("| "+modInfo.getID()+" | "+modInfo.getName()+" | "+modInfo.getAuthors()+" | "+modInfo.getVersion()+" |")
            if (modInfo.getMixin() != null) {
                Mixins.add(modInfo.getMixin()!!)
            }
            modInfo.getID()?.let { Mods.put(it,modInfo) }
        }
        for(mod in Mods.values.toList()) {
            val id = mod.getID()
            for(i in mod.getListeners()!!) {
                val className = i.asString!!
                try {
                    val clazz: Class<*> = Launch.classLoader.findClass(className)
                    VulpesListenerManager.addListener(clazz.getDeclaredConstructor().newInstance())
                } catch(e: ClassNotFoundException) {
                    LOGGER.error("Mod \"$id\" specified listener \"$className\" which doesn't contain a valid class, skipping")
                    LOGGER.error("Error reason: $e")
                }
            }
        }
    }
}