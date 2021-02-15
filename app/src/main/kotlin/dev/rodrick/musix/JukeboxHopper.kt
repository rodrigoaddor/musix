package dev.rodrick.musix

import de.tr7zw.nbtapi.NBTTileEntity
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.*
import org.bukkit.block.data.Directional
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

object JukeboxHopper : Listener, BukkitRunnable() {
  private val delayKey = NamespacedKey(MusixPlugin.instance, "delay")
  val hoppers = mutableSetOf<Location>()

  private val faces =
    arrayOf(BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST)

  fun checkLocation(location: Location) {
    if (location.block.type == Material.HOPPER) {
      if (
        location.clone().add((location.block.blockData as Directional).facing.direction).block.type == Material.JUKEBOX ||
        location.clone().add(0.0, 1.0, 0.0).block.type == Material.JUKEBOX
      ) {
        hoppers.add(location)
      }

    } else if (location.block.type == Material.JUKEBOX) {
      val jukebox = location.block.state as Jukebox
      for (face in faces) {
        val block = jukebox.location.clone().add(face.direction).block
        if (block.type == Material.HOPPER &&
          (face == BlockFace.DOWN || (block.blockData as Directional).facing == face.oppositeFace)
        ) {
          hoppers.add(block.location)
        }
      }
    }
  }

  fun searchInChunk(chunk: Chunk) {
    val jukeboxes = chunk.tileEntities.filterIsInstance<Jukebox>()
    for (jukebox in jukeboxes) {
      checkLocation(jukebox.location)
    }
  }

  @EventHandler
  private fun onPlayerPlace(e: BlockPlaceEvent) = checkLocation(e.block.location)

  @EventHandler(ignoreCancelled = true)
  private fun onChunkLoad(e: ChunkLoadEvent) = searchInChunk(e.chunk)

  @EventHandler(ignoreCancelled = true)
  private fun onDropperInsert(e: BlockDispenseEvent) {
    if (e.block.type != Material.DROPPER) return
    val dropper = e.block.state as Dropper
    val jukebox = dropper.location.clone().add((dropper.blockData as Directional).facing.direction).block.state as? Jukebox
    if (jukebox != null) {
      e.isCancelled = true

      if (jukebox.playing.isAir && e.item.type.isRecord) {
        MusixPlugin.instance.logger.info(dropper.inventory.contents.joinToString { it?.type?.name ?: "EMPTY" })
        object : BukkitRunnable() {
          var clear = false
          override fun run() {
            if (!clear) {
              val item = dropper.inventory.contents.filter { it?.isSimilar(e.item) == true }.randomOrNull()
              if (item != null) {
                item.amount--
                dropper.update()
                jukebox.setRecord(e.item)
                jukebox.persistentDataContainer.set(delayKey, PersistentDataType.BYTE, 1)
                jukebox.update()
                clear = true
              } else {
                cancel()
              }
            } else {
              jukebox.persistentDataContainer.remove(delayKey)
              jukebox.update()
              cancel()
            }
          }
        }.runTaskTimer(MusixPlugin.instance, 0, 1)
      }
    }
  }

  override fun run() {
    val iterator = hoppers.iterator()
    while (iterator.hasNext()) {
      val location = iterator.next()

      val hopper = if (location.isWorldLoaded && location.block.state is Hopper) location.block.state as Hopper else {
        iterator.remove()
        continue
      }

      val nbt = NBTTileEntity(hopper)
      if (hopper.block.isBlockIndirectlyPowered || nbt.getInteger("TransferCooldown") > 0) return

      val pushJukebox = (hopper.blockData as Directional).facing.let { facing ->
        hopper.location.clone().add(facing.direction)
      }.block.state as? Jukebox

      val pullJukebox = hopper.location.clone().add(0.0, 1.0, 0.0).block.state as? Jukebox

      pushJukebox?.let { jukebox ->
        if (!jukebox.hasDelay && jukebox.playing.isAir) {
          val disc = hopper.snapshotInventory.contents.firstOrNull { it?.type?.isRecord == true }
          if (disc != null) {
            hopper.snapshotInventory.remove(disc)
            hopper.update()
            jukebox.setRecord(disc)
            jukebox.update()
            nbt.setInteger("TransferCooldown", 8)
          }
        }
      }

      pullJukebox?.let { jukebox ->
        if (!jukebox.hasDelay && !jukebox.playing.isAir && hopper.snapshotInventory.filterNotNull().size < 5) {
          hopper.snapshotInventory.addItem(jukebox.record)
          hopper.update()
          nbt.setInteger("TransferCooldown", 8)
          jukebox.stopPlaying()
          jukebox.setRecord(null)
          jukebox.update(true)
          NBTTileEntity(jukebox).setItemStack("RecordItem", ItemStack(Material.AIR))
        }
      }
    }
  }

  private val Jukebox.hasDelay: Boolean
    get() = persistentDataContainer.has(
      delayKey,
      PersistentDataType.BYTE
    )
}