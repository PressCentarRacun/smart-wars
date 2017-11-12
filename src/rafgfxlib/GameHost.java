package rafgfxlib;

import java.awt.BorderLayout;
import java.awt.BufferCapabilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.JFrame;

/**
 * Glavni objekat za složenije aplikacije, koje će imati više odvojenih stanja. Treba ga
 * instancirati samo jednom, ovaj objekat je vlasnik prozora i preko njega se postavlja
 * trenutno aktivno stanje, koje će onda da bude pozivano za ažuriranje i iscrtavanje, te
 * će samo ono dobijati input događaje.
 * @author Aleksandar
 *
 */
public class GameHost implements MouseListener, 
MouseWheelListener, MouseMotionListener, KeyListener
{
	
	static 
	{
	    System.setProperty("sun.java2d.transaccel", "True");
	    //if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
	    	//System.setProperty("sun.java2d.d3d", "True");
	    //else
	    System.setProperty("sun.java2d.opengl", "true");
	    System.setProperty("sun.java2d.ddforcevram", "True");
	}
	
	public static enum GFMouseButton
	{
		None,
		Left,
		Middle,
		Right,
		WheelUp,
		WheelDown,
		WheelLeft,
		WheelRight,
		Special1,
		Special2
	}

	private int screenX = 640;
	private int screenY = 480;
	
	private static JFrame myFrame = null;
	private static Canvas myPanel = null;
	private String title = "RAF GameHost";
	
	private Color backColor = Color.blue;
	private boolean clearBackBuffer = true;
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private int updateRate = 30;
	private int swapInterval = 2;
	private boolean hwAccelAvailable = false;
	private int refreshRate = 0;
	private boolean updatesRunning = true;
	private boolean fullscreenMode = false;
	private boolean renderRunning = true;
	
	private boolean useHQ = false;
	
	private Thread runnerThread = null;
	
	private boolean[] mouseButtons = new boolean[GFMouseButton.values().length];
	private boolean[] keyboardKeys = new boolean[1024];
	
	private GameState currentState = null;
	private GameState nextState = null;
	
	private HashMap<String, GameState> states = new HashMap<String, GameState>();
	
	private boolean gameRunning = true;
	private BufferStrategy buffStrategy;
	private BufferCapabilities buffCapabilities;
	
	/**
	 * Glavni objekat za složenije aplikacije, koje će imati više odvojenih stanja. Treba ga
	 * instancirati samo jednom, ovaj objekat je vlasnik prozora i preko njega se postavlja
	 * trenutno aktivno stanje, koje će onda da bude pozivano za ažuriranje i iscrtavanje, te
	 * će samo ono dobijati input događaje.
	 * @param title naslov prozora
	 * @param sizeX širina prostora za crtanje u pikselima
	 * @param sizeY visina prostora za crtanje u pikselima
	 */
	public GameHost(String title, int sizeX, int sizeY)
	{
		initGameHost(title, sizeX, sizeY, false);
	}
	
	public GameHost(String title, int sizeX, int sizeY, boolean fullscreen)
	{
		initGameHost(title, sizeX, sizeY, fullscreen);
	}
	
	private void initGameHost(String title, int sizeX, int sizeY, boolean fullscreen)
	{
		if(sizeX < 320) sizeX = 320;
		if(sizeY < 240) sizeY = 240;
		
		if(sizeX > 2048) sizeX = 2048;
		if(sizeY > 2048) sizeY = 2048;
		
		fullscreenMode = fullscreen;
		
		if(fullscreenMode)
		{
			sizeX = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
			sizeY = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
		}
		
		screenX = sizeX;
		screenY = sizeY;
		
		myPanel = new Canvas();
		myPanel.setSize(screenX, screenY);
		
		if(title != null) this.title = title;
		
		this.title = title;
		
		myPanel.addMouseListener(this);
		myPanel.addMouseMotionListener(this);
		myPanel.addMouseWheelListener(this);
		myPanel.addKeyListener(this);
		
		initGameWindow();
		
		myPanel.createBufferStrategy(2);
		buffStrategy = myPanel.getBufferStrategy();
		buffCapabilities = buffStrategy.getCapabilities();
		
		//System.out.println("Fullscreen required for page-flip : " + buffCapabilities.isFullScreenRequired());
		//System.out.println("Is page flipping : " + buffCapabilities.isPageFlipping());
		//System.out.println("Is fullscreen supported : " + GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isFullScreenSupported());
		hwAccelAvailable = buffCapabilities.isPageFlipping() && !buffCapabilities.isFullScreenRequired();
		//System.out.println("HW acceleration status : " + hwAccelAvailable);
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] gs = ge.getScreenDevices();

	    for (int i = 0; i < gs.length; i++) 
	    {
	    	DisplayMode dm = gs[i].getDisplayMode();
	    	int drefreshRate = dm.getRefreshRate();
	    	if (drefreshRate != DisplayMode.REFRESH_RATE_UNKNOWN)
	    	{
	    		refreshRate = drefreshRate;
	    		if(System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0)
	    		{
	    			if(refreshRate < 60)
	    				refreshRate = 60;
	    		}
	    		//if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
	    		break;
	    	}
	    }
	    
	    if(fullscreenMode)
	    {
	    	GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(myFrame);
	    }
	    //System.getProperties().list(System.out);
		
		runnerThread = new Thread(new Runnable()
		{
			
			@Override
			public void run()
			{
				//System.out.println("S - refreshRate = " + refreshRate);
				//System.out.println("S - updateRate = " + updateRate);
				//System.out.println("S - swapInterval = " + swapInterval);
				
				while(gameRunning)
				{
					long startTime = System.currentTimeMillis();
					
					if(updatesRunning) 
						tick();
					
					if(renderRunning)
					{
						Graphics g = buffStrategy.getDrawGraphics();
						doRendering(g);
						g.dispose();
						
						if(swapInterval > 0)
						{
							buffStrategy.show();
							for(int i = 0; i < swapInterval; i++)
							{
								Toolkit.getDefaultToolkit().sync();
								
								if(!hwAccelAvailable || swapInterval > 1)
								{
									try
									{
										long frameTime = System.currentTimeMillis() - startTime;
										long sleepTime = 1000 / updateRate - frameTime;
										
										if(sleepTime > 0)
											Thread.sleep(sleepTime);
									} 
									catch (InterruptedException e) { }
								}
							}
						}
						else
						{
							buffStrategy.show();
						}
					}
					
					if(swapInterval == 0)
					{
						try
						{
							long frameTime = System.currentTimeMillis() - startTime;
							long sleepTime = 1000 / updateRate - frameTime;
							
							if(sleepTime > 0)
								Thread.sleep(sleepTime);
						} 
						catch (InterruptedException e) { }
					}
				}
				
				System.exit(0);
			}
		});
		
		//runnerThread.start();
	}
	
	private void doRendering(Graphics g)
	{
		if(clearBackBuffer)
		{
			g.setColor(backColor);
			g.fillRect(0, 0, screenX, screenY);
		}
		
		if(useHQ) 
		{
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		else
		{
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
		
		if(currentState != null)
			currentState.render((Graphics2D)g, getWidth(), getHeight());
	}
	
	/**
	 * Vraća prijavljene GameState objekte po nazivu
	 * @param name naziv koji traženi GameState daje u getName() pozivu
	 * @return referenca na GameState ako je nađen, ili null ako nije
	 */
	public GameState getState(String name)
	{
		return states.get(name);
	}
	
	/**
	 * Prelazak na novo stanje, po referenci
	 * @param nextState referenca na GameState objekat, trebao bi biti jedan od prijavljenih
	 */
	public void setState(GameState nextState)
	{
		if(nextState == null) return;
		if(currentState == nextState) return;
		
		this.nextState = nextState;
	}
	
	/**
	 * Prelazak na novo stanje, po nazivu
	 * @param stateName naziv stanja, kako ga vraća njegova getName() metoda
	 */
	public void setState(String stateName)
	{
		boolean shouldStart = currentState == null;
		setState(getStateByName(stateName));
		if(shouldStart) runnerThread.start();
	}
	
	/**
	 * Referenca na trenutno aktivno stanje
	 * @return referenca na GameState objekat
	 */
	public GameState getCurrentState()
	{
		return currentState;
	}
	
	/**
	 * Traženje reference na bilo koje od trneutno prijavljenih stanja
	 * @param name naziv stanja, kako ga vraća njegova getName() metoda
	 * @return referenca ako je stanje pronađeno, null ako nije
	 */
	public GameState getStateByName(String name)
	{
		return states.get(name);
	}
	
	/**
	 * Prijavljuje stanje na ovaj Host, ovo *ne bi trebalo ručno raditi*, jer
	 * se prijavljivanje radi u konstruktoru stanja.
	 * @param state referenca na stanje koje se registruje
	 */
	public void registerState(GameState state)
	{
		if(state == null)
			return;
		
		if(!states.containsValue(state))
			states.put(state.getName(), state);
	}
	
	private void tick()
	{
		if(nextState != null)
		{
			if(currentState != null) currentState.suspendState();
			
			currentState = nextState;
			nextState = null;
			
			currentState.resumeState();
		}
		
		if(currentState != null) currentState.update();
	}
	
	private void initGameWindow()
	{
		if(myFrame != null)
		{
			System.out.println("initGameWindow() already called, can't do again");
			return;
		}
		
		myFrame = new JFrame(title);
		
		if(fullscreenMode)
		{
			myFrame.setUndecorated(true);
		}
		
		myFrame.setLayout(new BorderLayout());
		
		myPanel.setPreferredSize(new Dimension(screenX, screenY));
		myPanel.setMaximumSize(new Dimension(screenX, screenY));
		myFrame.add(myPanel, BorderLayout.CENTER);
		myFrame.setResizable(false);
		myFrame.pack();
		myFrame.setVisible(true);
		myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		myFrame.addKeyListener(this);
		
		myFrame.setIgnoreRepaint(true);
		
		//handleWindowInit();
		
		myFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	            public void windowClosing(java.awt.event.WindowEvent evt){
	            	if(currentState != null)
	            	{
	            		if(currentState.handleWindowClose())
	            			gameRunning = false;
	            	}
	            	else
	            	{
	            		gameRunning = false;
	            	}
	            }
	       });
		
		
	}
	
	/**
	 * Metod koji programski prekida izvršavanje aplikacije čim se trenutni frejm završi.
	 */
	public void exit()
	{
		gameRunning = false;
	}
	
	/**
	 * Metod koji daje širinu prostora za crtanje, pošto prozor nije resizable, treba
	 * biti ista vrijednost koja je zadata i u konstruktoru.
	 * @return širina u pikselima
	 */
	public int getWidth()
	{
		return screenX;
	}
	
	/**
	 * Metod koji daje visinu prostora za crtanje, pošto prozor nije resizable, treba
	 * biti ista vrijednost koja je zadata i u konstruktoru.
	 * @return visina u pikselima
	 */
	public int getHeight()
	{
		return screenY;
	}
	
	/**
	 * Metod koji će da iscrta zadato stanje, ali u off-screen sliku, umjesto na ekran.
	 * Obratiti pažnju da se ovo ne pozove iz render() metode istog stanja, jer bi to
	 * izazvalo beskonačnu rekurziju.
	 * @param canvas objekat slike u koju će se crtati, može biti null, pa će nova slika biti alocirana;
	 * ako se ovo radi često, bolje je imati jednu unaprijed alociranu sliku koja će se reciklirati.
	 * @param state stanje koje se treba iscrtati, biće pozvan njegov render() metod
	 * @return vraća referencu na proslijeđenu ili novokonstruisanu sliku, u koju je iscrtano stanje
	 */
	public BufferedImage renderSnapshot(BufferedImage canvas, GameState state)
	{
		if(canvas == null)
			canvas = new BufferedImage(screenX, screenY, BufferedImage.TYPE_3BYTE_BGR);
		
		Graphics2D g = (Graphics2D)canvas.getGraphics();
		
		if(clearBackBuffer)
		{
			g.setColor(backColor);
			g.fillRect(0, 0, screenX, screenY);
		}
		
		if(useHQ) 
		{
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		else
		{
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
		
		if(state != null)
			state.render((Graphics2D)g, screenX, screenY);
		
		return canvas;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0)
	{
		GFMouseButton button = GFMouseButton.None;
		if(arg0.getButton() == 1) button = GFMouseButton.Left;
		if(arg0.getButton() == 2) button = GFMouseButton.Middle;
		if(arg0.getButton() == 3) button = GFMouseButton.Right;
		
		if(arg0.getButton() == 5) button = GFMouseButton.Special1;
		if(arg0.getButton() == 4) button = GFMouseButton.Special2;
		
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		mouseButtons[button.ordinal()] = true;
		
		if(currentState != null)
			currentState.handleMouseDown(mouseX, mouseY, button);	
	}

	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		GFMouseButton button = GFMouseButton.None;
		if(arg0.getButton() == 1) button = GFMouseButton.Left;
		if(arg0.getButton() == 2) button = GFMouseButton.Middle;
		if(arg0.getButton() == 3) button = GFMouseButton.Right;
		
		if(arg0.getButton() == 5) button = GFMouseButton.Special1;
		if(arg0.getButton() == 4) button = GFMouseButton.Special2;
		
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		mouseButtons[button.ordinal()] = false;
		
		if(currentState != null)
			currentState.handleMouseUp(mouseX, mouseY, button);	
	}

	@Override
	public void keyPressed(KeyEvent arg0)
	{
		if(arg0.getKeyCode() >= 0 && arg0.getKeyCode() < 1024)
			keyboardKeys[arg0.getKeyCode()] = true;
		
		if(currentState != null)
			currentState.handleKeyDown(arg0.getKeyCode());
	}

	@Override
	public void keyReleased(KeyEvent arg0)
	{
		if(arg0.getKeyCode() >= 0 && arg0.getKeyCode() < 1024)
			keyboardKeys[arg0.getKeyCode()] = false;
		
		if(currentState != null)
			currentState.handleKeyUp(arg0.getKeyCode());
	}

	@Override
	public void keyTyped(KeyEvent arg0) {}

	@Override
	public void mouseDragged(MouseEvent arg0) 
	{
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		if(currentState != null)
			currentState.handleMouseMove(mouseX, mouseY);
	}

	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		if(currentState != null)
			currentState.handleMouseMove(mouseX, mouseY);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		if(arg0.getWheelRotation() > 0)
		{
			mouseButtons[GFMouseButton.WheelDown.ordinal()] = true;
			mouseX = arg0.getX();
			mouseY = arg0.getY();
			
			if(currentState != null)
				currentState.handleMouseDown(mouseX, mouseY, GFMouseButton.WheelDown);
			
			mouseButtons[GFMouseButton.WheelDown.ordinal()] = false;
		}
		else if(arg0.getWheelRotation() < 0)
		{
			mouseButtons[GFMouseButton.WheelUp.ordinal()] = true;
			mouseX = arg0.getX();
			mouseY = arg0.getY();
			
			if(currentState != null)
				currentState.handleMouseDown(mouseX, mouseY, GFMouseButton.WheelUp);
			
			mouseButtons[GFMouseButton.WheelUp.ordinal()] = false;
		}
	}
	
	/**
	 * Upit trenutnog stanja tastera miša, putem GFMouseButton enuma
	 * @param button taster za koje se upit vrši
	 * @return true ako je taster pritisnut, false ako nije
	 */
	public boolean isMouseButtonDown(GFMouseButton button)
	{
		return mouseButtons[button.ordinal()];
	}
	
	/**
	 * Upit trenutnog stanja tastera na tastaturi
	 * @param keyCode kod tipke, koristiti konstante iz KeyEvent.VK_*
	 * @return true ako je taster pritisnug, false ako nije
	 */
	public boolean isKeyDown(int keyCode)
	{
		if(keyCode >= 0 && keyCode < 1024)
			return keyboardKeys[keyCode];
		else
			return false;
	}
	
	/**
	 * Daje referencu na JFrame prozor aplikacije
	 * @return JFrame
	 */
	public JFrame getWindow()
	{
		return myFrame;
	}
	
	/**
	 * Pali ili gasi automatsko čišćenje pozadine prije render() poziva
	 * @param clr true za automatsko brisanje, sa false se prethodna slika zadržava  
	 */
	public void setBackgroundClear(boolean clr)
	{
		clearBackBuffer = clr;
	}
	
	/**
	 * Boja na koju će biti postavljena pozadina, ako je setBackgroundClear uključeno
	 * @param c boja pozadine
	 */
	public void setBackgroundClearColor(Color c)
	{
		backColor = c;
	}
	
	/**
	 * Trenutna horizontalna pozicija kursora miša u prostoru za crtanje
	 * @return X koordinata kursora u pikselima
	 */
	public int getMouseX()
	{
		return mouseX;
	}
	
	/**
	 * Trenutna vertikalna pozicija kursora miša u prostoru za crtanje
	 * @return Y koordinata kursora u pikselima
	 */
	public int getMouseY()
	{
		return mouseY;
	}
	
	/**
	 * Postavlja hint za viši kvalitet iscrtavanja koji će se onda automatski primjenjivati
	 * nad Graphics2D objektom koji se daje u render() metodi
	 * @param hq true za viši kvalitet interpolacije i uključen anti-aliasing primitiva
	 */
	public void setHighQuality(boolean hq)
	{
		useHQ = hq;
	}
	
	/**
	 * Postavlja ikonicu prozora
	 * @param icon Image objekat (može biti BufferedImage)
	 */
	public void setIcon(Image icon)
	{
		myFrame.setIconImage(icon);
	}
	
	/**
	 * Postavlja ciljnu frekvenciju ažuriranja u Hz/fps. Tajming je realizovan jednostavnim
	 * sleep metodama, zbog čega je moguć neravnomijeran tok izvršavanja (judder). Ukoliko
	 * ažuriranje i iscrtavanje traje duže od (1 / fps) sekundi, svukupan tempo izvršavanja
	 * će se usporiti na tu brzinu, nije implementiran nikakav dinamički update ili frameskipping.  
	 * @param fps ciljni broj ažuriranih i iscrtanih okvira u sekundi, od 1 do 120
	 */
	public void setUpdateRate(int fps)
	{
		if(fps >= 1 && fps < 240)
		{
			updateRate = fps;
			if(refreshRate > 0)
			{
				if(fps <= refreshRate && refreshRate % fps == 0)
					swapInterval = refreshRate / fps;
				else
					swapInterval = 0;
			}
			else
			{
				swapInterval = 0;
			}
		}
		else
		{
			System.out.println("Valid range for setUpdateRate is 1 - 120");
		}
	}
	
	/**
	 * Vraća ciljnu frekvenciju ažuriranja u Hz/fps
	 * @return frekvencija u Hz
	 */
	public int getUpdateRate()
	{
		return updateRate;
	}
	
	public void setFullscreenMode(boolean fsm)
	{
		fullscreenMode = fsm;
	}
	
	public boolean getFullscreenMode()
	{
		return fullscreenMode;
	}
}
