package main;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.ClientController;
import controller.ServerController;
import debug.DebugDisplay;
import debug.Measurement;
import debug.PerformanceMonitor;
import debug.PerformanceOverlay;
import memory.Pools;
import model.Model;
import multiplayer.LocalMessageQueue;
import multiplayer.LocalPipe;
import multiplayer.NetworkException;
import rafgfxlib.GameFrame;
import util.ImageCache;
import view.ClientView;

@SuppressWarnings("serial")
public class GameWindow extends GameFrame implements GameStarter {
	private long lastUpdateTime;
	private long lastServerUpdateTime;
	private long lastViewUpdateTime;
	private long lastModelUpdateTime;

	private Model model;
	private ClientView view;
	private ClientController controller;
	private ServerController localServerController;
	private PerformanceOverlay po;
	private LoadingWindow loadingWindow;
	private GraphicsDevice device;
	private DisplayMode oldDisplayMode;
	private boolean fullscreen;
	private GameMode gameMode;
	private InetSocketAddress serverAddress;

	public GameWindow(boolean fullscreen, GameMode gameMode, InetSocketAddress serverAddress) {
		super("Smart Wars", Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

		this.fullscreen = fullscreen;
		this.gameMode = gameMode;
		this.serverAddress = serverAddress;
		loadingWindow = new LoadingWindow();
		loadingWindow.setVisible(true);

		po = null;
	}

	@Override
	public void initGameWindow(boolean fullscreen) {
		super.initGameWindow(fullscreen);
		getWindow().setLocationRelativeTo(null);
		getWindow().setVisible(false);
		getWindow().setBackground(Color.BLACK);

	}

	public void usePerformanceOverlay(PerformanceOverlay po) {
		this.po = po;
	}

	boolean skipFirst = false;
	
	@Override
	public void startGame() {
		if (Thread.currentThread() == runnerThread) {
			// Running startGame() from the game runner thread happens when the controller decides
			// the game should be restarted. However, this would deadlock, so if we're on the runner
			// thread, we'll ask Swing to run this same method from the UI thread.
			stopThread = true;
			SwingUtilities.invokeLater(() -> {
				startGame();
			});
			
			return;
		}
		
		stopThread = true;
		ImageCache.getInstance().preload(Constants.IMAGES_TO_PRELOAD);
		Pools.repopulate(true);

		model = new Model();
		// localServerModel = new Model();
		view = new ClientView();
		if (gameMode == GameMode.NETWORK) {
			controller = new ClientController(this, view, model, gameMode, serverAddress);
		} else {
			localServerController = new ServerController(this, model, gameMode);
			controller = new ClientController(this, view, model, gameMode, null);
		}
		lastUpdateTime = System.nanoTime();

		setUpdateRate(60);
		setBackgroundClear(false);
		setHighQuality(false);

		if (fullscreen) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			device = ge.getDefaultScreenDevice();

			oldDisplayMode = device.getDisplayMode();
			try {
				DisplayMode[] availableModes = device.getDisplayModes();
				DisplayMode dm = null;
				
				for (DisplayMode mode : availableModes) {
					if (mode.getWidth() == Constants.WINDOW_WIDTH && mode.getHeight() == Constants.WINDOW_HEIGHT) {
						if (dm == null) {
							dm = mode;
						} else {
							if (mode.getBitDepth() != DisplayMode.BIT_DEPTH_MULTI && mode.getBitDepth() > dm.getBitDepth()) {
								dm = mode;
							} else if (mode.getBitDepth() == dm.getBitDepth()) {
								if (mode.getRefreshRate() != DisplayMode.REFRESH_RATE_UNKNOWN && mode.getRefreshRate() > dm.getRefreshRate()) {
									dm = mode;
								}
							}
						}
					}
				}
				
				if (dm != null) {
					device.setFullScreenWindow(getWindow());
					device.setDisplayMode(dm);
				} else {
					System.err.println("Could not find a suitable resolution. Falling back to windowed mode.");
					fullscreen = false;
				}
				
			} catch (Exception e) {
				System.err.println("Setting fullscreen failed: " + e.getMessage());
				System.exit(-1);
			}
		}

		if (gameMode == GameMode.NETWORK) {
			// Run game thread after sync
			try {
				controller.setUpConnections();
			} catch (IOException ex) {
				System.err.println("Could not connect to the server: " + ex);
				System.exit(-1);
			}
		} else {
			LocalMessageQueue clientToServer = new LocalMessageQueue();
			LocalMessageQueue serverToClient = new LocalMessageQueue();
			localServerController.setLocalPipe(new LocalPipe(clientToServer, serverToClient));
			controller.setLocalPipe(new LocalPipe(serverToClient, clientToServer));
			/*
			 * model.updatePlayerId(PlayerSide.LEFT_PLAYER,
			 * model.getPlayerOnSide(PlayerSide.LEFT_PLAYER).getUuid());
			 * model.updatePlayerId(PlayerSide.RIGHT_PLAYER,
			 * model.getPlayerOnSide(PlayerSide.RIGHT_PLAYER).getUuid());
			 */
		}

		startThread();
		loadingWindow.setVisible(false);
		getWindow().setVisible(true);

		lastUpdateTime = System.nanoTime();
		lastServerUpdateTime = System.nanoTime();
		lastModelUpdateTime = System.nanoTime();
		lastViewUpdateTime = System.nanoTime();
	}

