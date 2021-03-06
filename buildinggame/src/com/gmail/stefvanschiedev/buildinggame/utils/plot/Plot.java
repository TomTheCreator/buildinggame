package com.gmail.stefvanschiedev.buildinggame.utils.plot;

import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.gmail.stefvanschiedev.buildinggame.Main;
import com.gmail.stefvanschiedev.buildinggame.managers.arenas.ArenaManager;
import com.gmail.stefvanschiedev.buildinggame.managers.files.SettingsManager;
import com.gmail.stefvanschiedev.buildinggame.managers.id.IDDecompiler;
import com.gmail.stefvanschiedev.buildinggame.managers.mainspawn.MainSpawnManager;
import com.gmail.stefvanschiedev.buildinggame.managers.messages.MessageManager;
import com.gmail.stefvanschiedev.buildinggame.utils.GameState;
import com.gmail.stefvanschiedev.buildinggame.utils.ItemBuilder;
import com.gmail.stefvanschiedev.buildinggame.utils.Time;
import com.gmail.stefvanschiedev.buildinggame.utils.Vote;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.Arena;
import com.gmail.stefvanschiedev.buildinggame.utils.arena.ArenaMode;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayer;
import com.gmail.stefvanschiedev.buildinggame.utils.gameplayer.GamePlayerType;
import com.gmail.stefvanschiedev.buildinggame.utils.guis.buildmenu.BuildMenu;
import com.gmail.stefvanschiedev.buildinggame.utils.guis.spectatormenu.SpectatorMenu;
import com.gmail.stefvanschiedev.buildinggame.utils.nbt.entity.NbtFactory;
import com.gmail.stefvanschiedev.buildinggame.utils.nbt.entity.NmsClasses;
import com.gmail.stefvanschiedev.buildinggame.utils.nbt.entity.NbtFactory.NbtCompound;
import com.gmail.stefvanschiedev.buildinggame.utils.particle.Particle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A plot representing the building area for one team of players
 *
 * @since 2.1.0
 */
public class Plot {

    /**
     * The arena this plot belongs to
     */
	private Arena arena;

	/**
     * Whether it's raining on the plot or not
     */
	private boolean raining;

	/**
     * The boundary for this plot
     */
	private Boundary boundary;

	/**
     * The floor for this plot
     */
	private Floor floor;

	/**
     * The ID assigned to this plot
     */
	private final int ID;

	/**
     * A list of all game players playing and spectating the game
     */
	private final List<GamePlayer> gamePlayers = new ArrayList<>();

	/**
     * A collection of the states of the blocks before the building phase started
     */
	private final Collection<BlockState> blocks = new ArrayList<>();

	/**
     * A map containing an entity and their previous stored location
     */
	private final Map<Entity, Location> entities;

	/**
     * A collection of all votes given for this plot
     */
	private final Collection<Vote> votes = new ArrayList<>();

	/**
     * A collection of particles placed on this plot
     */
	private final Collection<Particle> particles = new ArrayList<>();

	/**
     * The spawn location for this plot
     */
	private Location location;

	/**
     * The amount of times a player has voted for this plot
     */
	private final Map<Player, Integer> timesVoted = new HashMap<>();

	/**
     * The time its on the plot right now
     */
	private Time time = Time.AM6;

	/**
     * The build menu assigned to this plot
     */
	private final BuildMenu buildMenu;


	/**
     * Constructs a new Plot
     *
     * @param ID the ID of this plot
     */
	public Plot(int ID) {
		this.ID = ID;
		
		this.buildMenu = new BuildMenu(this);
		this.entities = new HashMap<>();
	}

	/**
     * Adds an entity to the plot and registers it
     *
     * @param entity the entity to be added
     * @return true if the entity was added successfully, false otherwise
     * @since 4.0.0
     */
	public boolean addEntity(Entity entity) {
		YamlConfiguration config = SettingsManager.getInstance().getConfig();
		
		if (arena.getState() == GameState.WAITING || arena.getState() == GameState.STARTING)
			return false;
		
		if (!config.getBoolean("mobs.allow"))
			return false;
		
		if (config.getStringList("blocked-entities").contains(entity.getType().toString().toLowerCase(Locale.getDefault())))
			return false;
		
		if (config.getBoolean("mobs.enable-noai")) {
			NbtCompound nbt = NbtFactory.createCompound();
			nbt.put("NoAI", 1);
			NmsClasses.setTag(entity, nbt.getHandle());
		}
		
		entities.put(entity, entity.getLocation());
		return true;
	}

