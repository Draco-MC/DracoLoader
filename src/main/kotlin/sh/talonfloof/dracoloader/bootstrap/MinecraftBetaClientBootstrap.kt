/*
 * Copyright 2022 Vulpes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.talonfloof.dracoloader.bootstrap

import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.LaunchClassLoader
import org.spongepowered.asm.mixin.MixinEnvironment
import java.io.File

class MinecraftBetaClientBootstrap : ITweaker {
    override fun acceptOptions(args: MutableList<String>?, gameDir: File?, assetsDir: File?, profile: String?) {

    }

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader?) {
        MixinEnvironment.getCurrentEnvironment().side = MixinEnvironment.Side.CLIENT
        try {
            val clazz: Class<*> = classLoader!!.findClass("sh.talonfloof.dracoloader.MainKt")
            clazz.getMethod("main", Array<String>::class.java)
                .invoke(null, arrayOf<String>())
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    override fun getLaunchTarget(): String = "net.minecraft.client.MinecraftApplet"

    override fun getLaunchArguments(): Array<String> = arrayOf()
}