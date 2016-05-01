package com.winterhaven_mc.homestar;

import org.bukkit.Location;
import org.bukkit.World;
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
import org.bukkit.scheduler.BukkitTask;


/**
 * Implements player event listener for <code>HomeStar</code> events.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
class PlayerEventListener implements Listener {

	// reference to main class
	private final PluginMain plugin;
	
	
	/**
	 * constructor method for <code>PlayerEventListener</code> class
	 * 
	 * @param	plugin		A reference to this plugin's main class
	 */
	PlayerEventListener(final PluginMain plugin) {
		
		// reference to main
		this.plugin = plugin;
		
		// register events in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
	}

	
	/**
	 * Player interact event listener
	 */
	@SuppressWarnings("deprecation")
	@EventHandler
	void onPlayerUse(final PlayerInteractEvent event) {

		// get player
		final Player player = event.getPlayer();
		
		// if cancel-on-interaction is configured true, check if player is in warmup hashmap
		if (plugin.getConfig().getBoolean("cancel-on-interaction")) {
			
			// if player is in warmup hashmap, check if they are interacting with a block (not air)
			if (plugin.warmupManager.isWarmingUp(player)) {

				// if player is interacting with a block, cancel teleport, output message and return
				if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {			
					plugin.warmupManager.cancelTeleport(player);
					plugin.messageManager.sendPlayerMessage(player, "teleport-cancelled-interaction");

					// play sound effects if enabled
					plugin.messageManager.playerSound(player, "teleport-fail");
					return;
				}
			}
		}
		
		// get players item in hand
		ItemStack playerItem = player.getItemInHand();

		// if item used is not a homestar, do nothing and return
		if (!plugin.utilities.isHomeStar(playerItem)) {
			return;
		}
		
		// if event action is not a right click, or not a left click if configured, do nothing and return
		if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) 
				|| event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
				|| (plugin.getConfig().getBoolean("left-click") 
				&& !(event.getAction().equals(Action.LEFT_CLICK_AIR)
				|| event.getAction().equals(Action.LEFT_CLICK_BLOCK)))) {
				return;
		}
		
		// cancel event
		event.setCancelled(true);
		player.updateInventory();
		
		// if players current world is not enabled in config, do nothing and return
		if (!playerWorldEnabled(player)) {
			return;
		}
		
		// if player does not have homestar.use permission, send message and return
		if (!player.hasPermission("homestar.use")) {
			plugin.messageManager.sendPlayerMessage(player, "permission-denied-use");
			plugin.messageManager.playerSound(player, "teleport-denied-permission");
			return;
		}
		
		// if shift-click is configured true and player is not sneaking, send message and return
		if (plugin.getConfig().getBoolean("shift-click") && !event.getPlayer().isSneaking()) {
			plugin.messageManager.sendPlayerMessage(player, "usage-shift-click");
			return;
		}
		
		// if player cooldown has not expired, send player cooldown message and return
		if (plugin.cooldownManager.getTimeRemaining(player) > 0) {
			plugin.messageManager.sendPlayerMessage(player, "teleport-cooldown");
			return;
		}
		
		// if player is warming up, do nothing and return
		if (plugin.warmupManager.isWarmingUp(player)) {
			return;
		}
		
		// get player world
		World playerWorld = player.getWorld();
		
		// get home display name from language file
		String destinationName = plugin.messageManager.getHomeDisplayName();
		
		// if player has bukkit bedspawn, try to get safe bedspawn location
		Location location = player.getBedSpawnLocation();
		
		// if center-on-block is configured true, get block centered location
		if (plugin.getConfig().getBoolean("center-on-block")) {
			location = plugin.utilities.getBlockCenteredLocation(location);
		}
		
		// if bedspawn location is null, check if bedspawn-fallback is configured true
		if (location == null) {
			
			// send missing or obstructed message
			plugin.messageManager.sendPlayerMessage(player, "teleport-fail-no-bedspawn");
			
			// if bedspawn-fallback is configured false, play teleport fail sound and return
			if (!plugin.getConfig().getBoolean("bedspawn-fallback")) {
				plugin.messageManager.playerSound(player, "teleport-fail");
				return;
			}
			// else set destination to spawn location
			else {
				
				// set destinationName string to spawn name from language file
				destinationName = plugin.messageManager.getSpawnDisplayName();
				
				// if multiverse is enabled, get spawn location from it so we have pitch and yaw
				if (plugin.mvEnabled) {
					location = plugin.mvCore.getMVWorldManager().getMVWorld(playerWorld).getSpawnLocation();
				}
				// else get bukkit spawn location and player's pitch and yaw
				else {
					location = playerWorld.getSpawnLocation();
					location.setPitch(player.getLocation().getPitch());
					location.setYaw(player.getLocation().getYaw());
				}
			}		
		}
		
		// if player is less than config min-distance from destination, send player min-distance message and return
		if (player.getWorld() == location.getWorld() && location.distance(player.getLocation()) < plugin.getConfig().getInt("minimum-distance")) {
			plugin.messageManager.sendPlayerMessage(player, "teleport-min-distance", destinationName);
			return;
		}
		
		// if remove-from-inventory is configured on-use, take one spawn star item from inventory now
		if (plugin.getConfig().getString("remove-from-inventory").equalsIgnoreCase("on-use")) {
			ItemStack removeItem = playerItem;
			removeItem.setAmount(playerItem.getAmount() - 1);
			player.getInventory().setItemInHand(removeItem);
		}
		
		// if warmup setting is greater than zero, send warmup message
		if (plugin.getConfig().getInt("teleport-warmup") > 0) {
			plugin.messageManager.sendPlayerMessage(player, "teleport-warmup",destinationName);
			
			// if enabled, play sound effect
			plugin.messageManager.playerSound(player, "teleport-warmup");
		}
		
		// initiate delayed teleport for player to destination
		BukkitTask teleportTask = 
				new DelayedTeleportTask(player,
						location,
						destinationName,
						playerItem.clone()).runTaskLater(plugin, plugin.getConfig().getInt("teleport-warmup") * 20);
		
		// insert player and taskId into warmup hashmap
		plugin.warmupManager.putPlayer(player, teleportTask.getTaskId());
		
		// if log-use is enabled in config, write log entry
		if (plugin.getConfig().getBoolean("log-use")) {
			
			// construct log message
			String configItemName = plugin.messageManager.getItemName();
			String log_message = player.getName() + " just used a " + configItemName + " in " + player.getWorld().getName() + ".";
			
			// strip color codes from log message
			log_message = log_message.replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
			
			// write message to log
			plugin.getLogger().info(log_message);
		}
		
	}
	
	
	@EventHandler
	void onPlayerDeath(final PlayerDeathEvent event) {
		
		Player player = (Player)event.getEntity();
		
		// cancel any pending teleport for player
		plugin.warmupManager.removePlayer(player);
		
	}

	
	@EventHandler
	void onPlayerQuit(final PlayerQuitEvent event) {
		
		Player player = event.getPlayer();
		
		// cancel any pending teleport for player
		plugin.warmupManager.removePlayer(player);
		
		// remove player from message cooldown map
		plugin.messageManager.removePlayerCooldown(player);
		
	}
	
	
	/**
	 * Prevent homestar items from being used in crafting recipes if configured
	 * @param event
	 */
	@EventHandler
	void onCraftPrepare(final PrepareItemCraftEvent event) {

		// if allow-in-recipes is true in configuration, do nothing and return
		if (plugin.getConfig().getBoolean("allow-in-recipes")) {
			return;
		}

		// if crafting inventory contains homestar item, set result item to null
		for(ItemStack itemStack : event.getInventory()) {
			if (plugin.utilities.isHomeStar(itemStack)) {
				event.getInventory().setResult(null);
			}
		}
	}
	
	
	/**
	 * Event listener for EntityDamageByEntity event<br>
	 * Cancels pending teleport if player takes damage
	 * @param event
	 */
	@EventHandler
	void onEntityDamage(final EntityDamageEvent event) {
		
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
				if (plugin.warmupManager.isWarmingUp((Player) entity)) {
					plugin.warmupManager.cancelTeleport((Player) entity);
					plugin.messageManager.sendPlayerMessage((Player) entity, "teleport-cancelled-damage");
					plugin.messageManager.playerSound((Player) entity, "teleport-cancelled");
				}				
			}
		}
	}
	
	
	@EventHandler
	void onPlayerMovement(final PlayerMoveEvent event) {
				
		// if cancel-on-movement configuration is false, do nothing and return
		if (!plugin.getConfig().getBoolean("cancel-on-movement")) {
			return;
		}
			
		Player player = event.getPlayer();

		// if player is in warmup hashmap, cancel teleport and send player message
		if (plugin.warmupManager.isWarmingUp(player)) {

			// check for player movement other than head turning
			if (event.getFrom().distance(event.getTo()) > 0) {
				plugin.warmupManager.cancelTeleport(player);
				plugin.messageManager.sendPlayerMessage(player,"teleport-cancelled-movement");
				plugin.messageManager.playerSound(player, "teleport-cancelled");
			}
		}
	}

	
	/**
	 * Test if player world is enabled in config
	 * @param player
	 * @return
	 */
	private boolean playerWorldEnabled(final Player player) {
		
		// if player world is in list of enabled worlds, return true
		if (plugin.commandManager.getEnabledWorlds().contains(player.getWorld().getName())) {
			return true;
		}
		
		// otherwise return false
		return false;
	}

}
