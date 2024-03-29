/*
 * Copyright (c) 2022 Tim Savage.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.winterhavenmc.homestar.teleport;

import com.winterhavenmc.homestar.PluginMain;
import com.winterhavenmc.homestar.messages.Macro;
import com.winterhavenmc.homestar.messages.MessageId;
import com.winterhavenmc.homestar.sounds.SoundId;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * This class provides methods that are common to the concrete Teleporter classes.
 * It is not intended to be subclassed, except to provide these common methods to
 * the Teleporter classes within this package. The methods are declared final to
 * prevent them being overridden, and the class and methods are declared package-private
 * to prevent their use outside this package.
 */
abstract class AbstractTeleporter {

	final protected PluginMain plugin;

	public AbstractTeleporter(final PluginMain plugin) {
		this.plugin = plugin;
	}


	/**
	 * Get bedspawn destination for a player
	 *
	 * @param player the player
	 * @return the player bedspawn destination wrapped in an {@link Optional}
	 */
	final Optional<Location> getHomeDestination(final Player player) {

		// if player is null, return empty optional
		if (player == null) {
			return Optional.empty();
		}

		// get player bed spawn location
		Location location = player.getBedSpawnLocation();

		// if location is null, return empty optional
		if (location == null) {
			return Optional.empty();
		}

		// return optional wrapped destination for player bed spawn location
		return Optional.of(location);
	}


	/**
	 * Get spawn destination for a player
	 *
	 * @param player the player
	 * @return the player spawn destination wrapped in an {@link Optional}
	 */
	final Optional<Location> getSpawnDestination(final Player player) {

		// if player is null, return empty optional
		if (player == null) {
			return Optional.empty();
		}

		// get spawn location for player
		Location location = plugin.worldManager.getSpawnLocation(player);

		// if location is null, return empty optional
		if (location == null) {
			return Optional.empty();
		}

		// return destination for player spawn
		return Optional.of(location);
	}


	/**
	 * Send invalid destination message to player
	 *
	 * @param player the player
	 * @param destinationName the destination name
	 */
	void sendInvalidDestinationMessage(final Player player, final String destinationName) {
		plugin.messageBuilder.compose(player, MessageId.TELEPORT_FAIL_NO_BEDSPAWN)
				.setMacro(Macro.DESTINATION, destinationName)
				.send();
		plugin.soundConfig.playSound(player, SoundId.TELEPORT_CANCELLED);
	}


	/**
	 * Get overworld spawn location corresponding to a player nether or end world.
	 *
	 * @param player the passed player whose current world will be used to find a matching over world spawn location
	 * @return {@link Optional} wrapped spawn location of the normal world associated with the passed player
	 * nether or end world, or the current player world spawn location if no matching normal world found
	 */
	Optional<Location> getOverworldSpawnLocation(final Player player) {

		// check for null parameter
		if (player == null) {
			return Optional.empty();
		}

		// create list to store normal environment worlds
		List<World> normalWorlds = new ArrayList<>();

		// iterate through all server worlds
		for (World checkWorld : plugin.getServer().getWorlds()) {

			// if world is normal environment, try to match name to passed world
			if (checkWorld.getEnvironment().equals(World.Environment.NORMAL)) {

				// check if normal world matches passed world minus nether/end suffix
				if (checkWorld.getName().equals(player.getWorld().getName().replaceFirst("(_nether$|_the_end$)", ""))) {
					return Optional.of(plugin.worldManager.getSpawnLocation(checkWorld));
				}

				// if no match, add to list of normal worlds
				normalWorlds.add(checkWorld);
			}
		}

		// if only one normal world exists, return that world
		if (normalWorlds.size() == 1) {
			return Optional.of(plugin.worldManager.getSpawnLocation(normalWorlds.get(0)));
		}

		// if no matching normal world found and more than one normal world exists, return passed world spawn location
		return Optional.of(plugin.worldManager.getSpawnLocation(player.getWorld()));
	}


	/**
	 * Check if a player is in a nether world
	 *
	 * @param player the player
	 * @return true if player is in a nether world, false if not
	 */
	boolean isInNetherWorld(final Player player) {
		return player.getWorld().getEnvironment().equals(World.Environment.NETHER);
	}


	/**
	 * Check if a player is in an end world
	 *
	 * @param player the player
	 * @return true if player is in an end world, false if not
	 */
	boolean isInEndWorld(final Player player) {
		return player.getWorld().getEnvironment().equals(World.Environment.THE_END);
	}

}
