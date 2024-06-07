package sh.talonfloof.dracoloader

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import sh.talonfloof.dracoloader.mod.DracoModLoader

val LOGGER: Logger = LogManager.getLogger("DracoLoader")
var isServer: Boolean = false

fun main(args: Array<String>?) {
    LOGGER.info("Draco Loader is now starting...")
    DracoModLoader.loadMods()
}