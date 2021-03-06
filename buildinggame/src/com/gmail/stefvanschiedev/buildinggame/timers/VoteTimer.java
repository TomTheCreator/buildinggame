package com.gmail.stefvanschiedev.buildinggame.timers;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.gmail.stefvanschiedev.buildinggame.Main;
import com.gmail.stefvanschiedev.buildinggame.api.Win;
import com.gmail.stefvanschiedev.buildinggame.api.events.PlayerWinEvent;
import com.gmail.stefvanschiedev.buildinggame.managers.commands.CommandExecuter;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;
import com.gmail.stefvanschiedev.buildinggame.managers.softdependencies.SDVault;
import com.gmail.stefvanschiedev.buildinggame.timers.utils.Timer;
import com.gmail.stefvanschiedev.buildinggame.utils.GameState;
import com.gmail.stefvanschiedev.buildinggame.utils.ItemBuilder;
import com.gmail.stefvanschiedev.buildinggame.utils.Vote;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayer;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayerType;
import com.gmail.stefvanschiedev.buildinggame.utils.plot.Plot;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * This handles the voting time for this arena
 *
 * @since 2.1.0
 */
public class VoteTimer extends Timer {

    /**
     * Whether this timer is active or not
     */
	private boolean running;

	/**
     * The amount of seconds left for this plot
     */
	private int seconds;

	/**
     * The original amount of seconds per plot
     */
	private int originalSeconds;

	/**
     * The arena this timer belongs to
     */
	private final Arena arena;

	/**
     * The plot which is currently being voted on
     */
	private Plot plot;

	/**
     * The config.yml YAML configuration
     */
	private final YamlConfiguration config = SettingsManager.getInstance().getConfig();

	/**
     * The messages.yml YAML configuration
     */
	private final YamlConfiguration messages = SettingsManager.getInstance().getMessages();

	/**
     * Constructs a new VoteTimer with the given amount of seconds
     *
     * @param seconds the amount of time per plot
     * @param arena the arena this timer belongs to
     */
	public VoteTimer(int seconds, Arena arena) {
		this.seconds = seconds;
		originalSeconds = seconds;
		this.arena = arena;
	}

