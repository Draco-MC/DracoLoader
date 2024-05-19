package sh.talonfloof.dracoloader.bootstrap

import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.LaunchClassLoader
import org.spongepowered.asm.mixin.MixinEnvironment
import java.io.File


open class MinecraftClientBootstrap : ITweaker {
    private var Args: ArrayList<String>? = null

    private fun addArg(name: String, value: String?) {
        Args!!.add(name)
        if (value != null) {
            Args!!.add(value)
        }
    }

    override fun acceptOptions(args: MutableList<String>?, gameDir: File?, assetsDir: File?, profile: String?) {
        this.Args = args?.let { ArrayList(it) }

        addArg("--version", profile)
        if(!this.Args!!.contains("--assetsDir")) {
            addArg("--assetsDir", assetsDir!!.path)
        }
        if(!this.Args!!.contains("--accessToken")) {
            addArg("--accessToken", "0")
        }
    }

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader?) {
        MixinEnvironment.getCurrentEnvironment().side = MixinEnvironment.Side.CLIENT
        try {
            val clazz: Class<*> = classLoader!!.findClass("sh.talonfloof.dracoloader.MainKt")
            clazz.getMethod("main", Array<String>::class.java)
                .invoke(null, Args?.toTypedArray())
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    override fun getLaunchTarget(): String = "net.minecraft.client.main.Main"

    override fun getLaunchArguments(): Array<String>? {
        return Args?.toTypedArray()
    }
}
