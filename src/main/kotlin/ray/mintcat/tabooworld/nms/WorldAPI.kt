package ray.mintcat.tabooworld.nms

import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld
import org.bukkit.event.world.WorldInitEvent
import org.bukkit.event.world.WorldLoadEvent
import ray.mintcat.tabooworld.event.TabooWorldCreateEvent
import java.io.File

class WorldAPI {
    var console: MinecraftServer? = null
    var server = Bukkit.getServer() as CraftServer
    var worlds: Map<String, World>? = null
    var pluginManager = Bukkit.getPluginManager()
    var worldContainer = Bukkit.getWorldContainer()
    var logger = Bukkit.getLogger()

    init {
        try {
            val fConsole = CraftServer::class.java.getDeclaredField("console")
            fConsole.isAccessible = true
            console = fConsole[server] as MinecraftServer
            val fWorlds = CraftServer::class.java.getDeclaredField("worlds")
            fWorlds.isAccessible = true
            worlds = fWorlds[server] as Map<String, World>
        } catch (exception: NoSuchFieldException) {
            exception.printStackTrace()
        } catch (exception: IllegalArgumentException) {
            exception.printStackTrace()
        } catch (exception: IllegalAccessException) {
            exception.printStackTrace()
        }
    }

    fun createWorld(creator: WorldCreator): World? {
        val name = creator.name()
        var generator = creator.generator()
        val folder = File(worldContainer, name)
        val world = Bukkit.getWorld(name)
        val type = WorldType.getType(creator.type().getName())
        val generateStructures = creator.generateStructures()
        if (world != null) {
            return world
        }
        require(!(folder.exists() && !folder.isDirectory)) { "File exists with the name '$name' and isn't a folder" }
        if (generator == null) {
            generator = server.getGenerator(name)
        }
        val converter: Convertable = WorldLoaderServer(worldContainer, server.handle.server.dataConverterManager)
        if (converter.isConvertable(name)) {
            logger.info("Converting world '$name'")
            converter.convert(name, object : IProgressUpdate {
                private var b = System.currentTimeMillis()
                override fun a(s: String) {}
                override fun a(i: Int) {
                    if (System.currentTimeMillis() - b >= 1000L) {
                        b = System.currentTimeMillis()
                        MinecraftServer.LOGGER.info("Converting... $i%")
                    }
                }

                override fun c(s: String) {}
            })
        }
        var dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + console!!.worlds.size
        var used = false
        do {
            for (server in console!!.worlds) {
                used = server.dimension == dimension
                if (used) {
                    dimension++
                    break
                }
            }
        } while (used)
        val hardcore = false
        val sdm: IDataManager = ServerNBTManager(worldContainer, name, true, server.handle.server.dataConverterManager)
        var worlddata = sdm.worldData
        var worldSettings: WorldSettings? = null
        if (worlddata == null) {
            worldSettings = WorldSettings(
                creator.seed(),
                EnumGamemode.getById(server.defaultGameMode.value),
                generateStructures,
                hardcore,
                type
            )
            worldSettings.setGeneratorSettings(creator.generatorSettings())
            worlddata = WorldData(worldSettings, name)
        }
        worlddata.checkName(name) // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
        val internal = WorldServer(
            console,
            sdm,
            worlddata,
            dimension,
            console!!.methodProfiler,
            creator.environment(),
            generator
        ).b() as WorldServer
        if (!worlds!!.containsKey(name.lowercase())) {
            return null
        }
        if (worldSettings != null) {
            internal.a(worldSettings)
        }
        internal.scoreboard = server.getScoreboardManager().mainScoreboard.handle
        internal.tracker = EntityTracker(internal)
        internal.addIWorldAccess(WorldManager(console, internal))
        internal.worldData.difficulty = EnumDifficulty.EASY
        internal.setSpawnFlags(true, true)
        console!!.worlds.add(internal)
        if (generator != null) {
            internal.world.populators.addAll(generator.getDefaultPopulators(internal.world))
        }
        pluginManager.callEvent(WorldInitEvent(internal.world))
        logger.info("Preparing start region for level " + (console!!.worlds.size - 1) + " (Seed: " + internal.seed + ")")
        if (internal.world.keepSpawnInMemory) {
            val short1: Short = 196
            var i = System.currentTimeMillis()
            var j = -short1.toInt()
            while (j <= short1) {
                var k = -short1.toInt()
                while (k <= short1) {
                    val l = System.currentTimeMillis()
                    if (l < i) {
                        i = l
                    }
                    if (l > i + 1000L) {
                        val i1 = (short1 * 2 + 1) * (short1 * 2 + 1)
                        val j1 = (j + short1) * (short1 * 2 + 1) + k + 1
                        logger.info("Preparing spawn area for " + name + ", " + j1 * 100 / i1 + "%")
                        i = l
                    }
                    val chunkcoordinates = internal.spawn
                    try {
                        internal.chunkProviderServer.getChunkAt(
                            chunkcoordinates.x + j shr 4,
                            chunkcoordinates.z + k shr 4
                        )
                    } catch (_: Exception) {
                    }
                    k += 16
                }
                j += 16
            }
        }
        pluginManager.callEvent(WorldLoadEvent(internal.world))
        TabooWorldCreateEvent(internal.world).call()
        return internal.world
    }
}