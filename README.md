### This repository has been moved to the WinterhavenMC organization.

https://github.com/winterhavenmc/HomeStar

# HomeStar

A configurable item that serves as a physical replacement for the /home command. Exactly like *SpawnStar*, except for home! In my survival world, I felt that the /home command was too easy. But my players really wanted to be able to return to home easily. So I made this plugin as a compromise. Now, players need to buy HomeStars from shops before venturing out into the wilderness if they want to be able to teleport back to their home (bed spawn) location.

# Features

*   A fully configurable item that returns a player to their home (bed spawn) when used. Default item is a netherstar, but can be changed to any item.
*   Customizable display name and lore.
*   Configurable cool down period.
*   Configurable warm up period, with optional particle effects during warm up.
*   Individually configurable options to cancel teleport during warmup on damage, movement, or block interaction.
*   Configurable option to remove item from inventory on use, after successful teleport, or never.
*   Configurable option to prevent using HomeStar items in crafting recipes.
*   Configurable option to require shift-click to use.
*   Configurable option to fallback to world spawn if user bed spawn is not set.
*   Uses MultiVerse world aliases in messages, if installed.
*   Uses MultiVerse world spawn location, if installed and spawn fallback is configured. (So players will be looking in the right direction on respawn.)
*   Configurable per message repeat delay (message cooldown) where appropriate.
*   Customizable language support.

# Commands

Command | Description
------- | -----------
`/homestar reload` | reloads the configuration without needing to restart the server.
`/homestar status` | displays configuration settings.
`/homestar give <playername> [quantity]` | allows admins or others with permission to give HomeStars directly to players.

# Permissions

Permission | Description | Default
---------- | ----------- | -------
`homestar.use` | gives a player the ability to use a HomeStar. | true
`homestar.admin` | gives a player access to the following admin commands: | op
`homestar.give` | allows players to give HomeStar items to other players. | op
`homestar.reload` | allows reloading of configuration files. | op
`homestar.status` | allows viewing configuration settings. | op
