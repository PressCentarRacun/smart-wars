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

import javax.swing.JFrame;

/**
 * Klasa namijenjena jednostavnim grafiÄ�kim eksperimentima i demonstracijama. Koristi se
 * nasljeÄ‘ivanjem i popunjavanjem apstraktnih metoda poput update() i render(), uz
 * metode koje obraÄ‘uju input dogaÄ‘aje, ako je potrebna interakcija.
 * @author Aleksandar StanÄ�iÄ‡
 *
 */
public abstract class GameFrame extends Canvas implements MouseListener, 
	MouseWheelListener, MouseMotionListener, KeyListener
{
	private static final long serialVersionUID = 6058915663486070170L;
	
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
	private String title = "RAF GameFrame";
	
	private Color backColor = Color.black;
	private boolean clearBackBuffer = true;
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private int updateRate = 30;
	private boolean updatesRunning = true;
	private boolean renderRunning = true;
	
	private boolean useHQ = false;
	
	private Thread runnerThread = null;
	
	private boolean[] mouseButtons = new boolean[GFMouseButton.values().length];
	private boolean[] keyboardKeys = new boolean[1024];
	private BufferStrategy buffStrategy;
	private BufferCapabilities buffCapabilities;
	private boolean hwAccelAvailable;
	private int refreshRate;
	private int swapInterval;
	
	protected boolean stopThread;
	
	/**
	 * Konstruktor za GameFrame, koji se mora pozvati iz naslijeÄ‘enih klasa
	 * @param title naslov prozora
	 * @param sizeX Å¡irina u pikselima
	 * @param sizeY visina u pikselima
	 */
	public GameFrame(String title, int sizeX, int sizeY)
	{
		super();
		
		if(sizeX < 320) sizeX = 320;
		if(sizeY < 240) sizeY = 240;
		
		if(sizeX > 2048) sizeX = 2048;
		if(sizeY > 2048) sizeY = 2048;
		
		screenX = sizeX;
		screenY = sizeY;
		
		setSize(screenX, screenY);
		
		if(title != null) this.title = title;
		
		this.title = title;
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		
		stopThread = false;
		
		
		//runnerThread.start();
	}
	
	/**
	 * PoÄ�etak rada glavnog threada koji poziva update() i render() metoda, mora
	 * se pozvati kako bi aplikacija poÄ�ela sa radom, najbolje na kraju naslijeÄ‘enog
	 * konstruktora, nakon Å¡to se svi resursi uÄ�itaju i pripreme.
	 */
	public void startThread()
	{
		if (runnerThread != null && runnerThread.isAlive()) {
			try {
				runnerThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		runnerThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(buffStrategy == null)
				{
					try
					{
						Thread.sleep(100);
					} 
					catch (InterruptedException e)
					{}
				}
				
				while(true)
				{
					if (stopThread) {
						break;
					}
					/*
					long startTime = System.currentTimeMillis();
					if(updatesRunning) tick();
					if(renderRunning) repaint();
					try
					{
						long frameTime = System.currentTimeMillis() - startTime;
						long sleepTime = 1000 / updateRate - frameTime;
						if(sleepTime > 0)
							Thread.sleep(sleepTime);
					} 
					catch (InterruptedException e) { }
					*/
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
			}
		});
		
		if(!runnerThread.isAlive()) 
		{
			runnerThread.start();
			stopThread = false;
		}
		else 
		{
			System.out.println("Already running!");
		}
	}
	
	private void tick()
	{
		update();
	}
	
	/**
	 * Inicijalizacija prozora (JFrame) u kome se nalazi panel igre, potrebno pozvati
	 * nakon zavrÅ¡etka konstruktora.
	 */
	public void initGameWindow(boolean undecorated)
	{
		if(myFrame != null)
		{
			System.out.println("initGameWindow() already called, can't do again");
			return;
		}
		
		myFrame = new JFrame(title);
		myFrame.setUndecorated(undecorated);
		myFrame.setLayout(new BorderLayout());
		
		setPreferredSize(new Dimension(screenX, screenY));
		setMaximumSize(new Dimension(screenX, screenY));
		myFrame.add(this, BorderLayout.CENTER);
		myFrame.setResizable(false);
		myFrame.pack();
		myFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		myFrame.addKeyListener(this);
		
		myFrame.setIgnoreRepaint(true);
		
		handleWindowInit();
		
		this.createBufferStrategy(2);
		buffStrategy = this.getBufferStrategy();
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
		
		myFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	            public void windowClosing(java.awt.event.WindowEvent evt){
	                handleWindowDestroy();
	                System.exit(0);
	            }
	       });
	}
	
    public void doRendering(Graphics g)
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
		
		render((Graphics2D)g, getWidth(), getHeight());
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
		handleMouseDown(mouseX, mouseY, button);	
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
		handleMouseUp(mouseX, mouseY, button);	
	}

	@Override
	public void keyPressed(KeyEvent arg0)
	{
		if(arg0.getKeyCode() >= 0 && arg0.getKeyCode() < 1024)
			keyboardKeys[arg0.getKeyCode()] = true;
		handleKeyDown(arg0.getKeyCode());
	}

	@Override
	public void keyReleased(KeyEvent arg0)
	{
		if(arg0.getKeyCode() >= 0 && arg0.getKeyCode() < 1024)
			keyboardKeys[arg0.getKeyCode()] = false;
		handleKeyUp(arg0.getKeyCode());
	}

	@Override
	public void keyTyped(KeyEvent arg0) {}

	@Override
	public void mouseDragged(MouseEvent arg0) 
	{
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		handleMouseMove(mouseX, mouseY);
	}

	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		handleMouseMove(mouseX, mouseY);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		if(arg0.getWheelRotation() > 0)
		{
			mouseButtons[GFMouseButton.WheelDown.ordinal()] = true;
			mouseX = arg0.getX();
			mouseY = arg0.getY();
			handleMouseDown(mouseX, mouseY, GFMouseButton.WheelDown);
			mouseButtons[GFMouseButton.WheelDown.ordinal()] = false;
		}
		else if(arg0.getWheelRotation() < 0)
		{
			mouseButtons[GFMouseButton.WheelUp.ordinal()] = true;
			mouseX = arg0.getX();
			mouseY = arg0.getY();
			handleMouseDown(mouseX, mouseY, GFMouseButton.WheelUp);
			mouseButtons[GFMouseButton.WheelUp.ordinal()] = false;
		}
	}
	
	/**
	 * Daje trenutni status tipki miÅ¡a 
	 * @param button dugme iz GFMouseButton enuma za koje se traÅ¾i status
	 * @return true ako je pritisnuta, false ako nije
	 */
	protected boolean isMouseButtonDown(GFMouseButton button)
	{
		return mouseButtons[button.ordinal()];
	}
	
	/**
	 * Daje trenutni status tipki tastature 
	 * @param keyCode konstanta iz KeyEvent koja odreÄ‘uje tipku
	 * @return true ako je pritisnuta, false ako nije
	 */
	protected boolean isKeyDown(int keyCode)
	{
		if(keyCode >= 0 && keyCode < 1024)
			return keyboardKeys[keyCode];
		else
			return false;
	}
	
	/**
	 * Daje JFrame objekat trenutnog okvira
	 * @return JFrame igre (konstruisan prilikom initGameWindow poziva)
	 */
	protected JFrame getWindow()
	{
		return myFrame;
	}
	
	/**
	 * Pali ili gase automatsko brisanje pozadine prije render() metode
	 * @param clr da li treba raditi clear
	 */
	protected void setBackgroundClear(boolean clr)
	{
		clearBackBuffer = clr;
	}
	
	/**
	 * Postavlja boju na koju Ä‡e pozadina biti postavljena, ako je ukljuÄ�en setBackgroundClear
	 * @param c boja
	 */
	protected void setBackgroundClearColor(Color c)
	{
		backColor = c;
	}
	
	/**
	 * Trenutna X koordinata miÅ¡a, u prostoru panela za crtanje
	 * @return X koordinata
	 */
	protected int getMouseX()
	{
		return mouseX;
	}
	
	/**
	 * Trenutna Y koordinata miÅ¡a, u prostoru panela za crtanje
	 * @return Y koordinata
	 */
	protected int getMouseY()
	{
		return mouseY;
	}
	
	/**
	 * Postavlja hint za viÅ¡i kvalitet iscrtavanja koji Ä‡e se onda automatski primjenjivati
	 * nad Graphics2D objektom koji se daje u render() metodi
	 * @param hq true za viÅ¡i kvalitet interpolacije i ukljuÄ�en anti-aliasing primitiva
	 */
	protected void setHighQuality(boolean hq)
	{
		useHQ = hq;
	}
	
	/**
	 * Postavlja ikonicu prozora
	 * @param icon Image objekat (moÅ¾e biti BufferedImage)
	 */
	protected void setIcon(Image icon)
	{
		myFrame.setIconImage(icon);
	}
	
	/**
	 * Postavlja ciljnu frekvenciju aÅ¾uriranja u Hz/fps. Tajming je realizovan jednostavnim
	 * sleep metodama, zbog Ä�ega je moguÄ‡ neravnomijeran tok izvrÅ¡avanja (judder). Ukoliko
	 * aÅ¾uriranje i iscrtavanje traje duÅ¾e od (1 / fps) sekundi, svukupan tempo izvrÅ¡avanja
	 * Ä‡e se usporiti na tu brzinu, nije implementiran nikakav dinamiÄ�ki update ili frameskipping.  
	 * @param fps ciljni broj aÅ¾uriranih i iscrtanih okvira u sekundi, od 1 do 120
	 */
	protected void setUpdateRate(int fps)
	{
		if(fps >= 1 && fps < 120)
		{
			updateRate = fps;
		}
		else
		{
			System.out.println("Valid range for setUpdateRate is 1 - 120");
		}
	}
	
	/**
	 * Metod Ä‡e biti pozvan prilikom initGameWindow() poziva, nakon Å¡to je prozor konstruisan.
	 */
	public abstract void handleWindowInit();
	
	/**
	 * Poziva se prilikom gaÅ¡enja prozora (ako je korisnik kliknuo na X)
	 */
	public abstract void handleWindowDestroy();
	
	/**
	 * Metod koji treba da obavi kompletno iscrtavanje cijelog frejma, poziva se automatski,
	 * zadatom frekvencijom (update rate). Ne treba da sadrÅ¾i nikakva logiÄ�ka aÅ¾uriranja, samo crtanje.
	 * @param g Graphics2D objekat preko koga se obavlja crtanje na ekran
	 * @param sw Å¡irina trenutnog prostora za crtanje
	 * @param sh visina trenutnog prostora za crtanje
	 */
	public abstract void render(Graphics2D g, int sw, int sh);
	
	/**
	 * Metod koji treba da aÅ¾urira stanje igre, poziva se prije render() poziva, jednakom frekvencijom.
	 */
	public abstract void update();
	
	/**
	 * Metod koji Ä‡e biti pozvan na pritisak tastera miÅ¡a (okretanja scroll toÄ�ka se takoÄ‘e smatraju tasterima) 
	 * @param x X koordinata u pikselima na kojima je kursor bio u trenutku pritiska
	 * @param y Y koordinata u pikselima na kojima je kursor bio u trenutku pritiska
	 * @param button dugme miÅ¡a koje je pritisnuto, iz GFMouseButton enuma
	 */
	public abstract void handleMouseDown(int x, int y, GFMouseButton button);
	
	/**
	 * Metod koji Ä‡e biti pozvan na puÅ¡tanje tastera miÅ¡a (okretanja scroll toÄ�ka se takoÄ‘e smatraju tasterima) 
	 * @param x X koordinata u pikselima na kojima je kursor bio u trenutku puÅ¡tanja
	 * @param y Y koordinata u pikselima na kojima je kursor bio u trenutku puÅ¡tanja
	 * @param button dugme miÅ¡a koje je puÅ¡teno, iz GFMouseButton enuma
	 */
	public abstract void handleMouseUp(int x, int y, GFMouseButton button);
	
	/**
	 * Metod koji se poziva pri svakoj promjeni pozicije kursora miÅ¡a, bez obzira da li su tasteri pritisnuti.
	 * @param x X koordinata kursora u pikselima
	 * @param y Y koordinata kursora u pikselima
	 */
	public abstract void handleMouseMove(int x, int y);
	
	/**
	 * Metod koji se poziva kada je pritisnut taster na tastaturi.
	 * @param keyCode kod tipke koja je pritisnuta, porediti sa vrijednostima iz KeyEvent.VK_*
	 */
	public abstract void handleKeyDown(int keyCode);
	
	/**
	 * Metod koji se poziva kada je puÅ¡teni taster na tastaturi.
	 * @param keyCode kod tipke koja je puÅ¡tena, porediti sa vrijednostima iz KeyEvent.VK_*
	 */
	public abstract void handleKeyUp(int keyCode);
}
