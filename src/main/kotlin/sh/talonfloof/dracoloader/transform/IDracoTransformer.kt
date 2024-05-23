package sh.talonfloof.dracoloader.transform

interface IDracoTransformer {
    fun transform(loader: ClassLoader, className: String, originalClassData: ByteArray?) : ByteArray?
}