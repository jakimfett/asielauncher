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
	private String filename, directory;
	private boolean triedRenewSessionID, keepLoggedIn;
	private JSONObject info;
	
	@SuppressWarnings("unchecked")
	public AuthenticationYggdrasil(String _directory, boolean _keepLoggedIn) {
		directory = _directory;
		triedRenewSessionID = false;
		keepLoggedIn = _keepLoggedIn;
		filename = directory + "yggdrasil.json";
		info = Utils.readJSONFile(filename);
		if(info == null) {
			info = new JSONObject();
		}
		if(get("clientToken") == null) {
			// Generate client token
			info.put("clientToken", UUID.randomUUID().toString());
			save();
		}
	}
	
	private void save() {
		Utils.saveStringToFile(filename, info.toJSONString());
	}
	
	private String get(String name) { return info.containsKey(name) ? (String)info.get(name) : null; }

	public void setKeepPassword(boolean l) {
		keepLoggedIn = l;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject sendJSONPayload(String path, JSONObject payload) {
		try {
			if(!payload.containsKey("agent")) {
				JSONObject agent = new JSONObject();
				agent.put("name", "Minecraft");
				agent.put("version", 1);
				payload.put("agent", agent);
			}
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
	
	private boolean handlingError(JSONObject errorJSON) {
		if(errorJSON == null) {
			error = "Server error!";
			return true;
		}
		if(!errorJSON.containsKey("error")) return false;
		if(errorJSON.containsKey("errorMessage")) error = (String)errorJSON.get("errorMessage");
		else error = (String)errorJSON.get("error");
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private boolean renewSessionID() {
		triedRenewSessionID = true;
		sessionID = get("sessionID");
		if(sessionID == null || !info.containsKey("selectedProfile")) {
			// No Session ID or profile found!
			return false;
		}
		// Session ID is stored, attempt renewing.
		JSONObject payload = new JSONObject();
		payload.put("accessToken", sessionID);
		payload.put("clientToken", get("clientToken"));
		payload.put("selectedProfile", null);
		JSONObject answer = sendJSONPayload("/refresh", payload);
		if(handlingError(answer)) {
			sessionID = null;
			return false;
		}
		JSONObject profile = (JSONObject)answer.get("selectedProfile");
		realUsername = (String)profile.get("name");
		sessionID = (String)answer.get("accessToken");
		if(keepLoggedIn) {
			info.put("sessionID", sessionID);
			save();
		}
		return true;
	}
	
	@Override
	public String getSessionID() {
		return getMojangSessionID();
	}
	
	public String getMojangSessionID() {
		if(!info.containsKey("selectedProfile")) return null;
		JSONObject profile = (JSONObject)info.get("selectedProfile");
		return "token:"+sessionID+":"+(String)profile.get("id");
	}
	
	@Override
	public boolean requiresPassword() {
		if(!triedRenewSessionID) return !renewSessionID();
		else return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean authenticate(String username, String password) {
		if(triedRenewSessionID && sessionID != null && sessionID.length() > 0) {
			return true; // Already authenticated, don't bother.
		}
		realUsername = username; // For now.
		JSONObject payload = new JSONObject();
		payload.put("username", username);
		payload.put("password", password);
		payload.put("clientToken", get("clientToken"));
		JSONObject answer = sendJSONPayload("/authenticate", payload);
		if(handlingError(answer)) return false;
		JSONObject profile = (JSONObject)answer.get("selectedProfile");
		realUsername = (String)profile.get("name");
		sessionID = (String)answer.get("accessToken");
		info.put("selectedProfile", profile);
		if(keepLoggedIn) {
			info.put("sessionID", sessionID);
			save();
		}
		return true;
	}

}
