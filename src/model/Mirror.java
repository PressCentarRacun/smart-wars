package model;

import java.awt.Rectangle;

import main.Constants;
import model.abilities.Ability;
import model.entitites.LineEntity;
import util.Vector2D;

public class Mirror extends LineEntity {

	private MirrorState mirrorState;

	public Mirror(Vector2D position, Vector2D velocity, double length) {
		super(position, velocity, length);
		mirrorState = MirrorState.TRAVELLING;
	}

	@Override
	public void update(double dt) {
		if (mirrorState == MirrorState.TRAVELLING) {
			super.update(dt);
			if (position.getdX() < Constants.MIRROR_X_MIN || position.getdX() > Constants.MIRROR_X_MAX) {
				System.out.println(position.getdX());
				System.out.println("CLAMP");
				position.clampdX(Constants.MIRROR_X_MIN, Constants.MIRROR_X_MAX);
				velocity = new Vector2D(0, 0);
				mirrorState = MirrorState.SPINNING;
			}
		} else if (mirrorState == MirrorState.SPINNING) {
			angle += Constants.MIRROR_ANGULAR_VELOCITY * dt;
			while (angle > Math.PI) {
				angle -= 2 * Math.PI;
			}
		}
	}

	public MirrorState getMirrorState() {
		return mirrorState;
	}

	public void setMirrorState(MirrorState mirrorState) {
		this.mirrorState = mirrorState;
	}

	@Override
	public boolean shouldCull() {
		return false;
	}

}
