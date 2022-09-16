package ray.mintcat.tabooworld

import org.bukkit.Bukkit
import ray.mintcat.tabooworld.event.TabooWorldCreateEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit

object Listener {

    @SubscribeEvent
    fun mv(event: TabooWorldCreateEvent) {
        if (TabooWorld.config.getBoolean("Multiverse-Core", true)) {
            submit {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"mv import ${event.world.name} normal")
            }
        }
    }

}