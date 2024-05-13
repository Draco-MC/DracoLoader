package sh.talonfox.vulpesloader.mod

import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource

class VulpesMixinConfigSource(private val modID: String) : IMixinConfigSource {
    override fun getId(): String {
        return modID
    }

    override fun getDescription(): String {
        return modID
    }
}