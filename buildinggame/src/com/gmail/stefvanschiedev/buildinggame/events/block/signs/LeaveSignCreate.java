package com.gmail.stefvanschiedev.buildinggame.events.block.signs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import com.gmail.stefvanschiedev.buildinggame.managers.arenas.SignManager;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;

/**
 * Handles players creating leave signs
 *
 * @since 3.1.0
 */
public class LeaveSignCreate implements Listener {

    /**
     * Handles players breaking join signs
     *
     * @param e an event indicating that a sign's text has changed
     * @see SignChangeEvent
     * @since 3.1.0
     */
	@EventHandler
	public void onSignChange(SignChangeEvent e) {
		YamlConfiguration messages = SettingsManager.getInstance().getMessages();
		YamlConfiguration signs = SettingsManager.getInstance().getSigns();
		
		Player player = e.getPlayer();
		
		if (!e.getLine(0).equalsIgnoreCase("[buildinggame]"))
			return;
			
		if (!e.getLine(1).equalsIgnoreCase("leave"))
			return;
			
		if (!player.hasPermission("bg.sign.create")) {
			MessageManager.getInstance().send(player, messages.getString("global.permissionNode"));
			return;
		}
		
		String line1 = messages.getString("signs.leave.line-1");
		String line2 = messages.getString("signs.leave.line-2");
		String line3 = messages.getString("signs.leave.line-3");
		String line4 = messages.getString("signs.leave.line-4");
		
		e.setLine(0, MessageManager.translate(line1));
		e.setLine(1, MessageManager.translate(line2));
		e.setLine(2, MessageManager.translate(line3));
		e.setLine(3, MessageManager.translate(line4));
		
		int number = 0;
		
		for (String string : signs.getKeys(false)) {
			try {
				number = Integer.parseInt(string);
			} catch (NumberFormatException nfe) {}
		}
		
		number++;
		
		signs.set(number + ".type", "leave");
		signs.set(number + ".world", e.getBlock().getLocation().getWorld().getName());
		signs.set(number + ".x", e.getBlock().getLocation().getBlockX());
		signs.set(number + ".y", e.getBlock().getLocation().getBlockY());
		signs.set(number + ".z", e.getBlock().getLocation().getBlockZ());
		SettingsManager.getInstance().save();
		
		SignManager.getInstance().setup();
	}
}