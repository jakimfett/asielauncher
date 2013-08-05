package org.smbarbour.mcu;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.ImageIcon;

import net.minecraft.Launcher;

public class MinecraftFrame extends Frame implements WindowListener {

	private static final long serialVersionUID = -2949740978305392280L;

	private Launcher applet = null;

	public static void main(String[] args) {
		String username = args[0];
		String sessionid = args[1];
		String titleName = args[2];
		File instPath = new File(args[3]);
		File lwjglPath = new File(args[4]);
		int width = Integer.parseInt(args[5]);
		int height = Integer.parseInt(args[6]);
		String serverAddress = args[7];
		boolean doConnect = Boolean.parseBoolean(args[8]);
		ImageIcon icon = new ImageIcon(Launcher.class.getResource("/resources/icon.png"));
		MinecraftFrame me = new MinecraftFrame(titleName, icon);
		Dimension windowSize = new Dimension(width, height); //new Dimension(1280, 720)
		me.launch(instPath, lwjglPath, username, sessionid, serverAddress, windowSize, doConnect);
	}
	
	public MinecraftFrame(String title, ImageIcon icon) {
		super(title);
		Image source = icon.getImage();
		int w = source.getWidth(null);
		int h = source.getHeight(null);
		if (w == -1) { w = 32; h = 32; }
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.drawImage(source, 0, 0, null);
		setIconImage(image);
		this.addWindowListener(this);
		g2d.dispose();
	}
	
	public void launch(File instance, File lwjgl, String username, String sessionid, String serverAddress, Dimension windowSize, boolean doConnect) {
		try {
			URI address;
			String host = "";
			String port = "";
			if(!serverAddress.equals("")) {
				address = new URI("my://" + serverAddress);
				if (address.getPort() != -1) {
					port = Integer.toString(address.getPort());
				} else {
					port = Integer.toString(25565);
				}
				host = address.getHost();
			}
			applet = new Launcher(instance, lwjgl, username, sessionid, host, port, doConnect);
			System.setProperty("minecraft.applet.TargetDirectory", instance.toString());
			System.setProperty("org.lwjgl.librarypath", new File(lwjgl, "natives").getAbsolutePath());
			System.setProperty("net.java.games.input.librarypath", new File(lwjgl, "natives").getAbsolutePath());
			this.add(applet);
			applet.setPreferredSize(windowSize);
			this.pack();
			this.setLocationRelativeTo(null);
			this.setResizable(true);
			validate();
			applet.init();
			applet.start();
			setVisible(true);
						
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}
	
	public boolean isAppletActive() {
		return applet.isActive();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(30000L);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
				System.out.println("~~~ Forcing exit! ~~~");
				System.exit(0);
			}
		}.start();
		
		if (applet != null)
		{
			applet.stop();
			applet.destroy();
		}
		System.exit(0);
	}

	@Override
	public void windowOpened(WindowEvent e) {}
	@Override
	public void windowActivated(WindowEvent e) {}
	@Override
	public void windowClosed(WindowEvent e) {}
	@Override
	public void windowIconified(WindowEvent e) {}
	@Override
	public void windowDeiconified(WindowEvent e) {}
	@Override
	public void windowDeactivated(WindowEvent e) {}

}