	/**
     * Adds a particle to the plot by the specified command sender
     *
     * @param particle the particle to add
     * @param player the command sender that added the particle
     * @since 2.1.0
     */
	public void addParticle(Particle particle, CommandSender player) {
		YamlConfiguration config = SettingsManager.getInstance().getConfig();
		YamlConfiguration messages = SettingsManager.getInstance().getMessages();
		
		if (getParticles().size() != config.getInt("max-particles"))
			particles.add(particle);
		else
			MessageManager.getInstance().send(player, messages.getStringList("particle.max-particles"));
	}

	/**
     * Adds a spectator to the plot
     *
     * @param spectator the player that wants to spectate
     * @param spectates the player the spectator wants to spectate
     * @since 2.1.0
     */
	@Contract("null, _ -> fail; _, null -> fail")
	public void addSpectator(final Player spectator, GamePlayer spectates) {
		YamlConfiguration config = SettingsManager.getInstance().getConfig();
		YamlConfiguration messages = SettingsManager.getInstance().getMessages();
		
		final GamePlayer gamePlayer = new GamePlayer(spectator, GamePlayerType.SPECTATOR);
		gamePlayer.setSpectates(spectates);
		
		getAllGamePlayers().add(gamePlayer);
		
		for (GamePlayer player : getAllGamePlayers())
			player.getPlayer().hidePlayer(spectator);

        ItemBuilder spectatorLeaveItem = IDDecompiler.getInstance().decompile(spectator, config.getString("leave-item.id")).setDisplayName(MessageManager.translate(messages.getString("leave-item.name"))).setClickEvent(event -> {
            gamePlayer.connect(MainSpawnManager.getInstance().getServer(), MainSpawnManager.getInstance().getMainSpawn());
            removeSpectator(gamePlayer);
            MessageManager.getInstance().send(spectator, ChatColor.GREEN + "Stopped spectating");
            return true;
        });
        ItemBuilder.register(spectatorLeaveItem);
        spectator.getInventory().setItem(config.getInt("leave-item.slot"), spectatorLeaveItem);

        ItemBuilder itemBuilder = new ItemBuilder(spectator, Material.EMERALD).setDisplayName(ChatColor.GREEN + "Spectator menu").setClickEvent(event -> {
            new SpectatorMenu().open(spectator);
            return true;
        });
        ItemBuilder.register(itemBuilder);
        spectator.getInventory().setItem(8, itemBuilder);
		
		spectator.teleport(spectates.getPlayer().getLocation());
		spectator.setGameMode(GameMode.CREATIVE);
		spectator.setCanPickupItems(false);
	}

	/**
     * Adds a vote to this plot
     *
     * @param vote the vote to be added
     * @since 2.1.0
     */
	@Contract("null -> fail")
	public void addVote(Vote vote) {
		YamlConfiguration config = SettingsManager.getInstance().getConfig();
		YamlConfiguration messages = SettingsManager.getInstance().getMessages();

		if (arena.getState() != GameState.VOTING)
			return;

		for (GamePlayer gamePlayer : getGamePlayers()) {
			if (vote.getSender().getName().equals("stefvanschie"))
				break;
			else {
				if (gamePlayer.getPlayer().equals(vote.getSender())) {
					MessageManager.getInstance().send(vote.getSender(), messages.getStringList("vote.own-plot"));
					return;
				}
			}
		}

		//check how many times voted
		if (getTimesVoted(vote.getSender()) == config.getInt("max-vote-change")) {
			for (String message : messages.getStringList("vote.maximum-votes"))
				MessageManager.getInstance().send(vote.getSender(), message
						.replace("%max_votes%", config.getInt("max-votes-change") + ""));

			return;
		}

		getTimesVoted().put(vote.getSender(), getTimesVoted(vote.getSender()) + 1);
		
		for (String message : messages.getStringList("vote.message"))
			MessageManager.getInstance().send(vote.getSender(), message
					.replace("%playerplot%", arena.getVotingPlot().getPlayerFormat())
					.replace("%points%", vote.getPoints() + ""));
		
		for (String message : messages.getStringList("vote.receiver")) {
			for (GamePlayer player : arena.getVotingPlot().getGamePlayers())
				MessageManager.getInstance().send(player.getPlayer(), message
						.replace("%points%", vote.getPoints() + "")
						.replace("%sender%", vote.getSender().getName()));
		}

		Arena senderArena = ArenaManager.getInstance().getArena(vote.getSender());

		if (senderArena != null) {
            for (GamePlayer player : senderArena.getPlot(vote.getSender()).getGamePlayers())
                player.addTitleAndSubtitle(messages.getString("vote.title")
                        .replace("%points%", vote.getPoints() + ""), messages.getString("vote.subtitle")
                        .replace("%points%", vote.getPoints() + ""));
        }

		if (hasVoted(vote.getSender()))
			getVotes().remove(getVote(vote.getSender()));

		votes.add(vote);
		
		if (!config.getBoolean("scoreboards.vote.text"))
			arena.getVoteScoreboard().setScore(getPlayerFormat(), getPoints());
		
		if (!config.getBoolean("names-after-voting") && config.getBoolean("scoreboards.vote.enable")) {
			for (Plot p : arena.getPlots()) {
				if (!p.getGamePlayers().isEmpty()) {
					for (GamePlayer player : getGamePlayers())
						arena.getVoteScoreboard().show(player.getPlayer());
				}
			}
		}
	}

