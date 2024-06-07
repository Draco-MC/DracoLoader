package sh.talonfloof.dracoloader.api

import sh.talonfloof.dracoloader.isServer

object DracoEnvironment {
    @JvmStatic
    fun getEnvironment() : EnvironmentType = if(isServer) EnvironmentType.SERVER else EnvironmentType.CLIENT
}