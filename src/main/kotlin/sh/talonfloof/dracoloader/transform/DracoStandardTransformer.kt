package sh.talonfloof.dracoloader.transform

import net.fabricmc.accesswidener.AccessWidenerClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import sh.talonfloof.dracoloader.transform.DracoAccessWidening.accessWidener


object DracoStandardTransformer : IDracoTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClassData: ByteArray?): ByteArray? {
        val isMinecraftClass =
            className.startsWith("net.minecraft.") || className.startsWith("com.mojang.") || className.indexOf('.') < 0
        val applyAccessWidener = isMinecraftClass && accessWidener.targets.contains(className)
        val classReader = ClassReader(originalClassData)
        val classWriter = ClassWriter(classReader, 0)
        var visitor: ClassVisitor = classWriter
        var visitCount = 0
        if (applyAccessWidener) {
            visitor = AccessWidenerClassVisitor.createClassVisitor(
                Opcodes.ASM9,
                visitor,
                accessWidener
            )
            visitCount++
        }
        if(visitCount <= 0)
            return originalClassData
        classReader.accept(visitor, 0)
        return classWriter.toByteArray()
    }
}