package sh.talonfloof.dracoloader.util

import net.minecraft.launchwrapper.Launch
import sh.talonfloof.dracoloader.mod.DracoModLoader
import java.io.File
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

fun interface ClassFinderVisitor {
    fun visit(t: String): Boolean
}


object ClassFinder {
    fun findClasses(visitor: ClassFinderVisitor) {
        val paths = DracoModLoader.MOD_PATHS
        for (path in paths.values) {
            val file = File(path)
            if (file.exists()) {
                findClasses(file, file, true, visitor)
            }
        }
    }

    private fun findClasses(root: File, file: File, includeJars: Boolean, visitor: ClassFinderVisitor): Boolean {
        if (file.isDirectory()) {
            for (child in file.listFiles()!!) {
                if (!findClasses(root, child, includeJars, visitor)) {
                    return false
                }
            }
        } else {
            if (file.getName().toLowerCase().endsWith(".jar") && includeJars) {
                var jar: JarFile? = null
                try {
                    jar = JarFile(file)
                } catch (ex: Exception) {
                }
                if (jar != null) {
                    val entries: Enumeration<JarEntry> = jar.entries()
                    while (entries.hasMoreElements()) {
                        val entry: JarEntry = entries.nextElement()
                        val name: String = entry.name
                        val extIndex = name.lastIndexOf(".class")
                        if (extIndex > 0) {
                            if (!visitor.visit(name)) {
                                return false
                            }
                        }
                    }
                }
            } else if (file.getName().toLowerCase().endsWith(".class")) {
                if (!visitor.visit(createClassName(root, file))) {
                    return false
                }
            }
        }
        return true
    }

    private fun createClassName(root: File, file: File): String {
        var file: File? = file
        val sb = StringBuffer()
        val fileName: String = file!!.getName()
        sb.append(fileName.substring(0, fileName.lastIndexOf(".class")))
        file = file.getParentFile()
        while (file != null && !file.equals(root)) {
            sb.insert(0, '.').insert(0, file.getName())
            file = file.getParentFile()
        }
        return sb.toString()
    }
}