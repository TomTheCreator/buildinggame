package com.gmail.stefvanschiedev.buildinggame.commands.subcommands.settings.subjects;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.CommandResult;
import com.gmail.stefvanschiedev.buildinggame.commands.commandutils.ConsoleCommand;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;

public class Remove extends ConsoleCommand {

	@Override
	public CommandResult onCommand(CommandSender sender, String[] args) {
		YamlConfiguration config = SettingsManager.getInstance().getConfig();
		
		if (args.length == 0) {
			MessageManager.getInstance().send(sender, ChatColor.RED + "Please specify the subject");
			return CommandResult.ARGUMENTEXCEPTION;
		}
		
		StringBuilder subject = new StringBuilder();
		
		for (String arg : args)
			subject.append(arg).append(' ');
		
		List<String> subjects = config.getStringList("subjects");
		subjects.remove(subject.toString());
		
		config.set("subjects", subjects);
		SettingsManager.getInstance().save();
		
		MessageManager.getInstance().send(sender, ChatColor.GREEN + "Removed subject " + subject);
		
		return CommandResult.SUCCES;
	}

	@Override
	public String getName() {
		return "remove";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getInfo() {
		return "Remove a subject";
	}

	@Override
	public String getPermission() {
		return "bg.setting.subjects.remove";
	}

}
