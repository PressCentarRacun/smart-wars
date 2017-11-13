package model;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import main.Constants;
import model.abilities.Ability;
import model.abilities.MirrorMagic;
import model.entitites.RoundEntity;
import model.abilities.Gun;
import util.Vector2D;
import view.Updatable;

public class Player extends RoundEntity {
	private final static int SERIALIZED_SIZE = 1 + 4 + 4 + 4;
	
	private PlayerSide playerSide;
	private double maxHealth;
	private double currHealth;
	private double speed; // pixels per second

	private ArrayList<Ability> abilities;
	private Gun gun;
	private MirrorMagic shortMirrorMagic;
	private MirrorMagic longMirrorMagic;

	public Player(PlayerSide playerSide) {
		super(playerSide == PlayerSide.LEFT_PLAYER ? Constants.LEFT_PLAYER_START_POS : Constants.RIGHT_PLAYER_START_POS,
				Constants.PLAYER_RADIUS);
		this.playerSide = playerSide;
		currHealth = maxHealth = Constants.PLAYER_HEALTH;
		speed = Constants.PLAYER_SPEED;
		gun = new Gun(this);
		shortMirrorMagic = new MirrorMagic(this, Constants.SHORT_MIRROR_LENGTH, false);
		longMirrorMagic = new MirrorMagic(this, Constants.LONG_MIRROR_LENGTH, true);
		abilities = new ArrayList<Ability>();
		abilities.add(gun);
		abilities.add(shortMirrorMagic);
		abilities.add(longMirrorMagic);
	}

	public void moveUp() {
		setVelocity(new Vector2D(0, -speed));
	}

	public void moveDown() {
		setVelocity(new Vector2D(0, speed));
	}

	public void stopMoving() {
		setVelocity(new Vector2D(0, 0));
	}

	public Bullet fireBullet() {
		return gun.fireBullet(position,
				this.playerSide == PlayerSide.LEFT_PLAYER ? new Vector2D(Constants.BULLET_SPEED, 0)
						: new Vector2D(-Constants.BULLET_SPEED, 0));
	}

	@Override
	public void update(double dt) {
		super.update(dt);
		for (Ability ability : abilities) {
			ability.update(dt);
		}
		position.clampY(20, Constants.WINDOW_HEIGHT - 20);
	}

	public PlayerSide getPlayerSide() {
		return playerSide;
	}

	public boolean isAlive() {
		return currHealth > 0;
	}

	public void receiveDamage(double damage) {
		currHealth -= damage;
		if (currHealth < 0) {
			currHealth = 0;
		}
	}

	public double getCurrHealth() {
		return currHealth;
	}

	@Override
	public boolean shouldCull() {
		return false;
	}
	
	public void doMirrorMagic(MirrorMagic mirrorMagic) {
		
	}

	public MirrorMagic getShortMirrorMagic() {
		return shortMirrorMagic;
	}

	public MirrorMagic getLongMirrorMagic() {
		return longMirrorMagic;
	}
	
	@Override
	public void serializeTo(ByteBuffer buffer) {
		super.serializeTo(buffer);
		buffer.put((byte)playerSide.getNum());
		buffer.putFloat((float)maxHealth);
		buffer.putFloat((float)currHealth);
		buffer.putFloat((float)speed);
	}
	
	@Override
	public void deserializeFrom(Model model, ByteBuffer buffer) {
		super.deserializeFrom(model, buffer);
		byte side = buffer.get();
		switch(side) {
		case 0x01: playerSide = PlayerSide.LEFT_PLAYER; break;
		case 0x02: playerSide = PlayerSide.RIGHT_PLAYER; break;
		}
		
		maxHealth = buffer.getFloat();
		currHealth = buffer.getFloat();
		speed = buffer.getFloat();
	}
	
	@Override
	public int getSerializedSize() {
		return super.getSerializedSize() + SERIALIZED_SIZE;
	}
}
