package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import debug.DebugDisplay;
import debug.PerformanceMonitor;
import main.GameState;
import memory.Pools;
import model.entitites.Entity;
import multiplayer.NetworkObject;
import util.Vector2D;

public class Model extends NetworkObject {

	private HashMap<PlayerSide, Player> players;
	private HashMap<UUID, Entity> entities;
	private ArrayList<Bullet> bullets; // separate bullets for culling
	private ArrayList<Asteroid> asteroids; // separate asteroids for culling
	private ArrayList<Mirror> mirrors; // separate mirrors for culling
	private ArrayList<Wormhole> wormholes; // separate wormholes for culling

	private GameState gameState;

	public Model() {
		super();
		gameState = GameState.RUNNING;
		players = new HashMap<>();
		players.put(PlayerSide.LEFT_PLAYER, new Player(PlayerSide.LEFT_PLAYER, null));
		players.put(PlayerSide.RIGHT_PLAYER, new Player(PlayerSide.RIGHT_PLAYER, null));
		bullets = new ArrayList<>();
		asteroids = new ArrayList<>();
		mirrors = new ArrayList<>();
		wormholes = new ArrayList<>();

		entities = new HashMap<>();
		synchronized (entities) {
			addEntity(players.get(PlayerSide.LEFT_PLAYER));
			addEntity(players.get(PlayerSide.RIGHT_PLAYER));
		}
	}

	public Player getPlayerOnSide(PlayerSide playerSide) {
		return players.get(playerSide);
	}

	public void update(double dt) {
		if (gameState == GameState.RUNNING) {
			synchronized (entities) {
				for (Entity entity : entities.values()) {
					entity.update(dt);
				}
			}
		}

		PerformanceMonitor m = PerformanceMonitor.getInstance();
		m.recordStatistic("CountEntities", entities.size());
		m.recordStatistic("CountBullets", bullets.size());
		m.recordStatistic("CountAsteroids", asteroids.size());
		m.recordStatistic("CountMirrors", mirrors.size());
		m.recordStatistic("CountWormholes", wormholes.size());

		DebugDisplay dd = DebugDisplay.getInstance();
		if (dd.isEnabled()) {
			for (Entity e : entities.values()) {
				dd.addRectangle("EntityBoundingBoxes", e.getBoundingBox());
			}

			dd.addOval("PlayerCircles", players.get(PlayerSide.LEFT_PLAYER).getBoundingBox());
			dd.addOval("PlayerCircles", players.get(PlayerSide.RIGHT_PLAYER).getBoundingBox());

			for (Wormhole w : wormholes) {
				dd.addOval("WormholeCircles", w.getBoundingBox());
			}

			for (Mirror mr : mirrors) {
				Vector2D pos = mr.getPosition();
				Vector2D lv = mr.getLineVector();

				Vector2D tl = pos.sub(lv.scale(0.5)), br = pos.add(lv.scale(0.5));
				dd.addLine("MirrorLines", new Point((int) tl.getX(), (int) tl.getY()),
						new Point((int) br.getX(), (int) br.getY()));

			}
		}
	}

	public void addEntity(Entity entity) {
		synchronized (entities) {
			entities.put(entity.getUuid(), entity);
			if (entity instanceof Bullet) {
				bullets.add((Bullet) entity);
			} else if (entity instanceof Asteroid) {
				asteroids.add((Asteroid) entity);
			} else if (entity instanceof Mirror) {
				mirrors.add((Mirror) entity);
			} else if (entity instanceof Wormhole) {
				wormholes.add((Wormhole) entity);
			}
		}
	}

	int ai = 0;

	public void removeEntity(Entity entity) {
		synchronized (entities) {
			entities.remove(entity.getUuid());
			if (entity instanceof Bullet) {
				bullets.remove((Bullet) entity);
				Pools.BULLET.free((Bullet) entity);
			} else if (entity instanceof Asteroid) {
				asteroids.remove((Asteroid) entity);
				Pools.ASTEROID.free((Asteroid) entity);
			} else if (entity instanceof Mirror) {
				mirrors.remove((Mirror) entity);
			} else if (entity instanceof Wormhole) {
				wormholes.remove((Wormhole) entity);
			}
		}
	}

	public ArrayList<Bullet> getBullets() {
		return bullets;
	}

	public ArrayList<Asteroid> getAsteroids() {
		return asteroids;
	}

	public ArrayList<Mirror> getMirrors() {
		return mirrors;
	}

	public ArrayList<Wormhole> getWormholes() {
		return wormholes;
	}

	public Collection<Entity> getEntities() {
		return entities.values();
	}

	public Entity getEntityById(UUID id) {
		synchronized(entities) {
			return entities.get(id);
		}
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
		if (gameState != GameState.RUNNING) {
			// Purge everything
			for (Asteroid a : asteroids) {
				Pools.ASTEROID.free(a);
			}

			for (Bullet b : bullets) {
				Pools.BULLET.free(b);
			}
		}
	}

	public GameState getGameState() {
		return gameState;
	}

	public void updatePlayerId(PlayerSide side, UUID uuid) {
		Player player = getPlayerOnSide(side);
		entities.remove(player.getUuid());
		player.setUuid(uuid);
		entities.put(uuid, player);
	}
}
