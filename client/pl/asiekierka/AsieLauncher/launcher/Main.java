package pl.asiekierka.AsieLauncher.launcher;

import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import pl.asiekierka.AsieLauncher.launcher.gui.AsieLauncherGUI;

public class Main {
    private static TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
    }};
    
	public static void main(String[] args) {
		// SSL patch
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch(Exception e) { e.printStackTrace(); }
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
				return true;
			}
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		AsieLauncherGUI al = new AsieLauncherGUI();
		System.out.println("Connecting...");
		if(!al.init()) {
			System.out.println("Error connecting!");
			System.exit(1);
		}
		boolean isRunning = true;
		while(al.isRunning && isRunning) {
			int sleepLength = 1000;
			if(!al.getLaunchedMinecraft()){
				al.repaint();
				sleepLength = 100;
			} else { // Launched Minecraft
				al.setVisible(false);
				if(!al.isActive()) {
					isRunning = false;
				}
			}
			try { Thread.sleep(sleepLength); }
			catch(Exception e) { e.printStackTrace(); }
		}
		System.out.println("Shutting down.");
		System.exit(0);
	}
}
