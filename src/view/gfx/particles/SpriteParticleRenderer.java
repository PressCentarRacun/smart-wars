package view.gfx.particles;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Constants;

public class SpriteParticleRenderer implements ParticleRenderer {
	private BufferedImage sprite;
	private ParticleAdditiveCompositor compositor;
	
	public SpriteParticleRenderer(BufferedImage sprite) {
		this.sprite = sprite;
		this.compositor = new ParticleAdditiveCompositor(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
	}

	@Override
	public void render(Graphics2D g, Particles particles) {
		compositor.compose(g, particles, sprite);
	}
}