package com.winterhaven_mc.homestar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Implements command executor for <code>HomeStar</code> commands.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
public class CommandManager implements CommandExecutor {

	private final PluginMain plugin; // reference to main class
	private ArrayList<String> enabledWorlds;

	/**
	 * constructor method for <code>CommandManager</code> class
	 * 
	 * @param plugin reference to main class
	 */
	CommandManager(PluginMain plugin) {
		this.plugin = plugin;
		plugin.getCommand("homestar").setExecutor(this);
		updateEnabledWorlds();
	}


	/** command executor method for HomeStar
	 * 
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		int maxArgs = 3;
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"command-args-count-over");
			if (sender.hasPermission("homestar.status")) {
				sender.sendMessage(ChatColor.AQUA + "/homestar status");
			}
			if (sender.hasPermission("homestar.reload")) {
				sender.sendMessage(ChatColor.AQUA + "/homestar reload");
			}
			if (sender.hasPermission("homestar.destroy")) {
				sender.sendMessage(ChatColor.AQUA + "/homestar destroy");
			}
			if (sender.hasPermission("homestar.give")) {
				sender.sendMessage(ChatColor.AQUA + "/homestar give <player> [amount]");
			}
			return true;
		}
		
		/*
		 * status command
		 */
		if (args.length < 1 || args[0].equalsIgnoreCase("status")) {
			
			// if command sender does not have permission to view status, output error message and return true
			if (!sender.hasPermission("homestar.status")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-status");
				return true;
			}
			
			// output config settings
			String versionString = this.plugin.getDescription().getVersion();
			sender.sendMessage(ChatColor.DARK_AQUA + "[HomeStar] " + ChatColor.AQUA + "Version: " + ChatColor.RESET + versionString);
			if (plugin.debug) {
				sender.sendMessage(ChatColor.DARK_RED + "DEBUG: true");
			}
			sender.sendMessage(ChatColor.GREEN + "Language: " + ChatColor.RESET + plugin.getConfig().getString("language"));
			sender.sendMessage(ChatColor.GREEN + "Default material: " + ChatColor.RESET + plugin.getConfig().getString("item-material"));
			sender.sendMessage(ChatColor.GREEN + "Minimum distance: " + ChatColor.RESET + plugin.getConfig().getInt("minimum-distance"));
			sender.sendMessage(ChatColor.GREEN + "Warmup: " + ChatColor.RESET + plugin.getConfig().getInt("teleport-warmup") + " seconds");
			sender.sendMessage(ChatColor.GREEN + "Cooldown: " + ChatColor.RESET + plugin.getConfig().getInt("teleport-cooldown") + " seconds");
			sender.sendMessage(ChatColor.GREEN + "Shift-click required: " + ChatColor.RESET + plugin.getConfig().getBoolean("shift-click"));
			sender.sendMessage(ChatColor.GREEN + "Cancel on damage/movement/interaction: " + ChatColor.RESET + "[ "
					+ plugin.getConfig().getBoolean("cancel-on-damage") + "/"
					+ plugin.getConfig().getBoolean("cancel-on-movement") + "/"
					+ plugin.getConfig().getBoolean("cancel-on-interaction") + " ]");
			sender.sendMessage(ChatColor.GREEN + "Remove from inventory: " + ChatColor.RESET + plugin.getConfig().getString("remove-from-inventory"));
			sender.sendMessage(ChatColor.GREEN + "Allow in recipes: " + ChatColor.RESET + plugin.getConfig().getBoolean("allow-in-recipes"));
			sender.sendMessage(ChatColor.GREEN + "Lightning: " + ChatColor.RESET + plugin.getConfig().getBoolean("lightning"));
			sender.sendMessage(ChatColor.GREEN + "Enabled Words: " + ChatColor.RESET + getEnabledWorlds().toString());
			return true;
		}
		String subcmd = args[0];

		/*
		 *  reload command
		 */
		if (cmd.getName().equalsIgnoreCase("homestar") &&
				subcmd.equalsIgnoreCase("reload")) {
			
			// if sender does not have permission to reload config, send error message and return true
			if (!sender.hasPermission("homestar.reload")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-reload");
				return true;
			}

			// reload main configuration
			plugin.reloadConfig();

			// update enabledWorlds field
			updateEnabledWorlds();
			
			// reload messages
			plugin.messageManager.reload();
			
			// send reloaded message to command sender
			plugin.messageManager.sendPlayerMessage(sender, "command-success-reload");
			return true;
		}

