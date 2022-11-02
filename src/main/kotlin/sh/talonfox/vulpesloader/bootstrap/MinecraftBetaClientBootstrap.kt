package sh.talonfox.vulpesloader.bootstrap

import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.LaunchClassLoader
import org.spongepowered.asm.mixin.MixinEnvironment
import java.io.File

class MinecraftBetaClientBootstrap : ITweaker {
    override fun acceptOptions(args: MutableList<String>?, gameDir: File?, assetsDir: File?, profile: String?) {

    }

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader?) {
        try {
            val clazz: Class<*> = classLoader!!.findClass("sh.talonfox.vulpesloader.MainKt")
            clazz.getMethod("main", Array<String>::class.java)
                .invoke(null, arrayOf<String>())
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }

        MixinEnvironment.getDefaultEnvironment().side = MixinEnvironment.Side.CLIENT
    }

    override fun getLaunchTarget(): String = "net.minecraft.client.MinecraftClient"

    override fun getLaunchArguments(): Array<String> = arrayOf()
}