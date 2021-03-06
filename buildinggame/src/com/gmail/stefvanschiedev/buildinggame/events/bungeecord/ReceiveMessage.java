package com.gmail.stefvanschiedev.buildinggame.events.bungeecord;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.stefvanschiedev.buildinggame.Main;
import com.gmail.stefvanschiedev.buildinggame.managers.arenas.ArenaManager;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;

import fr.rhaz.socket4mc.Bukkit.BukkitSocketJSONEvent;

public class ReceiveMessage implements Listener {

	@EventHandler
	public static void onBukkitSocketJSON(BukkitSocketJSONEvent e) {
		if (!e.getChannel().equals("BuildingGame"))
			return;
		
		//encode data
		String rawData = e.getData();
		
		if (rawData.startsWith("write:")) {
			//someone has executed a command
			rawData = rawData.replace("write:", "").trim();
			
			String[] data = rawData.split(", ");
			YamlConfiguration file;

			switch (data[0]) {
				case "arenas.yml":
					file = SettingsManager.getInstance().getArenas();
					break;
				case "config.yml":
					file = SettingsManager.getInstance().getConfig();
					break;
				case "messages.yml":
					file = SettingsManager.getInstance().getMessages();
					break;
				case "signs.yml":
					file = SettingsManager.getInstance().getSigns();
					break;
				default:
					return;
			}
			
			file.set(data[1], data[2].startsWith("(int)") ? Integer.parseInt(data[2].replace("(int)", "").trim()) : data[2]);
		} else if (rawData.startsWith("save"))
			SettingsManager.getInstance().save();
		else if (rawData.startsWith("join:")) {
			rawData = rawData.replace("join:", "");
			
			String[] data = rawData.split(", ");
			
			final Player player = Bukkit.getPlayer(data[0].trim());
			final Arena arena = ArenaManager.getInstance().getArena(data[1].trim());
			
			if (player == null) {
				System.out.println("Couldn't find player");
				return;
			} else if (arena == null) {
				System.out.println("Couldn't find arena");
				return;
			}
			
			BukkitRunnable task = new BukkitRunnable() {
				@Override
				public void run() {
					arena.join(player);
				}
			};
			
			task.runTask(Main.getInstance());
		}
		
		System.out.println("Received command");
	}
}