		/*
		 *  give command
		 */
		if (cmd.getName().equalsIgnoreCase("homestar") && subcmd.equalsIgnoreCase("give")) {
			
			// if command sender does not have permission to give HomeStars, output error message and return true
			if (!sender.hasPermission("homestar.give")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-give");
				return true;
			}

			// if too few arguments, send error usage message
			if (args.length < 2) {
				plugin.messageManager.sendPlayerMessage(sender, "command-args-count-under");
				sender.sendMessage(ChatColor.AQUA + "/homestar give <player> [quantity]");
				return true;
			}
			
			// if too many arguments, send error and usage message
			if (args.length > 3) {
				plugin.messageManager.sendPlayerMessage(sender, "command-args-count-over");
				sender.sendMessage(ChatColor.AQUA + "/homestar give <player> [quantity]");
				return true;				
			}
			
			Player player;
			String playerstring = "";
			int quantity = 1;

			if (args.length > 1) {
				playerstring = args[1];
			}
			if (args.length > 2) {
				try {
					quantity = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					plugin.messageManager.sendPlayerMessage(sender, "command-fail-quantity-invalid");
					return true;
				}
			}
			
			// validate quantity (min = 1, max = configured maximum, or runtime Integer.MAX_VALUE)
			quantity = Math.max(1, quantity);
			int maxQuantity = plugin.getConfig().getInt("max-give-amount");
			if (maxQuantity < 0) {
				maxQuantity = Integer.MAX_VALUE;
			}
			quantity = Math.min(maxQuantity, quantity);

			// check all known players for a match
			OfflinePlayer[] offlinePlayers = plugin.getServer().getOfflinePlayers();
			for (OfflinePlayer offlinePlayer : offlinePlayers) {
				if (playerstring.equalsIgnoreCase(offlinePlayer.getName())) {
					if (!offlinePlayer.isOnline()) {
						plugin.messageManager.sendPlayerMessage(sender, "command-fail-player-not-online");
						return true;
					}
				}
			}
			
			// try to match a player from given string
			List<Player> playerList = plugin.getServer().matchPlayer(playerstring);
			
			// if only one matching player, use it, otherwise send error message (no match or more than 1 match)
			if (playerList.size() == 1) {
				player = playerList.get(0);
			}
			else {
				// if unique matching player is not found, send player-not-found message to sender
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-player-not-found");
				return true;
			}

			// add specified quantity of homestar(s) to player inventory
			HashMap<Integer,ItemStack> noFit = player.getInventory().addItem(plugin.utilities.createItem(quantity));
			
			// count items that didn't fit in inventory
			int noFitCount = 0;
			for (int index : noFit.keySet()) {
				noFitCount += noFit.get(index).getAmount();
			}
			
			// if remaining items equals quantity given, send player-inventory-full message and return
			if (noFitCount == quantity) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-give-inventory-full", quantity);
				return true;
			}
			
			// subtract noFitCount from quantity
			quantity = quantity - noFitCount;
			
			// send message to player
			plugin.messageManager.sendPlayerMessage(sender, "command-success-give", quantity);
			return true;
		}
		
		/*
		 * destroy command
		 */
		if (cmd.getName().equalsIgnoreCase("homestar") && subcmd.equalsIgnoreCase("destroy")) {
			
			if (!(sender instanceof Player)) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-destroy-console");
				return true;
			}
			
			if (!sender.hasPermission("homestar.destroy")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-destroy");
				return true;
			}
			
			Player player = (Player) sender;
			ItemStack playerItem = player.getItemInHand();
			
			// check that player is holding a homestar stack
			if (!plugin.utilities.isHomeStar(playerItem)) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-destroy-no-match");
				return true;
			}
			int quantity = playerItem.getAmount();
			playerItem.setAmount(0);
			player.setItemInHand(playerItem);
			plugin.messageManager.sendPlayerMessage(sender, "command-success-destroy",quantity);			
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * update enabledWorlds ArrayList field from config file settings
	 */
	void updateEnabledWorlds() {
		
		// copy list of enabled worlds from config into enabledWorlds ArrayList field
		this.enabledWorlds = new ArrayList<String>(plugin.getConfig().getStringList("enabled-worlds"));
		
		// if enabledWorlds ArrayList is empty, add all worlds to ArrayList
		if (this.enabledWorlds.isEmpty()) {
			for (World world : plugin.getServer().getWorlds()) {
				enabledWorlds.add(world.getName());
			}
		}
		
		// remove each disabled world from enabled worlds field
		for (String disabledWorld : plugin.getConfig().getStringList("disabled-worlds")) {
			this.enabledWorlds.remove(disabledWorld);
		}
	}
	
	
	/**
	 * get list of enabled worlds
	 * @return ArrayList of String enabledWorlds
	 */
	ArrayList<String> getEnabledWorlds() {
		return this.enabledWorlds;
	}

}
