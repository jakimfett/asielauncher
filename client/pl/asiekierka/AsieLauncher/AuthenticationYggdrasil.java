package pl.asiekierka.AsieLauncher;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.*;
import org.json.simple.parser.*;

public class AuthenticationYggdrasil extends Authentication {
	// Special thanks to the MinecraftCoalition.
	
	private static final String SERVER = "https://authserver.mojang.com";
	private boolean triedRenewSessionID, keepLoggedIn;
	private String directory, clientToken, profileID;

	public AuthenticationYggdrasil(String _directory, boolean _keepLoggedIn) {
		directory = _directory;
		triedRenewSessionID = false;
		keepLoggedIn = _keepLoggedIn;
		clientToken = Utils.loadStringFromFile(directory + "clienttoken.txt");
		if(clientToken == null || clientToken.length() == 0) {
			// Generate client token
			clientToken = UUID.randomUUID().toString();
			Utils.saveStringToFile(directory + "clienttoken.txt", clientToken);
		}
	}
	
	private JSONObject sendJSONPayload(String path, JSONObject payload) {
		try {
			URL url = new URL(SERVER + path);
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			dos.writeBytes(payload.toJSONString());
			dos.flush();
			dos.close();
			String sAnswer = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			inputLine = in.readLine();
			while(inputLine != null) {
				sAnswer += inputLine;
				inputLine = in.readLine();
			}
			in.close();
			return (JSONObject)new JSONParser().parse(sAnswer);
		}
		catch(Exception e) {
			 e.printStackTrace(); error = "Internal AsieLauncher error"; return null;
		}
	}
	
	private boolean ifErrorThenSet(JSONObject errorJSON) {
		if(!errorJSON.containsKey("error")) return false;
		if(errorJSON.containsKey("errorMessage")) error = (String)errorJSON.get("errorMessage");
		else error = (String)errorJSON.get("error");
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private boolean renewSessionID() {
		triedRenewSessionID = true;
		sessionID = Utils.loadStringFromFile(directory + "sessionid.txt");
		if(sessionID == null || sessionID.length() == 0) { // No Session ID stored!
			return false;
		}
		// Session ID is stored, attempt renewing.
		JSONObject payload = new JSONObject();
		payload.put("accessToken", sessionID);
		payload.put("clientToken", clientToken);
		JSONObject answer = sendJSONPayload("/refresh", payload);
		if(ifErrorThenSet(answer)) return false;
		sessionID = (String)answer.get("accessToken");
		if(keepLoggedIn) saveSessionID();
		return true;
	}
	
	private void saveSessionID() {
		Utils.saveStringToFile(directory + "sessionid.txt", sessionID);
	}
	
	@Override
	public String getSessionID() {
		return "token:"+sessionID+":"+profileID;
	}
	
	@Override
	public boolean requiresPassword() {
		if(!triedRenewSessionID) return !renewSessionID();
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean authenticate(String username, String password) {
		if(triedRenewSessionID & sessionID != null && sessionID.length() > 0) {
			return true; // Already authenticated, don't bother.
		}
		realUsername = username; // For now.
		JSONObject payload = new JSONObject();
		payload.put("username", username);
		payload.put("password", password);
		payload.put("clientToken", clientToken);
		JSONObject answer = sendJSONPayload("/authenticate", payload);
		if(ifErrorThenSet(answer)) return false;
		sessionID = (String)answer.get("accessToken");
		if(keepLoggedIn) saveSessionID();
		return true;
	}

}
