package sh.talonfloof.dracoloader.mod

import com.google.gson.Gson
import com.llamalad7.mixinextras.MixinExtrasBootstrap
import net.minecraft.launchwrapper.*
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import sh.talonfloof.dracoloader.LOGGER
import sh.talonfloof.dracoloader.api.DracoListenerManager
import sh.talonfloof.dracoloader.isServer
import sh.talonfloof.dracoloader.transform.DracoTransformerRegistry
import sh.talonfloof.dracoloader.transform.IDracoTransformer
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


object DracoModLoader {
    private val MODS_DIRECTORY: File = File(Launch.minecraftHome, "mods")
    var MODS: MutableMap<String, DracoMod> = mutableMapOf()
    var MOD_PATHS: MutableMap<String, URI> = mutableMapOf()
    var MIXINS: MutableList<String> = mutableListOf()

    fun loadMods() {
        LOGGER.info("Attempting to discover Draco-compatible mods...")
        MODS_DIRECTORY.mkdir()
        var foundModYet = false
        Files.walkFileTree(MODS_DIRECTORY.toPath(), object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult? {
                if(file.toFile().name.endsWith(".jar")) {
                    try {
                        val jarFile = JarFile(file.toFile())
                        if (jarFile.getEntry("draco.json") != null) {
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
        val resources: Enumeration<URL> = Launch.classLoader.getResources("draco.json")
        while(resources.hasMoreElements()) {
            var url = resources.nextElement()
            val modInfo: DracoMod =
                Gson().fromJson(IOUtils.toString(url.openStream(), StandardCharsets.UTF_8), DracoMod::class.java)
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
        MixinBootstrap.init()
        MixinExtrasBootstrap.init()
        MIXINS.forEach {
            Mixins.addConfiguration(it, DracoMixinConfigSource(it))
        }
        for(mod in MODS.values.toList()) {
            val id = mod.getID()
            if(mod.getTransformers() != null) {
                for (i in mod.getTransformers()!!) {
                    val className = i.asString!!
                    try {
                        val clazz: Class<*> = Launch.classLoader.findClass(className)
                        DracoTransformerRegistry.addTransformer(clazz.getDeclaredConstructor().newInstance() as IDracoTransformer)
                    } catch (e: ClassNotFoundException) {
                        LOGGER.error("Mod \"$id\" specified transformer \"$className\" which doesn't contain a valid class, skipping")
                        LOGGER.error("Error reason: $e")
                    }
                }
            }
        }
        for(mod in MODS.values.toList()) {
            val id = mod.getID()
            if(mod.getListeners() != null) {
                for (i in mod.getListeners()!!) {
                    val className = i.asString!!
                    try {
                        val clazz: Class<*> = Launch.classLoader.findClass(className)
                        DracoListenerManager.addListener(clazz.getDeclaredConstructor().newInstance())
                    } catch (e: ClassNotFoundException) {
                        LOGGER.error("Mod \"$id\" specified listener \"$className\" which doesn't contain a valid class, skipping")
                        LOGGER.error("Error reason: $e")
                    }
                }
            }
        }
    }
}