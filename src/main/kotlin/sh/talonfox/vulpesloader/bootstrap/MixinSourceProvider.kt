package sh.talonfox.vulpesloader.bootstrap

import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource

open class MixinSourceProvider(private val identifier: String) : IMixinConfigSource {
    override fun getId(): String {
        return identifier
    }

    override fun getDescription(): String {
        return "A Mixin Provided by a mod loaded by VulpesLoader"
    }
}