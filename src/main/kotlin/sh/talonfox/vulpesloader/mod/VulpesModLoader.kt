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
import net.minecraft.launchwrapper.*
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import sh.talonfox.vulpesloader.LOGGER
import sh.talonfox.vulpesloader.api.VulpesListenerManager
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
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
import kotlin.io.path.toPath


object VulpesModLoader {
    private val MODS_DIRECTORY: File = File(Launch.minecraftHome, "mods")
    var MODS: MutableMap<String, VulpesMod> = mutableMapOf()
    var MOD_PATHS: MutableMap<String, URI> = mutableMapOf()
    var MIXINS: MutableList<String> = mutableListOf()

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
                            /*val info = Gson().fromJson(
                                IOUtils.toString(
                                    jarFile.getInputStream(jarFile.getJarEntry("vulpes.json")),
                                    StandardCharsets.UTF_8
                                ), VulpesMod::class.java
                            )
                            info.getID()?.let { ModJars.put(it, file.toUri()) }*/
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
            var url = resources.nextElement()
            val modInfo: VulpesMod =
                Gson().fromJson(IOUtils.toString(url.openStream(), StandardCharsets.UTF_8), VulpesMod::class.java)
            LOGGER.info("| "+modInfo.getID()+" | "+modInfo.getName()+" | "+modInfo.getAuthors()+" | "+modInfo.getVersion()+" |")
            if (modInfo.getMixin() != null) {
                MIXINS.add(modInfo.getMixin()!!)
            }
            modInfo.getID()?.let { MODS.put(it,modInfo) }
            when(url.protocol) {
                "jar" -> {
                    val spec = url.file
                    val separator = spec.indexOf("!/")
                    if (separator == -1) {
                        throw MalformedURLException("no !/ found in url spec:$spec")
                    }
                    url = URL(spec.substring(0, separator))
                    modInfo.getID()?.let { MOD_PATHS.put(it,url.toURI()) }
                }
                "file" -> {
                    modInfo.getID()?.let { MOD_PATHS.put(it,url.toURI().toPath().parent.toUri()) }
                }
                else -> {
                    throw RuntimeException("Unsupported Protocol: $url")
                }
            }
        }
        for(mod in MODS.values.toList()) {
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