package rafgfxlib;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

public class ImageViewer extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, ComponentListener, KeyListener {
	private static final long serialVersionUID = 7293205001517627801L;
	
	private BufferedImage[] imageList = null;
	private int imgPos = 0;
	
	private BufferedImage img = null;
	private BufferedImage img2 = null;
	private int compMode = 0;
	private int realWidth = 0;
	private int realHeight = 0;
	private int width = 0;
	private int height = 0;
	private int offsetX = 0;
	private int offsetY = 0;
	private int dragStartX = 0;
	private int dragStartY = 0;
	private int offsetStartX = 0;
	private int offsetStartY = 0;
	private int zoomLevel = 0;
	
	private int lastMX = 0;
	private int lastMY = 0;
	
	private boolean dragging = false;
	
	private int multi = 1;
	private int div = 1;
	
	private JFrame myWindow = null;
	private boolean colorPicked = false;
	private boolean altPicked = false;
	private int mouseX = 0, mouseY = 0;
	private int[] pickedColor = new int[3];
	private Font pickFont = new Font(Font.MONOSPACED, Font.BOLD, 16);
	
	public static JFrame constructViewer(BufferedImage imageA, BufferedImage imageB, String title)
	{
		if(title == null) title = "Image viewer";
		
		JFrame window = new JFrame(title);
		window.setResizable(true);
		ImageViewer viewer = new ImageViewer(imageA, imageB);
		window.add(viewer);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addKeyListener(viewer);
		return window;
	}
	
	public static JFrame constructViewer(BufferedImage[] imageList, String title)
	{
		if(title == null) title = "Image viewer";
		
		JFrame window = new JFrame(title);
		window.setResizable(true);
		ImageViewer viewer = new ImageViewer(imageList, window);
		window.add(viewer);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addKeyListener(viewer);
		return window;
	}
	
	public static JDialog constructViewerDialog(BufferedImage imageA, BufferedImage imageB, String title, Window owner)
	{
		if(title == null) title = "Image viewer";
		
		JDialog window = new JDialog(owner, title);
		window.setResizable(true);
		window.setModal(true);
		ImageViewer viewer = new ImageViewer(imageA, imageB);
		window.add(viewer);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addKeyListener(viewer);
		return window;
	}
	
	public static void showImageDlg(BufferedImage image, String title)
	{
		JDialog dlg = constructViewerDialog(image, null, title, null);
		dlg.setSize(800, 600);
		dlg.setVisible(true);
	}
	
	public static void showImageDlg(BufferedImage imageA, BufferedImage imageB, String title)
	{
		JDialog dlg = constructViewerDialog(imageA, imageB, title, null);
		dlg.setSize(800, 600);
		dlg.setVisible(true);
	}
	
	public static void showImageWindow(BufferedImage image, String title)
	{
		JFrame win = constructViewer(image, null, title);
		win.setSize(800, 600);
		win.setVisible(true);
	}
	
	public static void showImageWindow(BufferedImage[] imageList, String title)
	{
		JFrame win = constructViewer(imageList, title);
		win.setSize(800, 600);
		win.setVisible(true);
	}
	
	public static void showImageWindow(BufferedImage imageA, BufferedImage imageB, String title)
	{
		if(imageA == null) return;
		JFrame win = constructViewer(imageA, imageB, title);
		win.setSize(800, 600);
		win.setVisible(true);
	}
	
	public ImageViewer(BufferedImage img)
	{
		super(true);
		
		this.img = img;
		
		width = realWidth = img.getWidth();
		height = realHeight = img.getHeight();
		
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addComponentListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
	}
	
	public ImageViewer(BufferedImage[] imageList, JFrame parent)
	{
		super(true);
		
		this.img = imageList[0];
		imgPos = 0;
		this.imageList = imageList;
		this.myWindow = parent;
		
		width = realWidth = img.getWidth();
		height = realHeight = img.getHeight();
		
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addComponentListener(this);
		this.addMouseWheelListener(this);
		if(myWindow != null) myWindow.addKeyListener(this);
	}
	
