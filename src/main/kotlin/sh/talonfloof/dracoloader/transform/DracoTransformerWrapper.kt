package sh.talonfloof.dracoloader.transform

import net.minecraft.launchwrapper.IClassTransformer
import net.minecraft.launchwrapper.Launch
import org.spongepowered.asm.service.MixinService

class DracoTransformerWrapper(private val transformer: IDracoTransformer) : IClassTransformer {
    override fun transform(name: String, transformedName: String?, basicClass: ByteArray?): ByteArray? {
        val ret = transformer.transform(Launch.classLoader, name, basicClass)
        //MixinService.getService().reEntranceLock.clear()
        return ret
    }
}