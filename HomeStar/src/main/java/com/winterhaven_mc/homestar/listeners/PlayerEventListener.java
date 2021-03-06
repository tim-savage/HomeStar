package com.winterhaven_mc.homestar.listeners;

import com.winterhaven_mc.homestar.PluginMain;
import com.winterhaven_mc.homestar.messages.Message;
import com.winterhaven_mc.homestar.sounds.SoundId;
import com.winterhaven_mc.homestar.messages.MessageId;

import com.winterhaven_mc.homestar.util.HomeStar;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static com.winterhaven_mc.homestar.messages.MessageId.*;


/**
 * Implements player event listener for HomeStar events
 */
public final class PlayerEventListener implements Listener {

	// reference to main class
	private final PluginMain plugin;

	// set of craft table materials
	private final Set<Material> craftTables =  Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList(
					Material.CARTOGRAPHY_TABLE,
					Material.CRAFTING_TABLE,
					Material.FLETCHING_TABLE,
					Material.LOOM,
					Material.SMITHING_TABLE,
					Material.STONECUTTER)));


	/**
	 * Class constructor for PlayerEventListener
	 *
	 * @param plugin reference to this plugin's main class
	 */
	public PlayerEventListener(final PluginMain plugin) {

		// reference to main
		this.plugin = plugin;

		// register events in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	/**
	 * PlayerInteract event handler
	 *
	 * @param event PlayerInteractEvent handled by this method
	 */
	@EventHandler
	final void onPlayerUse(final PlayerInteractEvent event) {

		// get player
		final Player player = event.getPlayer();

		// if cancel-on-interaction is configured true, check if player is in warmup hashmap
		if (plugin.getConfig().getBoolean("cancel-on-interaction")) {

			// if player is in warmup hashmap, check if they are interacting with a block (not air)
			if (plugin.teleportManager.isWarmingUp(player)) {

				// if player is interacting with a block, cancel teleport, output message and return
				if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)
						|| event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

					// if player's last teleport initiated time is less than x ticks (def: 2), do nothing and return
					// this is a workaround for event double firing (once for each hand) on every player interaction
					if (!plugin.teleportManager.isInitiated(player)) {
						return;
					}

					// cancel teleport
					plugin.teleportManager.cancelTeleport(player);

					// send cancelled teleport message
					Message.create(player, TELEPORT_CANCELLED_INTERACTION).send();

					// play cancelled teleport sound
					plugin.soundConfig.playSound(player, SoundId.TELEPORT_CANCELLED);
					return;
				}
			}
		}

		// if item used is not a HomeStar, do nothing and return
		if (!HomeStar.isItem(event.getItem())) {
			return;
		}

		// get event action
		Action action = event.getAction();

		// if event action is PHYSICAL (not left click or right click), do nothing and return
		if (action.equals(Action.PHYSICAL)) {
			return;
		}

		// if event action is left-click, and left-click is config disabled, do nothing and return
		if (action.equals(Action.LEFT_CLICK_BLOCK)
				|| action.equals(Action.LEFT_CLICK_AIR)
				&& !plugin.getConfig().getBoolean("left-click")) {
			return;
		}

		// if player is not warming
		if (!plugin.teleportManager.isWarmingUp(player)) {

			// get clicked block
			Block block = event.getClickedBlock();

			// check if clicked block is air (null)
			if (block != null) {

				// check that player is not sneaking, to interact with blocks
				if (!event.getPlayer().isSneaking()) {

					// allow use of doors, gates and trap doors with item in hand
					if (block.getBlockData() instanceof Openable) {
						return;
					}

					// allow use of switches with item in hand
					if (block.getBlockData() instanceof Switch) {
						return;
					}

					// allow use of containers and other tile entity blocks with item in hand
					if (block.getState() instanceof TileState) {
						return;
					}

					// allow use of crafting tables with item in hand
					if (craftTables.contains(block.getType())) {
						return;
					}
				}
			}

			// cancel event
			event.setCancelled(true);

			// if players current world is not enabled in config, do nothing and return
			if (!plugin.worldManager.isEnabled(player.getWorld())) {
				Message.create(player, TELEPORT_FAIL_WORLD_DISABLED).send();
				plugin.soundConfig.playSound(player, SoundId.TELEPORT_DENIED_WORLD_DISABLED);
				return;
			}

			// if player does not have homestar.use permission, send message and return
			if (!player.hasPermission("homestar.use")) {
				Message.create(player, MessageId.PERMISSION_DENIED_USE).send();
				plugin.soundConfig.playSound(player, SoundId.TELEPORT_DENIED_PERMISSION);
				return;
			}

			// if shift-click configured and player is not sneaking,
			// send teleport fail shift-click message, cancel event and return
			if (plugin.getConfig().getBoolean("shift-click")
					&& !player.isSneaking()) {
				Message.create(player, TELEPORT_FAIL_SHIFT_CLICK).send();
				return;
			}

			// initiate teleport
			plugin.teleportManager.initiateTeleport(player);
		}
	}


	/**
	 * Player death event handler
	 *
	 * @param event PlayerDeathEvent handled by this method
	 */
	@EventHandler
	final void onPlayerDeath(final PlayerDeathEvent event) {

		// get event player
		Player player = event.getEntity();

		// cancel any pending teleport for player
		plugin.teleportManager.cancelTeleport(player);
	}


	/**
	 * Player quit event handler
	 *
	 * @param event PlayerQuitEvent handled by this method
	 */
	@EventHandler
	final void onPlayerQuit(final PlayerQuitEvent event) {

		Player player = event.getPlayer();

		// cancel any pending teleport for player
		plugin.teleportManager.cancelTeleport(player);
	}


	/**
	 * Prepare Item Craft event handler<br>
	 * Prevents HomeStar items from being used in crafting recipes if configured
	 *
	 * @param event PrepareItemCraftEvent handled by this method
	 */
	@EventHandler
	final void onCraftPrepare(final PrepareItemCraftEvent event) {

		// if allow-in-recipes is true in configuration, do nothing and return
		if (plugin.getConfig().getBoolean("allow-in-recipes")) {
			return;
		}

		// if crafting inventory contains HomeStar item, set result item to null
		for (ItemStack itemStack : event.getInventory()) {
			if (HomeStar.isItem(itemStack)) {
				event.getInventory().setResult(null);
			}
		}
	}


	/**
	 * EntityDamageByEntity event handler<br>
	 * Cancels pending teleport if player takes damage during warmup
	 *
	 * @param event EntityDamageEvent handled by this method
	 */
	@EventHandler
	final void onEntityDamage(final EntityDamageEvent event) {

		// if event is already cancelled, do nothing and return
		if (event.isCancelled()) {
			return;
		}

		// if cancel-on-damage configuration is true, check if damaged entity is player
		if (plugin.getConfig().getBoolean("cancel-on-damage")) {

			Entity entity = event.getEntity();

			// if damaged entity is player, check for pending teleport
			if (entity instanceof Player) {

				// if player is in warmup hashmap, cancel teleport and send player message
				if (plugin.teleportManager.isWarmingUp((Player) entity)) {
					plugin.teleportManager.cancelTeleport((Player) entity);
					Message.create(entity, TELEPORT_CANCELLED_DAMAGE).send();
					plugin.soundConfig.playSound(entity, SoundId.TELEPORT_CANCELLED);
				}
			}
		}
	}


	/**
	 * PlayerMoveEvent handler<br>
	 * Cancels teleport if player moves during warmup
	 *
	 * @param event PlayerMoveEvent handled by this method
	 */
	@EventHandler
	final void onPlayerMovement(final PlayerMoveEvent event) {

		// if cancel-on-movement configuration is false, do nothing and return
		if (!plugin.getConfig().getBoolean("cancel-on-movement")) {
			return;
		}

		Player player = event.getPlayer();

		// if player is in warmup hashmap, cancel teleport and send player message
		if (plugin.teleportManager.isWarmingUp(player)) {

			// check for player movement other than head turning
			if (event.getFrom().distance(Objects.requireNonNull(event.getTo())) > 0) {

				// cancel player teleport
				plugin.teleportManager.cancelTeleport(player);

				// send player message
				Message.create(player, TELEPORT_CANCELLED_MOVEMENT).send();

				// play sound
				plugin.soundConfig.playSound(player, SoundId.TELEPORT_CANCELLED);
			}
		}
	}

}