	public ImageViewer(BufferedImage imgA, BufferedImage imgB)
	{
		super(true);
		
		this.img = imgA;
		this.img2 = imgB;
		
		width = realWidth = img.getWidth();
		height = realHeight = img.getHeight();
		
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addComponentListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
	}
	
	private void drawOutlined(Graphics2D g, int x, int y, String text)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x - 1, y - 1);
		g.drawString(text, x + 1, y - 1);
		g.drawString(text, x - 1, y + 1);
		g.drawString(text, x + 1, y + 1);
		
		g.drawString(text, x - 1, y);
		g.drawString(text, x + 1, y);
		g.drawString(text, x, y - 1);
		g.drawString(text, x, y + 1);
		
		g.setColor(Color.WHITE);
		g.drawString(text, x, y);
	}
	
	private void drawColorInfo(Graphics2D g, BufferedImage img, int posX, int posY)
	{
		int x = ((mouseX - offsetX) * div) / multi;
		int y = ((mouseY - offsetY) * div) / multi;
		
		if(x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight())
		{
			img.getRaster().getPixel(x, y, pickedColor);
			Color c = new Color(pickedColor[0], pickedColor[1], pickedColor[2]);
			
			g.setColor(Color.BLACK);
			g.fillRect(posX + 0, posY + 0, 198, 60);
			
			g.setColor(c);
			g.fillRect(posX + 0, posY + 0, 70, 60);
			
			g.setColor(Color.RED);
			g.fillRect(posX + 70, posY + 0, pickedColor[0] / 2, 20);
			g.setColor(Color.GREEN);
			g.fillRect(posX + 70, posY + 20, pickedColor[1] / 2, 20);
			g.setColor(Color.BLUE);
			g.fillRect(posX + 70, posY + 40, pickedColor[2] / 2, 20);
			
			g.setFont(pickFont);
			
			drawOutlined(g, posX + 74, posY + 16, "R:" + pickedColor[0]);
			drawOutlined(g, posX + 74, posY + 36, "G:" + pickedColor[1]);
			drawOutlined(g, posX + 74, posY + 56, "B:" + pickedColor[2]);
			
			drawOutlined(g, posX + 4, posY + 16, "X:" + x);
			drawOutlined(g, posX + 4, posY + 36, "Y:" + y);
			
			g.setColor(Color.WHITE);
			g.drawRect(posX - 1, posY - 1, 198, 61);
			g.drawLine(posX + 70, posY + 0, 70, 60);
		}
	}
	
	@Override
	public void paint(Graphics gg)
	{
		Graphics2D g = (Graphics2D)gg;
		g.setBackground(Color.black);
		g.clearRect(0, 0, getWidth(), getHeight());
		
		if(compMode == 0)
		{
			if(multi == 1 && div == 1)
				g.drawImage(img, offsetX, offsetY, null);
			else
				g.drawImage(img, offsetX, offsetY, width, height, null);
			
			if(colorPicked) drawColorInfo(g, img, 0, 0);
			if(altPicked && img2 != null) drawColorInfo(g, img2, 0, 60);
		}
		else if(compMode == 1)
		{
			g.setClip(0, 0, lastMX, getHeight());
			if(multi == 1 && div == 1)
				g.drawImage(img, offsetX, offsetY, null);
			else
				g.drawImage(img, offsetX, offsetY, width, height, null);
			
			g.setClip(lastMX, 0, getWidth(), getHeight());
			if(multi == 1 && div == 1)
				g.drawImage(img2, offsetX, offsetY, null);
			else
				g.drawImage(img2, offsetX, offsetY, width, height, null);
			
			g.setClip(0, 0, getWidth(), getHeight());
		}
		else if(compMode == 2)
		{
			g.setClip(0, 0, getWidth(), lastMY);
			if(multi == 1 && div == 1)
				g.drawImage(img, offsetX, offsetY, null);
			else
				g.drawImage(img, offsetX, offsetY, width, height, null);
			
			g.setClip(0, lastMY, getWidth(), getHeight());
			if(multi == 1 && div == 1)
				g.drawImage(img2, offsetX, offsetY, null);
			else
				g.drawImage(img2, offsetX, offsetY, width, height, null);
			
			g.setClip(0, 0, getWidth(), getHeight());
		}
		else if(compMode == 3)
		{
			if(multi == 1 && div == 1)
				g.drawImage(img2, offsetX, offsetY, null);
			else
				g.drawImage(img2, offsetX, offsetY, width, height, null);
			
			if(colorPicked) drawColorInfo(g, img2, 0, 0);
			if(altPicked && img != null) drawColorInfo(g, img, 0, 60);
		}
		
		if(multi != 1 || div != 1 || imageList != null)
		{
			if(colorPicked)
			{
				//drawColorInfo(g, img);
			}
			else
			{
				g.setColor(new Color(0, 0, 0, 128));
				g.fillRect(25, 25, 75, 25);
				g.setColor(Color.white);
				
				if(imageList != null)
				{
					g.drawString("[" + (imgPos + 1) + "/" + imageList.length + "] " + (100 * multi) / div + "%", 30, 42);
				}
				else
				{
					g.drawString((100 * multi) / div + "%", 50, 42);
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) 
	{
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		if(dragging)
		{
			offsetX = offsetStartX + (arg0.getX() - dragStartX);
			offsetY = offsetStartY + (arg0.getY() - dragStartY);
			
			checkBounds();
			
			repaint();
		}
		
		lastMX = arg0.getX();
		lastMY = arg0.getY();
	}
	
	private void checkBounds()
	{
		if(getWidth() > width + 20)
		{
			offsetX = getWidth() / 2 - width / 2;
		}
		else
		{
			if(offsetX > 10) offsetX = 10;
			if(offsetX < (getWidth() - width - 10)) offsetX = (getWidth() - width - 10);
		}
		
		if(getHeight() > height + 20)
		{
			offsetY = getHeight() / 2 - height / 2;
		}
		else
		{
			if(offsetY > 10) offsetY = 10;
			if(offsetY < (getHeight() - height - 10)) offsetY = (getHeight() - height - 10);
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		if(dragging)
		{
			offsetX = offsetStartX + (arg0.getX() - dragStartX);
			offsetY = offsetStartY + (arg0.getY() - dragStartY);
			
			checkBounds();
			
			repaint();
		}
		
		lastMX = arg0.getX();
		lastMY = arg0.getY();
		
		if(compMode == 1 || compMode == 2 || colorPicked)
			repaint();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		/*
		if(arg0.getButton() == MouseEvent.BUTTON3)
		{
			if(img2 != null)
			{
				compMode = (compMode + 1) % 4;
				repaint();
			}
		}
		*/
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		//System.out.println("Mouse pressed! " + arg0.getButton());
		if(arg0.getButton() == MouseEvent.BUTTON1)
		{
			dragging = true;
			dragStartX = arg0.getX();
			dragStartY = arg0.getY();
			offsetStartX = offsetX;
			offsetStartY = offsetY;
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		
		if(arg0.getButton() == MouseEvent.BUTTON1)
		{
			dragging = false;
			
			if(colorPicked && (compMode == 0 || compMode == 3))
			{
				BufferedImage mainImage = (compMode == 0) ? img : img2;
				BufferedImage altImage = (compMode == 0) ? img2 : img;
				
				if(altPicked && altImage != null)
				{
					int x = ((mouseX - offsetX) * div) / multi;
					int y = ((mouseY - offsetY) * div) / multi;
					
					if(x >= 0 && x < altImage.getWidth() && y >= 0 && y < altImage.getHeight())
					{
						altImage.getRaster().getPixel(x, y, pickedColor);
						String colorString = String.format("#%02x%02x%02x", pickedColor[0], pickedColor[1], pickedColor[2]);
						StringSelection stringSelection = new StringSelection(colorString);
						Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
						clpbrd.setContents(stringSelection, null);
					}
				}
				else
				{
					int x = ((mouseX - offsetX) * div) / multi;
					int y = ((mouseY - offsetY) * div) / multi;
					
					if(x >= 0 && x < mainImage.getWidth() && y >= 0 && y < mainImage.getHeight())
					{
						mainImage.getRaster().getPixel(x, y, pickedColor);
						String colorString = String.format("#%02x%02x%02x", pickedColor[0], pickedColor[1], pickedColor[2]);
						StringSelection stringSelection = new StringSelection(colorString);
						Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
						clpbrd.setContents(stringSelection, null);
					}
				}
			}
		}	
		
		if(arg0.getButton() == MouseEvent.BUTTON3)
		{
			if(img2 != null)
			{
				compMode = (compMode + 1) % 4;
				repaint();
				
				//System.out.println("Comp mode = " + compMode);
			}
		}
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentResized(ComponentEvent e) {
		
		checkBounds();
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	private void updateZoomLevel(int lvl)
	{
		if(lvl < -3) lvl = -3;
		if(lvl > 3) lvl = 3;
		
		if(lvl == zoomLevel) return;
		zoomLevel = lvl;
		
		int imageSpaceX = (((lastMX - offsetX) * div) / multi);
		int imageSpaceY = (((lastMY - offsetY) * div) / multi);
		
		switch(lvl)
		{
		case -3:
			multi = 1;
			div = 8;
			break;
			
		case -2:
			multi = 1;
			div = 4;
			break;
			
		case -1:
			multi = 1;
			div = 2;
			break;
			
		case 0:
			multi = 1;
			div = 1;
			break;
			
		case 1:
			multi = 2;
			div = 1;
			break;
			
		case 2:
			multi = 4;
			div = 1;
			break;
			
		case 3:
			multi = 8;
			div = 1;
			break;
		}
		
		width = (realWidth * multi) / div;
		height = (realHeight * multi) / div;
		
		offsetX = lastMX - ((imageSpaceX * multi) / div);
		offsetY = lastMY - ((imageSpaceY * multi) / div);
		
		checkBounds();
		repaint();
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		if(arg0.getWheelRotation() > 0)
		{
			//System.out.println("Positive");
			updateZoomLevel(zoomLevel - 1);
		}
		else
		{
			//System.out.println("Negative");
			updateZoomLevel(zoomLevel + 1);
		}
		
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			colorPicked = true;
			repaint();
		}
		
		if(e.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			altPicked = true;
			if(colorPicked) repaint();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		//System.out.println("Key rel! " + e.getKeyCode());
		if(imageList != null)
		{
			if(e.getKeyCode() == KeyEvent.VK_LEFT)
			{
				imgPos--;
				if(imgPos < 0)
					imgPos = imageList.length - 1;
				img = imageList[imgPos];
				realWidth = img.getWidth();
				realHeight = img.getHeight();
				width = (realWidth * multi) / div;
				height = (realHeight * multi) / div;
				checkBounds();
				repaint();
			}
			else if(e.getKeyCode() == KeyEvent.VK_RIGHT)
			{
				imgPos++;
				if(imgPos >= imageList.length)
					imgPos = 0;
				img = imageList[imgPos];
				realWidth = img.getWidth();
				realHeight = img.getHeight();
				width = (realWidth * multi) / div;
				height = (realHeight * multi) / div;
				checkBounds();
				repaint();
			}
		}
		
		if(e.getKeyCode() == KeyEvent.VK_S)
		{
			JFileChooser fileChooser = new JFileChooser();
			
			fileChooser.setMultiSelectionEnabled(false);
			
			fileChooser.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return "Supported image formats (JPEG, PNG, BMP, GIF)";
				}
				
				@Override
				public boolean accept(File f) {
					String lowerName = f.getName().toLowerCase();
					
					return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
							lowerName.endsWith(".png") || lowerName.endsWith(".bmp") ||
							lowerName.endsWith(".gif") || f.isDirectory();
				}
			});
			
			int dlgResult = fileChooser.showDialog(myWindow, "Save image");
			
			if(dlgResult == JFileChooser.APPROVE_OPTION)
			{
				if(!Util.saveImage(img, fileChooser.getSelectedFile().getAbsolutePath()))
				{
					JOptionPane.showMessageDialog(myWindow, "Error saving image.", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
						
			}
		}
		
		if(e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			colorPicked = false;
			repaint();
		}
		
		if(e.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			altPicked = false;
			if(colorPicked) repaint();
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// TODO Auto-generated method stub
		
	}

}