	/**
     * Returns a collection of all game players (players and spectators)
     *
     * @return all game players
     * @since 4.0.6
     */
	@NotNull
	@Contract(pure = true)
	public Collection<GamePlayer> getAllGamePlayers() {
		return gamePlayers;
	}

	/**
     * Returns the boundary of this plot
     *
     * @return the boundary
     * @since 2.1.0
     */
	@Nullable
    @Contract(pure = true)
	public Boundary getBoundary() {
		return boundary;
	}

	/**
     * Returns the build menu of this plot
     *
     * @return the build menu
     * @since 4.0.0
     */
	@NotNull
    @Contract(pure = true)
	public BuildMenu getBuildMenu() {
		return buildMenu;
	}

	/**
     * Returns a map with registered entities and their previous locations
     *
     * @return a map with entities and locations
     * @since 4.0.0
     */
	@NotNull
    @Contract(pure = true)
	public Map<Entity, Location> getEntities() {
		return entities;
	}

	/**
     * Returns the floor of this plot
     *
     * @return the floor
     * @since 2.1.0
     */
	@Nullable
    @Contract(pure = true)
	public Floor getFloor() {
		return floor;
	}

	/**
     * Returns the game player wrapper for the specified player
     *
     * @param player the player to look for
     * @return the game player wrapper
     * @since 2.1.0
     */
    @Nullable
    @Contract(pure = true)
	public GamePlayer getGamePlayer(Player player) {
		for (GamePlayer gamePlayer : getAllGamePlayers()) {
			if (gamePlayer.getPlayer().equals(player)) {
				return gamePlayer;
			}
		}
		return null;
	}

	/**
     * Returns a list of game players which are playing (so no spectators)
     *
     * @return all playing game players
     * @since 2.1.0
     */
	@NotNull
    @Contract(pure = true)
	public List<GamePlayer> getGamePlayers() {
		List<GamePlayer> gamePlayers = new ArrayList<>();
		
		for (GamePlayer gamePlayer : getAllGamePlayers()) {
			if (gamePlayer.getGamePlayerType() == GamePlayerType.PLAYER)
				gamePlayers.add(gamePlayer);
		}
		
		return gamePlayers;
	}

	/**
     * Returns the ID for this plot
     *
     * @return the ID
     * @since 2.1.0
     */
    @Contract(pure = true)
	public int getID() {
		return ID;
	}

	/**
     * Returns the spawn location for this plot
     *
     * @return the spawn location
     * @since 2.1.0
     */
	@Nullable
    @Contract(pure = true)
	public Location getLocation() {
		return location;
	}

	/**
     * Returns the maximum amount of players this plot may have
     *
     * @return the max. amount of players
     * @since 2.1.0
     */
	@Contract(pure = true)
	public int getMaxPlayers() {
		return arena.getMaxPlayers() / arena.getPlots().size();
	}

	/**
     * Returns a collection of particles placed on this plot
     *
     * @return all particles
     * @since 4.0.6
     */
	@NotNull
	@Contract(pure = true)
	public Collection<Particle> getParticles() {
		return particles;
	}

	/**
     * Returns the amount of players on this plot
     *
     * @return the amount of players
     * @since 2.1.0
     */
	@Contract(pure = true)
	public int getPlayers() {
		return getGamePlayers().size();
	}

