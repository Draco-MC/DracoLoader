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
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.Launch.classLoader
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
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
    var ModJars: MutableMap<String, URL> = mutableMapOf()
    var Mixins: MutableList<String> = mutableListOf()

    fun loadMods() {
        println("Attempting to discover Vulpes-compatible mods...")
        MODS_DIRECTORY.mkdir()
        var foundModYet = false
        Files.walkFileTree(MODS_DIRECTORY.toPath(), object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult? {
                if(file.toFile().name.endsWith(".jar")) {
                    try {
                        val jarFile = JarFile(file.toFile())
                        if (jarFile.getEntry("vulpes.json") != null) {
                            val info = Gson().fromJson(IOUtils.toString(jarFile.getInputStream(jarFile.getJarEntry("vulpes.json")), StandardCharsets.UTF_8), VulpesMod::class.java)
                            info.getID()?.let { ModJars.put(it,file.toUri().toURL()) }
                            Launch.classLoader.addURL(file.toUri().toURL())
                        } else {
                            println("Attempted to load incompatible mod, "+file.toFile().nameWithoutExtension)
                        }
                    } catch (throwable: Throwable) {
                        println("An exception occurred while loading mod \""+file.toFile().nameWithoutExtension+"\"\n"+throwable.toString())
                    }
                }
                return super.visitFile(file, attrs);
            }
        })
        val resources: Enumeration<URL> = classLoader.getResources("vulpes.json")
        while(resources.hasMoreElements()) {
            val url = resources.nextElement()
            val modInfo: VulpesMod =
                Gson().fromJson(IOUtils.toString(url.openStream(), StandardCharsets.UTF_8), VulpesMod::class.java)
            println("| "+modInfo.getName()+" | "+modInfo.getAuthors()+" | "+modInfo.getVersion()+" |")
            if (modInfo.getMixin() != null) {
                Mixins.add(modInfo.getMixin()!!)
            }
            modInfo.getID()?.let { Mods.put(it,modInfo) }
        }
    }
}