package sh.talonfloof.dracoloader.transform

fun interface IDracoTransformer {
    fun transform(loader: ClassLoader, className: String, originalClassData: ByteArray?) : ByteArray?
}