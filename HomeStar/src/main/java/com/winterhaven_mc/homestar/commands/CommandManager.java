package com.winterhaven_mc.homestar.commands;

import com.winterhaven_mc.homestar.PluginMain;
import com.winterhaven_mc.homestar.SimpleAPI;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;



/**
 * Implements command executor for <code>HomeStar</code> commands.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
public final class CommandManager implements CommandExecutor, TabCompleter {
	
	private final static ChatColor helpColor = ChatColor.YELLOW;
	private final static ChatColor usageColor = ChatColor.GOLD;

	// reference to main class
	private final PluginMain plugin;

	private final static List<String> subcommands = 
			Collections.unmodifiableList(new ArrayList<String>(
					Arrays.asList("give", "destroy", "status", "reload", "help")));

	/**
	 * Class constructor method for CommandManager
	 * 
	 * @param plugin reference to main class
	 */
	public CommandManager(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("homestar").setExecutor(this);
		
		// register this class as tab completer
		plugin.getCommand("homestar").setTabCompleter(this);
	}

	
	/**
	 * Tab completer for HomeStar
	 */
	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, 
			final String alias, final String[] args) {
		
		final List<String> returnList = new ArrayList<String>();
		
		// return list of valid matching subcommands
		if (args.length == 1) {
			
			for (String subcommand : subcommands) {
				if (sender.hasPermission("homestar." + subcommand) 
						&& subcommand.startsWith(args[0].toLowerCase())) {
					returnList.add(subcommand);
				}
			}
		}
		
		// return list of online players, or commands if subcommand is 'help'
		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("help")) {
				for (String subcommand : subcommands) {
					if (sender.hasPermission("homestar." + subcommand) 
							&& subcommand.startsWith(args[0].toLowerCase())) {
						returnList.add(subcommand);
					}
				}
			}
			else {
				@SuppressWarnings("deprecation")
				List<Player> matchedPlayers = plugin.getServer().matchPlayer(args[1]);
				for (Player player : matchedPlayers) {
					returnList.add(player.getName());
				}
			}
		}

		// return some useful quantities
		if (args.length == 3) {
			returnList.add("1");
			returnList.add("2");
			returnList.add("3");
			returnList.add("5");
			returnList.add("10");
		}
		
		return returnList;
	}


	/** command executor method for HomeStar
	 * 
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command cmd, 
			final String label, final String[] args) {
		
		String subcmd = "";
		
		// get subcommand
		if (args.length > 0) {
			subcmd = args[0];
		}
		// if no arguments, display usage for all commands
		else {
			displayUsage(sender,"all");
			return true;
		}
		
		// status command
		if (subcmd.equalsIgnoreCase("status")) {
			return statusCommand(sender,args);
		}

		// reload command
		if (subcmd.equalsIgnoreCase("reload")) {
			return reloadCommand(sender,args);
		}

		// give command
		if (subcmd.equalsIgnoreCase("give")) {
			return giveCommand(sender,args);
		}
		
		// destroy command
		if (subcmd.equalsIgnoreCase("destroy")) {
			return destroyCommand(sender,args);
		}
		
		// help command
		if (subcmd.equalsIgnoreCase("help")) {
			return helpCommand(sender,args);
		}
		
		plugin.messageManager.sendPlayerMessage(sender, "command-fail-invalid-command");
		plugin.messageManager.playerSound(sender, "command-fail");
		displayUsage(sender,"help");
		return true;
	}

	/**
	 * Display plugin settings
	 * @param sender
	 * @return boolean
	 */
	private boolean statusCommand(final CommandSender sender, final String args[]) {
		
		// if command sender does not have permission to view status, output error message and return true
		if (!sender.hasPermission("homestar.status")) {
			plugin.messageManager.sendPlayerMessage(sender, "permission-denied-status");
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}

		// output config settings
		String versionString = this.plugin.getDescription().getVersion();
		sender.sendMessage(ChatColor.DARK_AQUA + "[HomeStar] "
				+ ChatColor.AQUA + "Version: " + ChatColor.RESET + versionString);
		if (plugin.debug) {
			sender.sendMessage(ChatColor.DARK_RED + "DEBUG: true");
		}
		sender.sendMessage(ChatColor.GREEN + "Language: " 
				+ ChatColor.RESET + plugin.getConfig().getString("language"));
		sender.sendMessage(ChatColor.GREEN + "Default material: " 
				+ ChatColor.RESET + plugin.getConfig().getString("item-material"));
		sender.sendMessage(ChatColor.GREEN + "Minimum distance: " 
				+ ChatColor.RESET + plugin.getConfig().getInt("minimum-distance"));
		sender.sendMessage(ChatColor.GREEN + "Warmup: " 
				+ ChatColor.RESET + plugin.getConfig().getInt("teleport-warmup") + " seconds");
		sender.sendMessage(ChatColor.GREEN + "Cooldown: " 
				+ ChatColor.RESET + plugin.getConfig().getInt("teleport-cooldown") + " seconds");
		sender.sendMessage(ChatColor.GREEN + "Left-click allowed: " 
				+ ChatColor.RESET + plugin.getConfig().getBoolean("left-click"));
		sender.sendMessage(ChatColor.GREEN + "Shift-click required: " 
				+ ChatColor.RESET + plugin.getConfig().getBoolean("shift-click"));
		sender.sendMessage(ChatColor.GREEN 
				+ "Cancel on damage/movement/interaction: " + ChatColor.RESET + "[ "
				+ plugin.getConfig().getBoolean("cancel-on-damage") + "/"
				+ plugin.getConfig().getBoolean("cancel-on-movement") + "/"
				+ plugin.getConfig().getBoolean("cancel-on-interaction") + " ]");
		sender.sendMessage(ChatColor.GREEN + "Remove from inventory: " 
				+ ChatColor.RESET + plugin.getConfig().getString("remove-from-inventory"));
		sender.sendMessage(ChatColor.GREEN + "Allow in recipes: " 
				+ ChatColor.RESET + plugin.getConfig().getBoolean("allow-in-recipes"));
		sender.sendMessage(ChatColor.GREEN + "Lightning: " 
				+ ChatColor.RESET + plugin.getConfig().getBoolean("lightning"));
		sender.sendMessage(ChatColor.GREEN + "Enabled Words: " 
				+ ChatColor.RESET + plugin.worldManager.getEnabledWorldNames().toString());
		return true;
	}
	
		
	/**
	 * Reload plugin settings
	 * @param sender
	 * @param args
	 * @return boolean
	 */
	private boolean reloadCommand(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission to reload config, send error message and return true
		if (!sender.hasPermission("homestar.reload")) {
			plugin.messageManager.sendPlayerMessage(sender,"permission-denied-reload");
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}

		String subcmd = args[0];
		
		// argument limits
		int minArgs = 1;
		int maxArgs = 1;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"command-fail-args-count-under");
			plugin.messageManager.playerSound(sender, "command-fail");
			displayUsage(sender, subcmd);
			return true;
		}

		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"command-fail-args-count-over");
			plugin.messageManager.playerSound(sender, "command-fail");
			displayUsage(sender, subcmd);
			return true;
		}
		
		// reload main configuration
		plugin.reloadConfig();

		// update enabledWorlds list
		plugin.worldManager.reload();
		
		// reload messages
		plugin.messageManager.reload();

		// set debug field
		plugin.debug = plugin.getConfig().getBoolean("debug");
		
		// send reloaded message
		plugin.messageManager.sendPlayerMessage(sender,"command-success-reload");
		return true;
	}
	

	/**
	 * Give target player a homestar item
	 * @param sender
	 * @param args
	 * @return boolean
	 */
	final boolean giveCommand(final CommandSender sender, final String args[]) {
		
		// usage: /give <targetplayer> [qty]
			
		// if command sender does not have permission to give HomeStars, output error message and return true
		if (!sender.hasPermission("homestar.give")) {
			plugin.messageManager.sendPlayerMessage(sender, "permission-denied-give");
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}

		String subcmd = args[0];

		// argument limits
		int minArgs = 2;
		int maxArgs = 3;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"command-fail-args-count-under");
			plugin.messageManager.playerSound(sender, "command-fail");
			displayUsage(sender, subcmd);
			return true;
		}

		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"command-fail-args-count-over");
			plugin.messageManager.playerSound(sender, "command-fail");
			displayUsage(sender, subcmd);
			return true;
		}
		
		String targetPlayerName = "";
		int quantity = 1;

		if (args.length > 1) {
			targetPlayerName = args[1];
		}
		if (args.length > 2) {
			try {
				quantity = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-quantity-invalid");
				plugin.messageManager.playerSound(sender, "command-fail");
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

		// try to match target player name to currently online player
		Player targetPlayer = matchPlayer(sender, targetPlayerName);

		// if no match, do nothing and return (message was output by matchPlayer method)
		if (targetPlayer == null) {
			return true;
		}

		// add specified quantity of homestar(s) to player inventory
		HashMap<Integer,ItemStack> noFit = targetPlayer.getInventory().addItem(SimpleAPI.createItem(quantity));

		// count items that didn't fit in inventory
		int noFitCount = 0;
		for (int index : noFit.keySet()) {
			noFitCount += noFit.get(index).getAmount();
		}

		// if remaining items equals quantity given, send player-inventory-full message and return
		if (noFitCount == quantity) {
			plugin.messageManager.sendPlayerMessage(sender, "command-fail-give-inventory-full", quantity);
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}

		// subtract noFitCount from quantity
		quantity = quantity - noFitCount;

		// don't display messages if giving item to self
		if (!sender.getName().equals(targetPlayer.getName())) {
			
			// send message and play sound to giver
			plugin.messageManager.sendPlayerMessage(sender, "command-success-give",quantity);
			
			// if giver is in game, play sound
			if (sender instanceof Player) {
				plugin.messageManager.playerSound(sender, "command-success-give-sender");
			}
			
			// send message to target player
			CommandSender targetSender = (CommandSender) targetPlayer;
			plugin.messageManager.sendPlayerMessage(targetSender, "command-success-give-target",quantity);
		}
		// play sound to target player
		plugin.messageManager.playerSound(targetPlayer, "command-success-give-target");
		return true;
	}


	/**
	 * Destroy command
	 * @param sender
	 * @param args
	 * @return boolean
	 */
	final boolean destroyCommand(final CommandSender sender, final String args[]) {
		
		// sender must be in game player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender,"command-fail-destroy-console");
			return true;
		}
		
		// if command sender does not have permission to destroy HomeStars, output error message and return true
		if (!sender.hasPermission("homestar.destroy")) {
			plugin.messageManager.sendPlayerMessage(sender, "permission-denied-destroy");
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}

		Player player = (Player) sender;
		ItemStack playerItem = player.getInventory().getItemInHand();
			
		// check that player is holding a homestar stack
		if (!SimpleAPI.isHomeStar(playerItem)) {
			plugin.messageManager.sendPlayerMessage(sender, "command-fail-destroy-no-match");
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}
		int quantity = playerItem.getAmount();
		playerItem.setAmount(0);
		player.getInventory().setItemInHand(playerItem);
		plugin.messageManager.sendPlayerMessage(sender, "command-success-destroy", quantity);
		plugin.messageManager.playerSound(player,"command-success-destroy");
		return true;
	}

	
	/**
	 * Display command usage
	 * @param sender
	 * @param passedCommand
	 */
	final void displayUsage(final CommandSender sender, final String passedCommand) {
	
		String command = passedCommand;
		
		if (command.isEmpty() || command.equalsIgnoreCase("help")) {
			command = "all";
		}
		if ((command.equalsIgnoreCase("status")	
				|| command.equalsIgnoreCase("all"))
				&& sender.hasPermission("homestar.status")) {
			sender.sendMessage(usageColor + "/homestar status");
		}
		if ((command.equalsIgnoreCase("reload") 
				|| command.equalsIgnoreCase("all"))
				&& sender.hasPermission("homestar.reload")) {
			sender.sendMessage(usageColor + "/homestar reload");
		}
		if ((command.equalsIgnoreCase("give") 
				|| command.equalsIgnoreCase("all"))
				&& sender.hasPermission("homestar.give")) {
			sender.sendMessage(usageColor + "/homestar give <player> [quantity]");
		}
		if ((command.equalsIgnoreCase("destroy") 
				|| command.equalsIgnoreCase("all"))
				&& sender.hasPermission("deathspawn.delete")) {
			sender.sendMessage(usageColor + "/homestar destroy");
		}
		if ((command.equalsIgnoreCase("help") 
				|| command.equalsIgnoreCase("all"))
				&& sender.hasPermission("homestar.help")) {
			sender.sendMessage(usageColor + "/homestar help [command]");
		}
	}


	/**
	 * Display help message for commands
	 * @param sender
	 * @param args
	 * @return
	 */
	final boolean helpCommand(final CommandSender sender, final String args[]) {

		// if command sender does not have permission to display help, output error message and return true
		if (!sender.hasPermission("homestar.help")) {
			plugin.messageManager.sendPlayerMessage(sender, "permission-denied-help");
			plugin.messageManager.playerSound(sender, "command-fail");
			return true;
		}

		String command = "help";
		
		if (args.length > 1) {
			command = args[1]; 
		}
		
		String helpMessage = "That is not a valid command.";
		
		if (command.equalsIgnoreCase("status")) {
			helpMessage = "Displays current configuration settings.";
		}
		if (command.equalsIgnoreCase("reload")) {
			helpMessage = "Reloads the configuration without needing to restart the server.";
		}
		if (command.equalsIgnoreCase("give")) {
			helpMessage = "Gives a HomeStar to a player.";
		}
		if (command.equalsIgnoreCase("destroy")) {
			helpMessage = "Destroys the stack of HomeStars you are holding.";
		}
		if (command.equalsIgnoreCase("help")) {
			helpMessage = "Displays help for HomeStar commands.";
		}
		sender.sendMessage(helpColor + helpMessage);
		displayUsage(sender,command);
		return true;
	}


	@SuppressWarnings("deprecation")
	final Player matchPlayer(final CommandSender sender, final String targetPlayerName) {
		
		Player targetPlayer = null;

		// check exact match first
		targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
		
		// if no match, try substring match
		if (targetPlayer == null) {
			List<Player> playerList = plugin.getServer().matchPlayer(targetPlayerName);
			
			// if only one matching player, use it, otherwise send error message (no match or more than 1 match)
			if (playerList.size() == 1) {
				targetPlayer = playerList.get(0);
			}
		}

		// if match found, return target player object
		if (targetPlayer != null) {
			return targetPlayer;
		}
		
		// check if name matches known offline player
		HashSet<OfflinePlayer> matchedPlayers = new HashSet<OfflinePlayer>();
		for (OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
			if (targetPlayerName.equalsIgnoreCase(offlinePlayer.getName())) {
				matchedPlayers.add(offlinePlayer);
			}
		}
		if (matchedPlayers.isEmpty()) {
			plugin.messageManager.sendPlayerMessage(sender, "command-fail-player-not-found");
			plugin.messageManager.playerSound(sender, "command-fail");
			return null;
		}
		else {
			plugin.messageManager.sendPlayerMessage(sender, "command-fail-player-not-online");
			plugin.messageManager.playerSound(sender, "command-fail");
			return null;
		}
	}
	
}