	@Override
	public void handleWindowInit() {
	}

	@Override
	public void handleWindowDestroy() {
		if (fullscreen) {
			device.setDisplayMode(oldDisplayMode);
			device.setFullScreenWindow(null);
			getWindow().setVisible(false);
		}
	}

	@Override
	public void render(Graphics2D g, int sw, int sh) {
		Measurement ms = PerformanceMonitor.getInstance().measure("DrawTotal");
		view.draw(g);
		ms.done();

		if (po != null) {
			po.draw(g);
		}

		DebugDisplay.getInstance().draw(g);
	}

	@Override
	public void update() {
		try {
			doUpdate();
		} catch (NetworkException e) {
			JOptionPane.showMessageDialog(null, "A network error has occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
	}
	
	public void doUpdate() {
		Measurement ms;
		PerformanceMonitor m = PerformanceMonitor.getInstance();
		if (gameMode == GameMode.LOCAL) {
			ms = m.measure("SrvControllerUpdateTotal");
			double dtServer = (double) (System.nanoTime() - lastServerUpdateTime) / 1e9;
			localServerController.update(dtServer);

			ms.done();
			lastServerUpdateTime = System.nanoTime();
			// localServerModel.update(dt);
		}
		
		ms = m.measure("ControllerUpdateTotal");
		double dtController = (double) (System.nanoTime() - lastUpdateTime) / 1e9;
		controller.update(dtController);

		lastUpdateTime = System.nanoTime();
		ms.done();

		ms = m.measure("ModelUpdateTotal");
		double dtModel = (double) (System.nanoTime() - lastModelUpdateTime) / 1e9;
		model.update(dtModel);

		lastModelUpdateTime = System.nanoTime();
		ms.done();

		ms = m.measure("ViewUpdateTotal");
		double dtView = (double) (System.nanoTime() - lastViewUpdateTime) / 1e9;
		view.update(dtView);

		lastViewUpdateTime = System.nanoTime();
		ms.done();
	}

	@Override
	public void handleMouseDown(int x, int y, GFMouseButton button) {
	}

	@Override
	public void handleMouseUp(int x, int y, GFMouseButton button) {
	}

	@Override
	public void handleMouseMove(int x, int y) {
	}

	@Override
	public void handleKeyDown(int keyCode) {
		if (keyCode == KeyEvent.VK_ESCAPE) {
			handleWindowDestroy();
			System.exit(0);
		}

		controller.handleKeyDown(keyCode);
	}

	@Override
	public void handleKeyUp(int keyCode) {
		controller.handleKeyUp(keyCode);
	}
}
