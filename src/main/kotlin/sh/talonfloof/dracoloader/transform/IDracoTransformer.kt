package sh.talonfloof.dracoloader.transform

import net.minecraft.launchwrapper.IClassTransformer
import net.minecraft.launchwrapper.Launch

interface IDracoTransformer : IClassTransformer {
    fun transform(loader: ClassLoader, className: String, originalClassData: ByteArray?) : ByteArray?
    override fun transform(name: String, transformedName: String?, basicClass: ByteArray?): ByteArray? {
        return transform(Launch.classLoader, name, basicClass)
    }
}