package sh.talonfloof.dracoloader.mod

import com.google.gson.Gson
import com.llamalad7.mixinextras.MixinExtrasBootstrap
import net.fabricmc.accesswidener.AccessWidenerReader
import net.minecraft.launchwrapper.*
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import sh.talonfloof.dracoloader.LOGGER
import sh.talonfloof.dracoloader.api.DracoListenerManager
import sh.talonfloof.dracoloader.api.ListenerSubscriber
import sh.talonfloof.dracoloader.isServer
import sh.talonfloof.dracoloader.transform.DracoAccessWidening.accessWidener
import sh.talonfloof.dracoloader.transform.DracoStandardTransformer
import sh.talonfloof.dracoloader.transform.DracoTransformerRegistry
import sh.talonfloof.dracoloader.transform.IDracoTransformer
import sh.talonfloof.dracoloader.util.ClassFinder
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.CodeSource
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.io.path.*


object DracoModLoader {
    private val MODS_DIRECTORY: File = File(Launch.minecraftHome, "mods")
    var MODS: MutableMap<String, DracoMod> = mutableMapOf()
    var MOD_PATHS: MutableMap<String, URI> = mutableMapOf()
    var MIXINS: MutableList<String> = mutableListOf()

    @OptIn(ExperimentalPathApi::class)
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
        DracoTransformerRegistry.addTransformer(DracoStandardTransformer)
        val accessWidenerReader = AccessWidenerReader(accessWidener)
        MODS.values.forEach {
            if (it.getAccessWidener() != null) {
                var path = MOD_PATHS[it.getID()!!]!!.toURL().toString()
                if(path.endsWith(".jar")) {
                    val jarFile = JarFile(File(URL(path).toURI()))
                    val entry = jarFile.getEntry(it.getAccessWidener()!!)
                        ?: throw RuntimeException("Missing AccessWidener file ${it.getAccessWidener()!!} for mod ${it.getID()!!}")
                    try {
                        jarFile.getInputStream(entry).bufferedReader().use { reader ->
                            accessWidenerReader.read(
                                reader,
                                "named"
                            )
                        }
                    } catch (e: Exception) {
                        throw java.lang.RuntimeException("Failed to read AccessWidener file from mod " + it.getID()!!, e)
                    }
                } else {
                    path = File(URL(path).toURI()).resolve(it.getAccessWidener()!!).toURI().toString()
                    if (!File(URI(path)).exists()) throw RuntimeException("Missing AccessWidener file ${it.getAccessWidener()!!} for mod ${it.getID()!!}")
                    try {
                        File(URI(path)).bufferedReader().use { reader ->
                            accessWidenerReader.read(
                                reader,
                                "named"
                            )
                        }
                    } catch (e: Exception) {
                        throw java.lang.RuntimeException("Failed to read AccessWidener file from mod " + it.getID()!!, e)
                    }
                }
                }
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
                        throw RuntimeException("Mod \"$id\" specified transformer \"$className\" which doesn't contain a valid class", e)
                    }
                }
            }
        }
        MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED = MixinEnvironment.CompatibilityLevel.JAVA_21
        MixinBootstrap.init()
        MixinExtrasBootstrap.init()
        MIXINS.forEach {
            Mixins.addConfiguration(it, DracoMixinConfigSource(it))
        }
        LOGGER.info("Scanning Discovered Mods... (This may take some time depending on the amount of mods being loaded)")
        ClassFinder.findClasses {
            val classReader = ClassReader(Launch.classLoader!!.getResourceAsStream(it))
            classReader.accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                    if(Type.getDescriptor(ListenerSubscriber::class.java).equals(descriptor)) {
                        return object : AnnotationVisitor(Opcodes.ASM9) {
                            override fun visitEnum(name: String, descriptor: String?, value: String) {
                                if("value" == name) {
                                    if(value == "COMMON" || value == (if(isServer) "SERVER" else "CLIENT")) {
                                        val extIndex = it.lastIndexOf(".class")
                                        assert(extIndex != -1)
                                        val className = it.substring(0,extIndex).replace("/",".")
                                        try {
                                            val clazz: Class<*> = Launch.classLoader.findClass(className)
                                            DracoListenerManager.addListener(clazz.getDeclaredConstructor().newInstance(),value == "COMMON")
                                        } catch (e: ClassNotFoundException) {
                                            throw RuntimeException(
                                                "Listener \"$className\" doesn't contain a valid class",
                                                e
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return null
                }
            },0)
            true
        }
        DracoListenerManager.freeze()
    }
}