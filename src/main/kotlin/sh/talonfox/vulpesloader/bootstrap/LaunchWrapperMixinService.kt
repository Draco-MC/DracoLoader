package sh.talonfox.vulpesloader.bootstrap

import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper


open class LaunchWrapperMixinService : MixinServiceLaunchWrapper() {
    override fun getMinCompatibilityLevel(): CompatibilityLevel? {
        return null
    }
    override fun getMaxCompatibilityLevel(): CompatibilityLevel? {
        return null
    }
}