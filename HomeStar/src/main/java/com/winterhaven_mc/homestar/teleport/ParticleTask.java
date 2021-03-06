package com.winterhaven_mc.homestar.teleport;

import com.winterhaven_mc.homestar.PluginMain;

import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;


/**
 * A self-cancelling, repeating task that generates ender signal particles
 * at a player's location as long as they are in the warmup hashmap
 */
final class ParticleTask extends BukkitRunnable {

	private final PluginMain plugin;
	private final Player player;


	/**
	 * Class constructor method
	 *
	 * @param player the player to emit particles
	 */
	ParticleTask(final PluginMain plugin, final Player player) {

		// check for null parameters
		this.plugin = Objects.requireNonNull(plugin);
		this.player = Objects.requireNonNull(player);
	}


	@Override
	public final void run() {

		// if player is in the warmup hashmap, display the particle effect at their location
		if (plugin.teleportManager.isWarmingUp(player)) {
			player.getWorld().playEffect(player.getLocation().add(0.0d, 1.0d, 0.0d), Effect.ENDER_SIGNAL, 0, 10);
		}
		// otherwise cancel this repeating task if the player is not in the warmup hashmap
		else {
			this.cancel();
		}
	}

}
