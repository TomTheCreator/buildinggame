package com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.CommandResult;
import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.ConsoleCommand;
import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.SubCommand;
import com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings.gui.FloorId;
import com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings.gui.FlySpeedId;
import com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings.gui.ParticleId;
import com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings.gui.RainId;
import com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings.gui.TimeId;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;

public class Gui extends ConsoleCommand {

	private final Collection<SubCommand> subCommands = new ArrayList<>();
	
	@Override
	public CommandResult onCommand(CommandSender sender, String[] args) {
		//add settings to list
		subCommands.add(new FloorId());
		subCommands.add(new FlySpeedId());
		subCommands.add(new ParticleId());
		subCommands.add(new RainId());
		subCommands.add(new TimeId());
		//test for the right setting
												
		if (args.length == 0) {
			for (SubCommand sc : subCommands) {
				if (sender.hasPermission(sc.getPermission()))
					MessageManager.getInstance().sendWithoutPrefix(sender, ChatColor.GREEN + "/bg setting gui " + sc.getName() + " - " + sc.getInfo());
			}
			return CommandResult.ARGUMENTEXCEPTION;
		}
												
		for (SubCommand subCommand : subCommands) {
			if (subCommand.getName().equalsIgnoreCase(args[0])) {
				if (sender.hasPermission(subCommand.getPermission())) {
					//remove first argument
														
					List<String> arguments = new ArrayList<>(Arrays.asList(args));
                    arguments.remove(0);
					args = arguments.toArray(new String[arguments.size()]);

					return subCommand.onCommand(sender, args);
				}
			}
		}
												
		MessageManager.getInstance().send(sender, ChatColor.RED + "That's not a setting");
		return CommandResult.ERROR;
	}

	@Override
	public String getName() {
		return "gui";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getInfo() {
		return "Change the gui";
	}

	@Override
	public String getPermission() {
		return "bg.setting.gui";
	}
}