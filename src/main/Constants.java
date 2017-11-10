package main;

import util.Vector2D;

public class Constants {
	public static final int WINDOW_WIDTH = 1280;
	public static final int WINDOW_HEIGHT = 720;

	public static final double BACKGROUND_ALPHA_PER_SECOND = 0.05f;
	public static final double STARS_ALPHA_PER_SECOND = 0.12f;

	public static final int PLAYER_ANIMATION_FPS = 24;

	// Z-index
	public static final int Z_BACKDROP = 0;
	public static final int Z_BLACK_HOLE = 1;
	public static final int Z_BULLETS = 2;
	public static final int Z_ASTEROID = 3;
	public static final int Z_PLAYER = 4;
	public static final int Z_OVERLAY = 5;
	public static final int Z_MIRROR = 6;

	// Player
	public static final Vector2D LEFT_PLAYER_START_POS = new Vector2D(60, WINDOW_HEIGHT / 2);
	public static final Vector2D RIGHT_PLAYER_START_POS = new Vector2D(WINDOW_WIDTH - 60, WINDOW_HEIGHT / 2);
	public static final double PLAYER_SPEED = 300;
	public static final double PLAYER_RADIUS = 20;
	public static final int PLAYER_HEALTH = 100;

	// Bullet
	public static final double BULLET_SPEED = 500;
	public static final Vector2D BULLET_SIZE = new Vector2D(8, 8);
	public static final double BULLET_COOLDOWN = 0.1;
	public static final double BULLET_DAMAGE = 10;
	public static final double BULLET_TELEPORT_COOLDOWN = 1;
	
	// Asteroid
	public static final double ASTEROID_SPAWN_PROBABILITY = 0.9; // per second
	public static final int ASTEROID_TYPE_COUNT = 6;
	public static final int ASTEROID_SPRITES_X = 4;
	public static final int ASTEROID_SPRITES_Y = 4;
	public static final int ASTEROID_SPAWN_X_MIN = (int) (LEFT_PLAYER_START_POS.getdX() + 80);
	public static final int ASTEROID_SPAWN_X_MAX = (int) (RIGHT_PLAYER_START_POS.getdX() - 80);
	public static final int ASTEROID_SPAWN_Y = -50;
	public static final double ASTEROID_DISINTEGRATION_TIME = 0.4;
	
	public static final int ASTEROID_PLAYER_DAMAGE = 25;
	
	public static final int ASTEROID_Y_VELOCITY = 100;
	public static final int ASTEROID_Y_VELOCITY_JITTER = 20;
	public static final int ASTEROID_X_VELOCITY = 20;
	public static final int ASTEROID_X_VELOCITY_JITTER = 7;
	public static final int ASTEROID_FPS = 5;
	public static final double ASTEROID_PUSH_VELOCITY_INTENSITY = 2;
	public static final double ASTEROID_PUSH_POSITION_INTENSITY = 2;
	
	// Mirror
	public static final double MIRROR_MAGIC_COOLDOWN = 5;
	public static final double MIRROR_VELOCITY = 200;
	public static final double SHORT_MIRROR_LENGTH = 20;
	public static final double LONG_MIRROR_LENGTH = 100;
	public static final double MIRROR_DIST_EPS = 10;
	public static final int MIRROR_X_MIN = (int) (LEFT_PLAYER_START_POS.getdX() + 80);
	public static final int MIRROR_X_MAX = (int) (RIGHT_PLAYER_START_POS.getdX() - 80);
	public static final int MIRROR_X_LAUNCH_LEFT = MIRROR_X_MIN + 10;
	public static final int MIRROR_X_LAUNCH_RIGHT = MIRROR_X_MAX - 10;
	public static final double MIRROR_ANGULAR_VELOCITY = Math.PI;
	
	// Black hole
	public static final double WORMHOLE_SPAWN_PROBABILITY = 0.1;
	public static final double WORMHOLE_RADIUS = 65;
	public static final double WORMHOLE_BIRTH_TIME = 0.7;
	public static final double WORMHOLE_ANGULAR_VELOCITY = Math.PI / 2;
	public static final double WORMHOLE_LIFETIME = 30;
}

