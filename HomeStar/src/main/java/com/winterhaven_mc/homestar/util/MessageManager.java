package com.winterhaven_mc.homestar.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.winterhaven_mc.homestar.PluginMain;
import com.winterhaven_mc.util.ConfigAccessor;
import com.winterhaven_mc.util.StringUtil;


/**
 * Implements message manager for <code>HomeStar</code>.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
public final class MessageManager {

	// reference to main class
	private final PluginMain plugin;

	// hashmap for per player message cooldown
	private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> messageCooldownMap;

	// configuration file manager for messages
	private ConfigAccessor messages;

	// configuration file manager for sounds
	private ConfigAccessor sounds;

	// selected language
	private String language;

	
	/**
	 * Class constructor
	 * @param plugin
	 */
	public MessageManager(final PluginMain plugin) {

		// set reference to main class
		this.plugin = plugin;

		// install localization files
		this.installLocalizationFiles();

		// get configured language
		this.language = plugin.getConfig().getString("language");

		// check if localization file for configured language exists, if not then fallback to en-US
		if (!new File(plugin.getDataFolder() 
				+ File.separator + "language" 
				+ File.separator + this.language + ".yml").exists()) {
			plugin.getLogger().info("Language file for " + this.language + " not found. Defaulting to en-US.");
			this.language = "en-US";
		}

		// instantiate custom configuration manager for configured language file
		this.messages = new ConfigAccessor(plugin, "language" + File.separator + this.language + ".yml");

		// initialize messageCooldownMap
		this.messageCooldownMap = new ConcurrentHashMap<UUID,ConcurrentHashMap<String,Long>>();

		// default sound file name
		final String soundFileName = "sounds.yml";

		// old (pre-1.9) sound file name
		final String oldsoundFileName = "pre-1.9_sounds.yml";

		// instantiate custom sound manager
		this.sounds = new ConfigAccessor(plugin, soundFileName);

		// install sound file if not present
		this.sounds.saveDefaultConfig();

		// install alternate sound file if not present
		File oldSounds = new File(plugin.getDataFolder() + File.separator + oldsoundFileName);
		if (!oldSounds.exists()) {
			plugin.saveResource(oldsoundFileName, false);
		}
		
		// release file object
		oldSounds = null;
	}


	/**
	 *  Send message to player
	 * 
	 * @param sender			player receiving message
	 * @param messageId			message identifier in messages file
	 */
	public final void sendPlayerMessage(final CommandSender sender, final String messageId) {
		this.sendPlayerMessage(sender, messageId, 1, "", null);
	}

	
	/**
	 *  Send message to player
	 * 
	 * @param sender			player receiving message
	 * @param messageId			message identifier in messages file
	 */
	public final void sendPlayerMessage(final CommandSender sender, final String messageId, final String destinationName) {
		this.sendPlayerMessage(sender, messageId, 1, destinationName, null);
	}

	
	/**
	 * Send message to player
	 * 
	 * @param sender			player receiving message
	 * @param messageId			message identifier in messages file
	 * @param quantity			number of items
	 */
	public final void sendPlayerMessage(final CommandSender sender, final String messageId, final Integer quantity) {
		this.sendPlayerMessage(sender, messageId, quantity, "", null);
	}

	
	/**
	 * Send message to player
	 * 
	 * @param sender			player recieving message
	 * @param messageId			message identifier in messages file
	 */
	final void sendPlayerMessage(final CommandSender sender, final String messageId, final Player targetPlayer) {
		this.sendPlayerMessage(sender, messageId, 1, "", targetPlayer);
	}

	
	/**
	 * Send message to player
	 * 
	 * @param sender			player recieving message
	 * @param messageId			message identifier in messages file
	 */
	final void sendPlayerMessage(final CommandSender sender, final String messageId, 
			final Integer quantity, final Player targetPlayer) {
		this.sendPlayerMessage(sender, messageId, quantity, "", targetPlayer);
	}

	
	/** Send message to player
	 * 
	 * @param sender			Player receiving message
	 * @param messageId			message identifier in messages file
	 * @param quantity			number of items
	 * @param targetPlayer		player targeted
	 */	
	final void sendPlayerMessage(final CommandSender sender,
			final String messageId,
			final Integer quantity,
			final String destinationName,
			final Player targetPlayer) {

		// if message is not enabled in messages file, do nothing and return
		if (!messages.getConfig().getBoolean("messages." + messageId + ".enabled")) {
			return;
		}

		// set substitution variable defaults
		String playerName = "console";
		String targetPlayerName = "player";
		String worldName = "unknown";
		String cooldownString = "";
		String warmupString = "";

		if (targetPlayer != null) {
			targetPlayerName = targetPlayer.getName();
		}

		// if sender is a player...
		if (sender instanceof Player) {

			Player player = (Player) sender;

			// get message cooldown time remaining
			Long lastDisplayed = getMessageCooldown(player,messageId);

			// get message repeat delay
			int messageRepeatDelay = messages.getConfig().getInt("messages." + messageId + ".repeat-delay");

			// if message has repeat delay value and was displayed to player more recently, do nothing and return
			if (lastDisplayed > System.currentTimeMillis() - messageRepeatDelay * 1000) {
				return;
			}

			// if repeat delay value is greater than zero, add entry to messageCooldownMap
			if (messageRepeatDelay > 0) {
				putMessageCooldown(player,messageId);
			}

			// set player dependent variables
			playerName = player.getName();
			worldName = plugin.worldManager.getWorldName(player.getWorld());
			cooldownString = getTimeString(plugin.teleportManager.getCooldownTimeRemaining(player));
		}

		// get message from file
		String message = messages.getConfig().getString("messages." + messageId + ".string");

		// get item name and strip color codes
		String itemName = getItemName();

		// get warmup value from config file
		warmupString = getTimeString(plugin.getConfig().getInt("teleport-warmup"));

		// if quantity is greater than one, use plural item name
		if (quantity > 1) {
			// get plural item name
			itemName = getItemNamePlural();
		}

		// do variable substitutions
		if (message.contains("%")) {
			message = StringUtil.replace(message,"%itemname%",itemName);
			message = StringUtil.replace(message,"%playername%",playerName);
			message = StringUtil.replace(message,"%worldname%",worldName);
			message = StringUtil.replace(message,"%timeremaining%",cooldownString);
			message = StringUtil.replace(message,"%warmuptime%",warmupString);
			message = StringUtil.replace(message,"%quantity%",quantity.toString());
			message = StringUtil.replace(message,"%destination%",destinationName);
			message = StringUtil.replace(message,"%targetplayer%",targetPlayerName);

			// do variable substitutions, stripping color codes from all caps variables
			message = StringUtil.replace(message,"%ITEMNAME%", 
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',itemName)));
			message = StringUtil.replace(message,"%PLAYERNAME%", 
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',playerName)));
			message = StringUtil.replace(message,"%WORLDNAME%", 
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',worldName)));
			message = StringUtil.replace(message,"%TARGETPLAYER%", 
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',targetPlayerName)));
			message = StringUtil.replace(message,"%DESTINATION%", 
					ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',destinationName)));

			// no stripping of color codes necessary, but do variable substitutions anyhow
			// in case all caps variables were used
			message = StringUtil.replace(message,"%TIMEREMAINING%", cooldownString);
			message = StringUtil.replace(message,"%WARMUPTIME%", warmupString);
			message = StringUtil.replace(message,"%QUANTITY%", quantity.toString());
		}
		
		// send message to player
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
	}


	/**
	 * Play sound effect for action
	 * @param sender
	 * @param soundId
	 */
	public final void playerSound(final CommandSender sender, final String soundId) {

		if (sender instanceof Player) {
			playerSound((Player)sender,soundId);
		}
	}


	/**
	 * Play sound effect for action
	 * @param player
	 * @param soundId
	 */
	final void playerSound(final Player player, final String soundId) {

		// if sound effects are disabled in config, do nothing and return
		if (!plugin.getConfig().getBoolean("sound-effects")) {
			return;
		}

		// if sound is set to enabled in sounds file
		if (sounds.getConfig().getBoolean("sounds." + soundId + ".enabled")) {

			// get player only setting from config file
			boolean playerOnly = sounds.getConfig().getBoolean("sounds." + soundId + ".player-only");

			// get sound name from config file
			String soundName = sounds.getConfig().getString("sounds." + soundId + ".sound");

			// get sound volume from config file
			float volume = (float) sounds.getConfig().getDouble("sounds." + soundId + ".volume");

			// get sound pitch from config file
			float pitch = (float) sounds.getConfig().getDouble("sounds." + soundId + ".pitch");

			try {
				// if sound is set player only, use player.playSound()
				if (playerOnly) {
					player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
				}
				// else use world.playSound() so other players in vicinity can hear
				else {
					player.getWorld().playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
				}
			} catch (IllegalArgumentException e) {
				plugin.getLogger().warning("An error occured while trying to play the sound '" + soundName 
						+ "'. You probably need to update the sound name in your sounds.yml file.");
			}
		}
	}


	/**
	 * Add entry to message cooldown map
	 * @param player
	 * @param messageId
	 */
	private final void putMessageCooldown(final Player player, final String messageId) {

		final ConcurrentHashMap<String, Long> tempMap = new ConcurrentHashMap<String, Long>();
		tempMap.put(messageId, System.currentTimeMillis());
		this.messageCooldownMap.put(player.getUniqueId(), tempMap);
	}


	/**
	 * get entry from message cooldown map
	 * @param player
	 * @param messageId
	 * @return cooldown expire time
	 */
	private final long getMessageCooldown(final Player player, final String messageId) {

		// check if player is in message cooldown hashmap
		if (messageCooldownMap.containsKey(player.getUniqueId())) {

			// check if messageID is in player's cooldown hashmap
			if (messageCooldownMap.get(player.getUniqueId()).containsKey(messageId)) {

				// return cooldown time
				return messageCooldownMap.get(player.getUniqueId()).get(messageId);
			}
		}
		return 0L;
	}


	/**
	 * Remove player from message cooldown map
	 * @param player
	 */
	public final void removePlayerCooldown(final Player player) {
		messageCooldownMap.remove(player.getUniqueId());
	}


	/**
	 * Get current language
	 * @return
	 */
	public final String getLanguage() {
		return this.language;
	}


	/**
	 * Get item name from language file
	 * @return
	 */
	public final String getItemName() {
		String itemName = messages.getConfig().getString("item-name");
		return itemName;
	}


	/**
	 * Get configured plural item name from language file
	 * @return
	 */
	public final String getItemNamePlural() {
		final String itemNamePlural = messages.getConfig().getString("item-name-plural");
		return itemNamePlural;
	}


	/**
	 * Get configured item lore from language file
	 * @return
	 */
	public final List<String> getItemLore() {
		final List<String> itemLore = messages.getConfig().getStringList("item-lore");
		return itemLore;
	}


	/**
	 * Get spawn display name from language file
	 * @return
	 */
	public final String getSpawnDisplayName() {
		return messages.getConfig().getString("spawn-display-name");
	}


	/**
	 * Get home display name from language file
	 * @return
	 */
	public final String getHomeDisplayName() {
		return messages.getConfig().getString("home-display-name");
	}


	/**
	 * Reload messages
	 */
	public final void reload() {

		// reinstall message files if necessary
		installLocalizationFiles();

		// get currently configured language
		String newLanguage = languageFileExists(plugin.getConfig().getString("language"));

		// if configured language has changed, instantiate new messages object
		if (!newLanguage.equals(this.language)) {
			this.messages = new ConfigAccessor(plugin, "language" + File.separator + newLanguage + ".yml");
			this.language = newLanguage;
			plugin.getLogger().info("New language " + this.language + " enabled.");
		}

		// reload language file
		messages.reloadConfig();
	}


	/**
	 * Install localization files from <em>language</em> directory in jar 
	 */
	private final void installLocalizationFiles() {

		List<String> filelist = new ArrayList<String>();

		// get the absolute path to this plugin as URL
		final URL pluginURL = plugin.getServer().getPluginManager().getPlugin(plugin.getName()).getClass()
				.getProtectionDomain().getCodeSource().getLocation();

		// read files contained in jar, adding language/*.yml files to list
		ZipInputStream zip;
		try {
			zip = new ZipInputStream(pluginURL.openStream());
			while (true) {
				ZipEntry e = zip.getNextEntry();
				if (e == null) {
					break;
				}
				String name = e.getName();
				if (name.startsWith("language" + '/') && name.endsWith(".yml")) {
					filelist.add(name);
				}
			}
		} catch (IOException e1) {
			plugin.getLogger().warning("Could not read language files from jar.");
		}

		// iterate over list of language files and install from jar if not already present
		for (String filename : filelist) {
			// this check prevents a warning message when files are already installed
			if (new File(plugin.getDataFolder() + File.separator + filename).exists()) {
				continue;
			}
			plugin.saveResource(filename, false);
			plugin.getLogger().info("Installed localization file:  " + filename);
		}
	}


	/**
	 * Return language identifier if file exists, else return default en-US
	 * @param language
	 * @return
	 */
	private final String languageFileExists(final String language) {

		// check if localization file for configured language exists, if not then fallback to en-US
		File languageFile = new File(plugin.getDataFolder() 
				+ File.separator + "language" 
				+ File.separator + language + ".yml");

		if (languageFile.exists()) {
			return language;
		}
		plugin.getLogger().info("Language file " + language + ".yml does not exist. Defaulting to en-US.");
		return "en-US";
	}


	/**
	 * Format the time string with hours, minutes, seconds
	 * @return
	 */
	private final String getTimeString(long duration) {

		StringBuilder timeString = new StringBuilder();

		int hours =   (int)duration / 3600;
		int minutes = (int)(duration % 3600) / 60;
		int seconds = (int)duration % 60;

		String hour_string = this.messages.getConfig().getString("hour");
		String hour_plural_string = this.messages.getConfig().getString("hour_plural");
		String minute_string = this.messages.getConfig().getString("minute");
		String minute_plural_string = this.messages.getConfig().getString("minute_plural");
		String second_string = this.messages.getConfig().getString("second");
		String second_plural_string = this.messages.getConfig().getString("second_plural");

		if (hours > 1) {
			timeString.append(hours);
			timeString.append(' ');
			timeString.append(hour_plural_string);
			timeString.append(' ');
		}
		else if (hours == 1) {
			timeString.append(hours);
			timeString.append(' ');
			timeString.append(hour_string);
			timeString.append(' ');
		}

		if (minutes > 1) {
			timeString.append(minutes);
			timeString.append(' ');
			timeString.append(minute_plural_string);
			timeString.append(' ');
		}
		else if (minutes == 1) {
			timeString.append(minutes);
			timeString.append(' ');
			timeString.append(minute_string);
			timeString.append(' ');
		}

		if (seconds > 1) {
			timeString.append(seconds);
			timeString.append(' ');
			timeString.append(second_plural_string);
		}
		else if (seconds == 1) {
			timeString.append(seconds);
			timeString.append(' ');
			timeString.append(second_string);
		}

		return timeString.toString().trim();
	}

}
