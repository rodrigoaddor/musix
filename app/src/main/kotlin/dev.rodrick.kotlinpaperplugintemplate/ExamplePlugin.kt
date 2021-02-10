package dev.rodrick.kotlinpaperplugintemplate

import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class ExamplePlugin: JavaPlugin() {
  companion object {
    lateinit var instance: ExamplePlugin
      private set
  }

  override fun onLoad() {
    logger.info("Now loading...")
  }

  override fun onEnable() {
    logger.info("Now enabling...")
  }

  init {
    instance = this
  }
}