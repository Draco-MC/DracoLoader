package sh.talonfloof.dracoloader.transform

import net.minecraft.launchwrapper.IClassTransformer
import net.minecraft.launchwrapper.Launch

class DracoTransformerWrapper(private val transformer: IDracoTransformer) : IClassTransformer {
    override fun transform(name: String, transformedName: String?, basicClass: ByteArray?): ByteArray? {
        return transformer.transform(Launch.classLoader, name, basicClass)
    }
}