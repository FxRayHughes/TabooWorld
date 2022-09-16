package ray.mintcat.tabooworld

import ray.mintcat.tabooworld.nms.WorldAPI
import taboolib.common.platform.Plugin
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object TabooWorld : Plugin() {

    val api = WorldAPI()

    @Config
    lateinit var config: ConfigFile
        private set

}