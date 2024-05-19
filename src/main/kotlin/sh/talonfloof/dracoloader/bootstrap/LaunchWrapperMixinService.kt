package sh.talonfloof.dracoloader.bootstrap

import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper


open class LaunchWrapperMixinService : MixinServiceLaunchWrapper() {
    override fun getMaxCompatibilityLevel(): CompatibilityLevel? {
        return CompatibilityLevel.JAVA_17
    }

    override fun getPlatformAgents(): Collection<String> {
        return listOf("sh.talonfloof.dracoloader.bootstrap.DracoMixinPlatformAgent")
    }
}