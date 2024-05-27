package sh.talonfloof.dracoloader.transform

import net.fabricmc.accesswidener.AccessWidenerClassVisitor
import org.objectweb.asm.*
import sh.talonfloof.dracoloader.LOGGER
import sh.talonfloof.dracoloader.api.Side
import sh.talonfloof.dracoloader.isServer
import sh.talonfloof.dracoloader.transform.DracoAccessWidening.accessWidener
import java.util.*

class EnvironmentStrippingData(api: Int, private val envType: String) : ClassVisitor(api) {
    var stripEntireClass = false
    val stripFields: MutableCollection<String> = HashSet()
    val stripMethods: MutableCollection<String> = HashSet()


    private inner class EnvironmentAnnotationVisitor(api: Int, private val onEnvMismatch: Runnable) :
        AnnotationVisitor(api) {
        override fun visitEnum(name: String, descriptor: String?, value: String) {
            if ("value" == name && envType != value) {
                onEnvMismatch.run()
            }
        }
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if (Type.getDescriptor(Side::class.java).equals(descriptor)) {
            return EnvironmentAnnotationVisitor(api) { stripEntireClass = true }
        }
        return null
    }

    private fun visitMemberAnnotation(
        descriptor: String,
        visible: Boolean,
        onEnvMismatch: Runnable
    ): AnnotationVisitor? {
        return if (Type.getDescriptor(Side::class.java).equals(descriptor)) {
            EnvironmentAnnotationVisitor(api, onEnvMismatch)
        } else null
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        return object : FieldVisitor(api) {
            override fun visitAnnotation(annotationDescriptor: String, visible: Boolean): AnnotationVisitor? {
                return visitMemberAnnotation(annotationDescriptor, visible) { stripFields.add(name + descriptor) }
            }
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor {
        val methodId = name + descriptor
        return object : MethodVisitor(api) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                return visitMemberAnnotation(descriptor, visible) { stripMethods.add(methodId) }
            }
        }
    }
}

class EnvironmentStripper(api: Int, visitor: ClassVisitor, private val data: EnvironmentStrippingData) : ClassVisitor(api,visitor) {
    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        return if (data.stripFields.contains(name + descriptor)) null else super.visitField(
            access,
            name,
            descriptor,
            signature,
            value
        )
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor? {
        return if (data.stripMethods.contains(name + descriptor)) null else super.visitMethod(
            access,
            name,
            descriptor,
            signature,
            exceptions
        )
    }
}

object DracoStandardTransformer : IDracoTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClassData: ByteArray?): ByteArray? {
        if(originalClassData == null)
            return originalClassData
        val isMinecraftClass =
            className.startsWith("net.minecraft.") || className.startsWith("com.mojang.") || className.indexOf('.') < 0
        val applyAccessWidener = isMinecraftClass && accessWidener.targets.contains(className)
        val environmentStrip = !isMinecraftClass
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
        if(environmentStrip) {
            val stripData = EnvironmentStrippingData(Opcodes.ASM9,if(isServer) "SERVER" else "CLIENT")
            classReader.accept(stripData, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
            if(stripData.stripEntireClass) {
                LOGGER.error("Class "+className+" cannot be loaded with the environment "+(if(isServer) "SERVER" else "CLIENT"))
                return null
            }
            if (!(stripData.stripFields.isEmpty() && stripData.stripMethods.isEmpty())) {
                visitor = EnvironmentStripper(
                    Opcodes.ASM9,
                    visitor,
                    stripData
                )
                visitCount++
            }
        }
        if(visitCount <= 0)
            return originalClassData
        classReader.accept(visitor, 0)
        return classWriter.toByteArray()
    }
}