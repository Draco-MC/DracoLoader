package sh.talonfloof.dracoloader.transform

import net.minecraft.launchwrapper.Launch

object DracoTransformerRegistry {
    @JvmStatic
    fun addTransformer(transformer: IDracoTransformer) {
        Launch.classLoader.registerTransformer(transformer)
    }
}