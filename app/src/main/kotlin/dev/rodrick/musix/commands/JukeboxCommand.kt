package dev.rodrick.musix.commands

import de.tr7zw.nbtapi.NBTTileEntity
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationArgument
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.rodrick.musix.JukeboxHopper
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Jukebox
import org.bukkit.inventory.ItemStack

object JukeboxCommand : CommandAPICommand("musix") {
  init {
    withSubcommand(CommandAPICommand("check").apply {
      withArguments(LocationArgument("location", LocationType.BLOCK_POSITION))
      executes(CommandExecutor { sender, args ->
        val location = args[0] as Location
        JukeboxHopper.checkLocation(location)
        if (JukeboxHopper.hoppers.contains(location)) {
          sender.sendMessage("The block at $location is a valid JukeboxHopper")
        }
      })
    })

    withSubcommand(CommandAPICommand("chunk").apply {
      executesPlayer(PlayerCommandExecutor { player, _ ->
        JukeboxHopper.searchInChunk(player.location.chunk)
      })
    })

    withSubcommand(CommandAPICommand("clearJukebox").apply {
      withArguments(LocationArgument("location", LocationType.BLOCK_POSITION))
      executes(CommandExecutor { _, args ->
        val jukebox = (args[0] as Location).block.state as? Jukebox
        if (jukebox == null) {
          CommandAPI.fail("That block isn't a Jukebox!")
        } else {
          jukebox.stopPlaying()
          jukebox.setRecord(null)
          jukebox.update(true)
          NBTTileEntity(jukebox).setItemStack("RecordItem", ItemStack(Material.AIR))
        }
      })
    })
  }
}