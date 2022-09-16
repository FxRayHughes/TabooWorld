package ray.mintcat.tabooworld.event

import org.bukkit.World
import taboolib.platform.type.BukkitProxyEvent

class TabooWorldCreateEvent(
    val world: World
) : BukkitProxyEvent()