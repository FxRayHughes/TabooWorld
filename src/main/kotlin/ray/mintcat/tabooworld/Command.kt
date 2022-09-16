package ray.mintcat.tabooworld

import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ray.mintcat.tabooworld.utils.error
import ray.mintcat.tabooworld.utils.info
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit
import taboolib.expansion.createHelper

@CommandHeader("tabooworld", ["tw"])
object Command {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val list = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.info("当前加载的世界: ")
            Bukkit.getWorlds().forEach { t ->
                sender.info("- &f${t.name}")
            }
        }
    }

    @CommandBody
    val tp = subCommand {
        dynamic(commit = "世界名称") {
            suggestion<CommandSender> { sender, context ->
                Bukkit.getWorlds().map { it.name }
            }
            dynamic(commit = "目标") {
                suggestion<CommandSender> { sender, context ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                execute<CommandSender> { sender, context, argument ->
                    val target = Bukkit.getPlayerExact(context.argument(0)) ?: return@execute run {
                        sender.error("玩家不存在")
                    }
                    submit(async = true) {
                        val world = Bukkit.getWorld(context.argument(0)) ?: return@submit run {
                            sender.error("世界不存在")
                        }
                        target.teleport(world.spawnLocation)
                        sender.info("已传送至 &f${context.argument(0)}")
                    }
                }
            }
            execute<Player> { sender, context, argument ->
                val world = Bukkit.getWorld(context.argument(0)) ?: return@execute run {
                    sender.error("世界不存在")
                }
                sender.teleport(world.spawnLocation)
                sender.info("已传送至 &f${context.argument(0)}")
            }
        }
    }

    @CommandBody
    val create = subCommand {
        dynamic(commit = "世界名称") {
            execute<CommandSender> { sender, context, argument ->
                sender.info("开始创建 &f${context.argument(0)}")
                submit(async = true) {
                    TabooWorld.api.createWorld(WorldCreator(context.argument(0)))
                    sender.info("&f${context.argument(0)} &7创建完成")
                }
            }
        }
    }

    @CommandBody
    val remove = subCommand {
        dynamic(commit = "世界名称") {
            suggestion<CommandSender> { sender, context ->
                Bukkit.getWorlds().map { it.name }
            }
            execute<CommandSender> { sender, context, argument ->
                sender.info("开始删除 &f${context.argument(0)}")
                submit(async = true) {
                    val world = Bukkit.getWorld(context.argument(0)) ?: return@submit run {
                        sender.error("世界不存在")
                    }
                    Bukkit.unloadWorld(world, true)
                    sender.info("&f${context.argument(0)} &7删除完成")
                }
            }
        }
    }

}