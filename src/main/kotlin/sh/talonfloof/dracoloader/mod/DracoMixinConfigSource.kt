package sh.talonfloof.dracoloader.mod

import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource

class DracoMixinConfigSource(private val modID: String) : IMixinConfigSource {
    override fun getId(): String {
        return modID
    }

    override fun getDescription(): String {
        return modID
    }
}