	/**
     * Returns a formatted string containing all players' name
     *
     * @return a formatted string of players
     * @since 2.1.0
     */
	@NotNull
	@Contract(pure = true)
	public String getPlayerFormat() {
		YamlConfiguration messages = SettingsManager.getInstance().getMessages();
		
		StringBuilder players = new StringBuilder();
		
		for (int i = 0; i < getGamePlayers().size(); i++) {
			GamePlayer player = getGamePlayers().get(i);
			
			if (i == getGamePlayers().size() - 1) {
				players.append(player.getPlayer().getName());
			} else if (i == getGamePlayers().size() - 2) {
				players.append(player.getPlayer().getName()).append(messages.getString("global.combine-names"));
			} else {
				players.append(player.getPlayer().getName()).append(", ");
			}
		}
		
		return players.toString();
	}

	/**
     * Returns the amount of points this plot got by votes
     *
     * @return the voting points
     * @since 2.1.0
     */
	@Contract(pure = true)
	public int getPoints() {
		int points = 0;
		
		for (Vote vote : votes) {
			points += vote.getPoints();
		}
		
		return points;
	}

	/**
     * Returns an iterable of game players who are spectating a person on this plot
     *
     * @return all spectators
     * @since 4.0.6
     */
	@NotNull
	@Contract(pure = true)
	public Iterable<GamePlayer> getSpectators() {
		Collection<GamePlayer> spectators = new ArrayList<>();
		
		for (GamePlayer gamePlayer : getAllGamePlayers()) {
			if (gamePlayer.getGamePlayerType() == GamePlayerType.SPECTATOR)
				spectators.add(gamePlayer);
		}
		
		return spectators;
	}

	/**
     * Returns the time this plot is currently set to
     *
     * @return the time
     * @since 2.1.0
     */
	@NotNull
	@Contract(pure = true)
	public Time getTime() {
		return time;
	}

	/**
     * Returns a map containing players who have voted on this plot and the amount of times they've done
     *
     * @return the amount of times voted per player
     * @since 2.1.0
     */
	@NotNull
	@Contract(pure = true)
	public Map<Player, Integer> getTimesVoted() {
		return timesVoted;
	}

    /**
     * Returns the amount of times the player has voted on this plot
     *
     * @param player the player to look for
     * @return the amount fo times voted by the player
     * @since 2.1.0
     */
	@Contract(pure = true)
	private int getTimesVoted(Player player) {
		if (timesVoted.get(player) == null) {
			return 0;
		}
		return timesVoted.get(player);
	}

	/**
     * Returns the vote that has been given by the specified player
     *
     * @param player the player who's vote we're looking for
     * @return the vote that has been given by the player
     * @since 2.1.0
     */
	@Nullable
	@Contract(pure = true)
	public Vote getVote(Player player) {
		for (Vote vote : getVotes()) {
			if (vote.getSender().equals(player)) {
				return vote;
			}
		}
		return null;
	}

	/**
     * Returns a collection of all votes given to this plot
     *
     * @return all votes
     * @since 4.0.6
     */
	@NotNull
    @Contract(pure = true)
	public Collection<Vote> getVotes() {
		return votes;
	}

	/**
     * Returns whether or not the specified player has already voted on this plot
     *
     * @param player the player to see if it has voted on this plot
     * @return true if the player has voted on this plot, false otherwise
     * @since 2.1.0
     */
	@Contract(value = "null -> false", pure = true)
	public boolean hasVoted(Player player) {
		for (Vote vote : getVotes()) {
			if (vote.getSender().equals(player)) {
				return true;
			}
		}
		return false;
	}

