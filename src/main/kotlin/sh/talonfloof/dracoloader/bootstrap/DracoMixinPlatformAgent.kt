package sh.talonfloof.dracoloader.bootstrap

import org.spongepowered.asm.launch.platform.IMixinPlatformAgent.AcceptResult
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent
import org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import sh.talonfloof.dracoloader.isServer

class DracoMixinPlatformAgent : MixinPlatformAgentDefault(), IMixinPlatformServiceAgent {
    override fun init() {

    }

    override fun getSideName() : String {
        return if(isServer) {
            "SERVER"
        } else {
            "CLIENT"
        }
    }

    override fun getMixinContainers(): MutableCollection<IContainerHandle>? {
        return null
    }
}