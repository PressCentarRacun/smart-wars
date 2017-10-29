package view;
import java.awt.Graphics2D;
import java.util.ArrayList;

import controller.MainController;
import main.Constants;
import model.Bullet;
import model.Model;
import rafgfxlib.GameFrame;
import util.Vector2D;

public class MainView extends GameFrame {
	private BackdropView backdrop;
	private long lastUpdateTime;
	
	private ArrayList<Drawable> drawables;
	private ArrayList<Updatable> updatables;
	
	private Model model;
	private MainController controller;
	
	private Bullet b;
	
	public MainView() {
		super("Smart Wars", Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

		drawables = new ArrayList<>();
		updatables = new ArrayList<>();
	
		lastUpdateTime = System.nanoTime();
		backdrop = new BackdropView();
		drawables.add(backdrop);
		updatables.add(backdrop);
		
		this.model = new Model();
		this.controller = new MainController(this, this.model);
		
		PlayerView bottomPlayerView = new PlayerView(model.getBottomPlayer(), false);
		drawables.add(bottomPlayerView);
		updatables.add(bottomPlayerView);

		Bullet b = new Bullet(new Vector2D(200,0), new Vector2D(0, 50));
		BulletView bv = new BulletView(b);
		this.b=b;
		PlayerView topPlayerView = new PlayerView(model.getTopPlayer(), true);
		drawables.add(topPlayerView);
		updatables.add(topPlayerView);
		drawables.add(bv);
		updatables.add(bv);
		
		setUpdateRate(60);
		startThread();
	}

	public void addDrawable(Drawable d) {
		drawables.add(d);
	}
	
	public void removeDrawable(Drawable d) {
		drawables.remove(d);
	}

	public void addUpdatable(BulletView bv) {
		this.updatables.add(bv);
	}
	
	@Override
	public void handleWindowInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleWindowDestroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(Graphics2D g, int sw, int sh) {
		for (Drawable d : drawables) {
			d.draw(g);
		}
	}

	@Override
	public void update() {
		long currentTime = System.nanoTime();
		double dt = (currentTime - lastUpdateTime) / 1000000000.0;
		
		b.update(dt);
		model.update(dt);
		controller.update();
		for (Updatable u : updatables) {
			u.update(dt);
		}
		
		lastUpdateTime = currentTime;
	}

	@Override
	public void handleMouseDown(int x, int y, GFMouseButton button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMouseUp(int x, int y, GFMouseButton button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMouseMove(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleKeyDown(int keyCode) {
		this.controller.handleKeyDown(keyCode);
	}

	@Override
	public void handleKeyUp(int keyCode) {
		this.controller.handleKeyUp(keyCode);
	}
}
