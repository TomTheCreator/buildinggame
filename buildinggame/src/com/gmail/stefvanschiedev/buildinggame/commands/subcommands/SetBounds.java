package com.gmail.stefvanschiedev.buildinggame.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.CommandResult;
import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.PlayerCommand;
import com.gmail.stefvanschiedev.buildinggame.managers.arenas.ArenaManager;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;
import com.gmail.stefvanschiedev.buildinggame.managers.plots.BoundaryManager;
import com.gmail.stefvanschiedev.buildinggame.utils.ItemBuilder;
import com.gmail.stefvanschiedev.buildinggame.utils.ItemBuilder.ClickEvent;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;
import com.gmail.stefvanschiedev.buildinggame.utils.plot.Plot;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a command to set the boundaries of a plot
 *
 * @since 2.1.0
 */
public class SetBounds extends PlayerCommand {

    /**
     * The method which is called once the correct command is entered
     *
     * @param player the player who entered the command
     * @param args the arguments provided
     * @return the commandresult this execution yielded
     * @see CommandResult
     * @since 2.1.0
     */
	@NotNull
    @Override
	public CommandResult onCommand(Player player, String[] args) {
		if (args.length < 2) {
			MessageManager.getInstance().send(player, ChatColor.RED + "Please specify the arena and plot");
			return CommandResult.ARGUMENTEXCEPTION;
		}
		
		final Arena arena = ArenaManager.getInstance().getArena(args[0]);
		
		if (arena == null) {
			MessageManager.getInstance().send(player, ChatColor.RED + "That's not a valid arena");
			return CommandResult.ERROR;
		}

		int id;

		try {
			id = Integer.parseInt(args[1]);
		} catch (NumberFormatException nfe) {
			MessageManager.getInstance().send(player, ChatColor.RED + "That's not a valid plot");
			return CommandResult.ERROR;
		}
		
		final Plot plot = arena.getPlot(id);
		
		if (plot == null) {
			MessageManager.getInstance().send(player, ChatColor.RED + "That's not a valid plot");
			return CommandResult.ERROR;
		}

        ItemBuilder itemBuilder = new ItemBuilder(player, Material.STICK).setDisplayName(ChatColor.LIGHT_PURPLE + "Wand").setClickEvent(new ClickEvent() {
            private Location previousLocation;

            @Override
            public boolean onClick(PlayerInteractEvent e) {
                YamlConfiguration arenas = SettingsManager.getInstance().getArenas();
                YamlConfiguration messages = SettingsManager.getInstance().getMessages();

                Player player = e.getPlayer();
                Action action = e.getAction();

                if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK)
                    return false;

                if (previousLocation == null) {
                    previousLocation = e.getClickedBlock().getLocation();

                    MessageManager.getInstance().send(player, ChatColor.GREEN + "Now click on the other corner");
                    return true;
                } else {
                    //second time
                    Location location = e.getClickedBlock().getLocation();

                    if (previousLocation.getWorld().equals(location.getWorld())) {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.world", location.getWorld().getName());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.world", previousLocation.getWorld().getName());
                    } else {
                        MessageManager.getInstance().send(player, ChatColor.RED + "The world has to be the same");
                        return true;
                    }

                    //x
                    if (previousLocation.getBlockX() < location.getBlockX()) {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.x", location.getBlockX());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.x", previousLocation.getBlockX());
                    } else {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.x", location.getBlockX());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.x", previousLocation.getBlockX());
                    }

                    //y
                    if (previousLocation.getBlockY() < location.getBlockY()) {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.y", location.getBlockY());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.y", previousLocation.getBlockY());
                    } else {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.y", location.getBlockY());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.y", previousLocation.getBlockY());
                    }

                    //z
                    if (previousLocation.getBlockZ() < location.getBlockZ()) {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.z", location.getBlockZ());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.z", previousLocation.getBlockZ());
                    } else {
                        arenas.set(arena.getName() + '.' + plot.getID() + ".low.z", location.getBlockZ());
                        arenas.set(arena.getName() + '.' + plot.getID() + ".high.z", previousLocation.getBlockZ());
                    }

                    SettingsManager.getInstance().save();
                    BoundaryManager.getInstance().setup();

                    for (String message : messages.getStringList("setBounds.succes"))
                        MessageManager.getInstance().send(player, message
                                .replace("%place%", plot.getID() + "")
                                .replace("%arena%", arena.getName()));

                    previousLocation = null;

                    player.getInventory().setItemInMainHand(null);
                    ItemBuilder.check(player);

                    return true;
                }
            }
        });
		ItemBuilder.register(itemBuilder);
        player.getInventory().setItemInMainHand(itemBuilder);
		
		MessageManager.getInstance().send(player, ChatColor.GREEN + "Please click on one corner");
		
		return CommandResult.SUCCES;
	}

    /**
     * Returns the name of this subcommand
     *
     * @return the name of this subcommand
     * @since 2.1.0
     */
	@NotNull
    @Contract(pure = true)
    @Override
	public String getName() {
		return "setbounds";
	}

    /**
     * Returns the aliases for this sbucommand
     *
     * @return an array of aliases for this subcommand
     * @since 2.1.0
     */
	@Nullable
    @Contract(pure = true)
	@Override
	public String[] getAliases() {
		return null;
	}

    /**
     * Returns an informational message about this subcommand
     *
     * @return an informational message
     * @since 2.1.0
     */
    @Nls
	@NotNull
    @Contract(pure = true)
    @Override
	public String getInfo() {
		return "Set the boundaries of a plot (inclusive)";
	}

    /**
     * Returns the permission node required for this subcommand
     *
     * @return the permission node for this subcommand
     * @since 2.1.0
     */
	@NotNull
    @Contract(pure = true)
    @Override
	public String getPermission() {
		return "bg.setbounds";
	}
}