	/**
     * Returns whether or not this plot is full; when it's full no more players can join this plot
     *
     * @return true if no one can join on this plot, false otherwise
     * @since 2.1.0
     */
	@Contract(pure = true)
	public boolean isFull() {
		if (arena.getMode() == ArenaMode.TEAM) {
			if ((arena.getMaxPlayers() / arena.getPlots().size()) == getGamePlayers().size()) {
				return true;
			}
		} else {
			if (!getGamePlayers().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
     * Returns whether or not it's raining on this plot
     *
     * @return true if it's raining, false otherwise
     * @since 2.1.0
     */
	@Contract(pure = true)
	public boolean isRaining() {
		return raining;
	}

    /**
     * Makes the specified game player join this plot. If the arena is in solo mode and someone has already joined this
     * plot, that person will be kicked from this plot
     *
     * @param gamePlayer the player to join
     * @return whether ot not the player was able to join the plot
     * @since 2.1.0
     */
	public boolean join(GamePlayer gamePlayer) {
		YamlConfiguration messages = SettingsManager.getInstance().getMessages();
		
		if (arena.getMode() == ArenaMode.TEAM) {
			if (!isFull()) {
				gamePlayers.add(gamePlayer);
				
				for (String s : MessageManager.translate(messages.getStringList("join.plot.message")))
					MessageManager.getInstance().send(gamePlayer.getPlayer(), s.replace("%plot%", getID() + ""));
				
				return true;
			} else {
				MessageManager.getInstance().send(gamePlayer.getPlayer(), MessageManager.translate(messages.getStringList("join.plot.full")));
				return false;
			}
		} else {
			if (gamePlayers.size() == 1) {
				gamePlayers.remove(0);
				gamePlayers.add(gamePlayer);
			} else
				gamePlayers.add(gamePlayer);
			
			for (String s : MessageManager.translate(messages.getStringList("join.plot.message")))
				MessageManager.getInstance().send(gamePlayer.getPlayer(), s.replace("%plot%", getID() + ""));
			
			return true;
		}
	}

    /**
     * Makes the specified game player leave this plot
     *
     * @param gamePlayer the player to leave this plot
     * @since 2.1.0
     */
	public void leave(GamePlayer gamePlayer) {
		gamePlayers.remove(gamePlayer);
	}

	/**
     * Removes the specified spectator from this plot and thus makes him stop spectating
     *
     * @param spectator the spectator to remove
     * @since 2.1.0
     */
	@Contract("null -> fail")
	public void removeSpectator(GamePlayer spectator) {
		getAllGamePlayers().remove(spectator);
		
		for (GamePlayer player : getAllGamePlayers())
			player.getPlayer().showPlayer(spectator.getPlayer());
		
		Player spPlayer = spectator.getPlayer();
		spectator.restore();
		spPlayer.setCanPickupItems(true);
		
		ItemBuilder.check(spPlayer);
	}

	/**
     * Restores the previous state of this plot which includes its block states and data, its time, rain state and
     * particles
     *
     * @since 2.1.0
     */
	@SuppressWarnings("deprecation")
	public void restore() {
		YamlConfiguration config = SettingsManager.getInstance().getConfig();
		
		if (!config.getBoolean("restore-plots")) {
			return;
		}
		
		for (BlockState blockState : blocks) {
			blockState.getLocation().getBlock().setType(blockState.getType());
			blockState.getLocation().getBlock().setData(blockState.getRawData());
		}
		
		setRaining(false);
		setTime(Time.AM6);
		
		getParticles().clear();
	}

	/**
     * Saves the current state of the blocks of the plot into memory, so it can be reset later on. This will overwrite
     * any pre-existing data
     *
     * @since 2.1.0
     */
	public void save() {
		if (getBoundary() == null) {
			Main.getInstance().getLogger().warning("No boundary's found. Disabling auto-resetting plots...");
			return;
		}
		
		for (Block block : getBoundary().getAllBlocks()) {
			blocks.add(block.getState());
		}
	}

    /**
     * Sets the arena this plot belongs to
     *
     * @param arena the new arena
     * @since 2.1.0
     */
	public void setArena(Arena arena) {
		this.arena = arena;
	}

    /**
     * Sets the boundary of this plot
     *
     * @param boundary the new boundary
     * @since 2.1.0
     */
	public void setBoundary(Boundary boundary) {
		this.boundary = boundary;
	}

    /**
     * Sets the floor of this plot
     *
     * @param floor the new floor
     * @since 2.1.0
     */
	public void setFloor(Floor floor) {
		this.floor = floor;
	}

	/**
     * Sets the spawn location of this plot
     *
     * @param location the new location
     * @since 2.1.0
     */
	public void setLocation(Location location) {
		this.location = location;
	}

    /**
     * Sets whether it should rain or not on this plot. This will also update the weather state for all players.
     *
     * @param raining the new raining state
     * @since 2.1.0
     */
	public void setRaining(boolean raining) {
		this.raining = raining;
		if (raining) {
			for (GamePlayer gamePlayer : getGamePlayers()) {
				gamePlayer.getPlayer().setPlayerWeather(WeatherType.DOWNFALL);
			}
		} else {
			for (GamePlayer gamePlayer : getGamePlayers()) {
				gamePlayer.getPlayer().setPlayerWeather(WeatherType.CLEAR);
			}
		}
	}

    /**
     * Sets the new time that it should be on this plot. This will also update the time for all players.
     *
     * @param time the new time
     * @since 2.1.0
     */
	public void setTime(Time time) {
		this.time = time;
		for (GamePlayer gamePlayer : getGamePlayers()) {
			gamePlayer.getPlayer().setPlayerTime(time.decode(), false);
		}
	}
}