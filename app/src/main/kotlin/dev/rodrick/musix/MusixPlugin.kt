package dev.rodrick.musix

import dev.jorel.commandapi.CommandAPI
import dev.rodrick.musix.commands.JukeboxCommand
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class MusixPlugin: JavaPlugin() {
  companion object {
    lateinit var instance: MusixPlugin
      private set
  }

  init {
    instance = this
  }

  override fun onLoad() {
    CommandAPI.onLoad(false)
  }

  override fun onEnable() {
    CommandAPI.onEnable(this)
    JukeboxCommand.register()

    Bukkit.getPluginManager().registerEvents(JukeboxHopper, this)
    JukeboxHopper.runTaskTimer(this, 0, 1)
  }
}