	/**
     * Called whenever a second has passed. This will generate a new plot if needed or end the game if all plots have
     * been voted on.
     *
     * @since 2.1.0
     */
	@Override
	public void run() {
		running = true;
		
		if (seconds == originalSeconds) {
			plot = getNextPlot();
			if (plot == null) {
				arena.setState(GameState.RESETING);
				
				Plot first = null;
				Plot second = null;
				Plot third = null;
				
				for (Plot plot : arena.getPlots()) {
					if (first == null || first.getPoints() < plot.getPoints()) {
						third = second;
						second = first;
						first = plot;
						continue;
					}
					if (second == null || second.getPoints() < plot.getPoints()) {
						third = second;
						second = plot;
						continue;
					}
					if (third == null || third.getPoints() < plot.getPoints())
						third = plot;
				}
				
				//call events
				if (first != null)
					Bukkit.getPluginManager().callEvent(new PlayerWinEvent(arena, first.getGamePlayers(), Win.FIRST));
				if (second != null)
					Bukkit.getPluginManager().callEvent(new PlayerWinEvent(arena, second.getGamePlayers(), Win.SECOND));
				if (third != null)
					Bukkit.getPluginManager().callEvent(new PlayerWinEvent(arena, third.getGamePlayers(), Win.THIRD));
				
				arena.setFirstPlot(first);
				arena.setSecondPlot(second);
				arena.setThirdPlot(third);
				
				for (Plot plot : arena.getUsedPlots()) {
					for (GamePlayer gamePlayer : plot.getAllGamePlayers()) {
						Player player = gamePlayer.getPlayer();
						
						player.getInventory().clear();

						if (first != null)
							player.teleport(first.getLocation());
						
						ItemBuilder.check(player);
						
						MessageManager.getInstance().send(player, messages.getStringList("game-ends.message"));
						if (second != null && third != null) {
							for (String message : messages.getStringList("game-ends.winners")) {
								MessageManager.getInstance().send(gamePlayer.getPlayer(), message
										.replace("%first_players%", first.getPlayerFormat())
										.replace("%second_players%", second.getPlayerFormat())
										.replace("%third_players%", third.getPlayerFormat()));
							}
						}

                        //noinspection ConstantConditions
                        gamePlayer.addTitleAndSubtitle(messages.getString("winner.title")
								.replace("%first%", first.getPlayerFormat())
								.replace("%second%", second == null ? "" : second.getPlayerFormat())
								.replace("%third%", third == null ? "" : third.getPlayerFormat())
								.replace("%first_points%", first.getPoints() + "")
								.replace("%second_points%", second == null ? "0" : second.getPoints() + "")
								.replace("%third_points%", third == null ? "0" : third.getPoints() + ""),
								messages.getString("winner.subtitle")
								.replace("%first%", first.getPlayerFormat())
								.replace("%second%", second == null ? "" : second.getPlayerFormat())
								.replace("%third%", third == null ? "" : third.getPlayerFormat())
								.replace("%first_points%", first.getPoints() + "")
								.replace("%second_points%", second == null ? "0" : second.getPoints() + "")
								.replace("%third_points%", third == null ? "0" : second.getPoints() + ""));
					
						if(SDVault.getInstance().isEnabled() && gamePlayer.getGamePlayerType() == GamePlayerType.PLAYER) {
							Economy vault = SDVault.getEconomy();
							if (first.equals(plot)) {
								double money = config.getInt("money.first");
								
								if (player.hasPermission("bg.rewards.money.double")) {
									money *= 2;
								}
								
								EconomyResponse r = vault.depositPlayer(player, money);
					
								for (String message : messages.getStringList("winner.first")) {
									MessageManager.getInstance().send(player, message
											.replace("%points%", plot.getPoints() + ""));
								}
						
								for (String command : config.getStringList("commands.first")) {
									String cmd = CommandExecuter.execute(command
											.replace("%player%", player.getName()));
									if (cmd != null)
										player.performCommand(command
												.replace("%player%", player.getName()));
								}
						
								if (r.transactionSuccess()) {
									for (String message : messages.getStringList("vault.message")) {
										MessageManager.getInstance().send(player, message
												.replace("%money%", money + ""));
									}
								}
							} else if (second.equals(plot)) {
								double money = config.getInt("money.second");
								
								if (player.hasPermission("bg.rewards.money.double")) {
									money *= 2;
								}
								
								EconomyResponse r = vault.depositPlayer(player, money);
								
								for (String message : messages.getStringList("winner.second")) {
									MessageManager.getInstance().send(player, message
											.replace("%points%", plot.getPoints() + ""));
								}
						
								for (String command : config.getStringList("commands.second")) {
									String cmd = CommandExecuter.execute(command
											.replace("%player%", player.getName()));
									if (cmd != null)
										player.performCommand(command
												.replace("%player%", player.getName()));
								}
						
								if (r.transactionSuccess()) {
									for (String message : messages.getStringList("vault.message")) {
										MessageManager.getInstance().send(player, message
												.replace("%money%", money + ""));
									}
								}
							} else if (third.equals(plot)) {
								double money = config.getInt("money.third");
								
								if (player.hasPermission("bg.rewards.money.double")) {
									money *= 2;
								}
								
								EconomyResponse r = vault.depositPlayer(player, money);
					
								for (String message : messages.getStringList("winner.third")) {
									MessageManager.getInstance().send(player, message
											.replace("%points%", plot.getPoints() + ""));
								}
						
								for (String command : config.getStringList("commands.third")) {
									String cmd = CommandExecuter.execute(command
											.replace("%player%", player.getName()));
									if (cmd != null)
										player.performCommand(command
												.replace("%player%", player.getName()));
								}
						
								if (r.transactionSuccess()) {
									for (String message : messages.getStringList("vault.message")) {
										MessageManager.getInstance().send(player, message
												.replace("%money%", money + ""));
									}
								}
							} else {
								double money = config.getInt("money.others");
								
								if (player.hasPermission("bg.rewards.money.double")) {
									money *= 2;
								}
								
								EconomyResponse r = vault.depositPlayer(player, money);
							
								for (String command : config.getStringList("commands.others")) {
									String cmd = CommandExecuter.execute(command
											.replace("%player%", player.getName()));
									if (cmd != null)
										player.performCommand(command
												.replace("%player%", player.getName()));
								}
						
								if (r.transactionSuccess()) {
									for (String message : messages.getStringList("vault.message")) {
										MessageManager.getInstance().send(player, message
												.replace("%money%", money + ""));
									}
								}
							}
						}
					}
				}
				if (first != null && !first.getGamePlayers().isEmpty()) {
					for (GamePlayer gamePlayer : first.getGamePlayers()) {
						for (String command : config.getStringList("win-commands")) {
							String cmd = CommandExecuter.execute(command
									.replace("%winner%", gamePlayer.getPlayer().getName()));
							if (cmd != null)
								gamePlayer.getPlayer().performCommand(command
										.replace("%winner%", gamePlayer.getPlayer().getName()).trim());
						}
					}
				}
				arena.getWinTimer().runTaskTimer(Main.getInstance(), 20L, 20L);
				running = false;
				this.cancel();
				return;
			}

			arena.setVotingPlot(plot);
			for (Plot plot : arena.getUsedPlots()) {
				for (GamePlayer gamePlayer : plot.getAllGamePlayers()) {
					Player player = gamePlayer.getPlayer();
					
					if (!config.getBoolean("names-after-voting") && config.getBoolean("scoreboards.vote.enable"))
						arena.getVoteScoreboard().show(player);
					
					player.setPlayerTime(this.plot.getTime().decode(), false);
					player.setPlayerWeather(this.plot.isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR);
				}
			}
		}
		//timings
		try {
			for (String key : config.getConfigurationSection("timings.vote-timer.at").getKeys(false)) {
                if (seconds == Integer.parseInt(key)) {
                    for (String command : config.getStringList("timings.vote-timer.at." + Integer.parseInt(key)))
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%arena%", arena.getName()));
                }
			}
			for (String key : config.getConfigurationSection("timings.vote-timer.every").getKeys(false)) {
                if (seconds % Integer.parseInt(key) == 0) {
                    for (String command : config.getStringList("timings.vote-timer.every." + Integer.parseInt(key)))
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%arena%", arena.getName()));
                }
			}
		} catch (NullPointerException | NumberFormatException e) {}
		
		seconds--;
		if (seconds <= 0) {
				for (Plot plot : arena.getUsedPlots()) {
					for (GamePlayer player : plot.getGamePlayers()) {
						if (config.getBoolean("names-after-voting")) {
							for (String message : messages.getStringList("voting.message")) {
								MessageManager.getInstance().send(player.getPlayer(), message
										.replace("%playerplot%", this.plot.getPlayerFormat()));
							}
							player.addTitleAndSubtitle(messages.getString("voting.title")
									.replace("%playerplot%", this.plot.getPlayerFormat()), messages.getString("voting.subtitle")
									.replace("%playerplot%", this.plot.getPlayerFormat()));
						}
						if (!this.plot.hasVoted(player.getPlayer())) {
							this.plot.addVote(new Vote(config.getInt("voting.default-vote-points"), player.getPlayer()));
						}
					}
				}
			seconds = config.getInt("votetimer");
			originalSeconds = seconds;
		}
	}

	/**
     * Returns a plot which we haven't voted for yet
     *
     * @return the next unvoted plot
     * @since 2.1.0
     */
	@Nullable
    @Contract(pure = true)
    private Plot getNextPlot() {
		for (Plot plot : arena.getPlots()) {
			if (!arena.getVotedPlots().contains(plot)) {
				if (!plot.getGamePlayers().isEmpty()) {
					return plot;
				}
			}
		}
		return null;
	}

	/**
     * Returns the amount of seconds left for this plot
     *
     * @return the amount of seconds left
     * @since 2.1.0
     */
    @Contract(pure = true)
	@Override
	public int getSeconds() {
		return seconds;
	}

	/**
     * Returns whether this timer is running or not
     *
     * @return true if this timer is running, false otherwise
     * @since 2.1.0
     */
	@Contract(pure = true)
	public boolean isActive() {
		return running;
	}
}