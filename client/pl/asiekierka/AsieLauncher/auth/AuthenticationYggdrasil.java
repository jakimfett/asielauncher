package pl.asiekierka.AsieLauncher.auth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.*;
import org.json.simple.parser.*;

import pl.asiekierka.AsieLauncher.common.Utils;

public class AuthenticationYggdrasil extends Authentication {
	// Special thanks to the MinecraftCoalition. (wiki.vg)
	
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

	@SuppressWarnings("unchecked")
	public void setKeepPassword(boolean l) {
		if(keepLoggedIn == true && l == false) {
			sessionID = null;
			info.put("sessionID", null);
		}
		keepLoggedIn = l;
	}
	
	private String readIn(BufferedReader in) {
		try {
			String answer = "";
			String inputLine = in.readLine();
			while(inputLine != null) {
				answer += inputLine;
				inputLine = in.readLine();
			}
			in.close();
			return answer;
		} catch(Exception e) { return ""; }
	}
	@SuppressWarnings("unchecked")
	private JSONObject sendJSONPayload(String path, JSONObject payload) {
		HttpsURLConnection con = null;
		try {
			if(!payload.containsKey("agent")) {
				JSONObject agent = new JSONObject();
				agent.put("name", "Minecraft");
				agent.put("version", 1);
				payload.put("agent", agent);
			}
			URL url = new URL(SERVER + path);
			con = (HttpsURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			dos.writeBytes(payload.toJSONString());
			dos.flush();
			dos.close();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			return (JSONObject)new JSONParser().parse(readIn(in));
		}
		catch(IOException ioe) {
			try {
				if(con == null) throw new Exception("Connection not initialized here");
				else {
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
					return (JSONObject)new JSONParser().parse(readIn(in));
				}
			} catch(Exception e) {
				e.printStackTrace(); error = "Internal AsieLauncher error"; return null;
			}
		}
		catch(Exception e) {
			 e.printStackTrace(); error = "Internal AsieLauncher error"; return null;
		}
	}
	
	private int handlingError(JSONObject errorJSON) {
		if(errorJSON == null) {
			error = "Server error!";
			return SERVER_DOWN;
		}
		if(!errorJSON.containsKey("error")) return OK;
		if(errorJSON.containsKey("errorMessage")) error = (String)errorJSON.get("errorMessage");
		else error = (String)errorJSON.get("error");
		return LOGIN_INVALID;
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
		//payload.put("selectedProfile", null);
		JSONObject answer = sendJSONPayload("/refresh", payload);
		if(handlingError(answer) != OK) {
			sessionID = null;
			return false;
		}
		JSONObject profile = (JSONObject)answer.get("selectedProfile");
		realUsername = (String)profile.get("name");
		sessionID = (String)answer.get("accessToken");
		_UUID = (String)profile.get("id");
		if(keepLoggedIn) {
			info.put("sessionID", sessionID);
			save();
		}
		return true;
	}
	
	@Override
	public String getSessionToken() {
		if(!info.containsKey("selectedProfile")) return null;
		JSONObject profile = (JSONObject)info.get("selectedProfile");
		//return "token:"+sessionID+":"+(String)profile.get("id");
        return sessionID;
	}
	
	@Override
	public boolean requiresPassword() {
		if(!triedRenewSessionID) return !renewSessionID();
		else return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int authenticate(String username, String password) {
		if(triedRenewSessionID && sessionID != null && sessionID.length() > 0) {
			return OK; // Already authenticated, don't bother.
		}
		realUsername = username; // For now.
		JSONObject payload = new JSONObject();
		payload.put("username", username);
		payload.put("password", password);
		payload.put("clientToken", get("clientToken"));
		JSONObject answer = sendJSONPayload("/authenticate", payload);
		if(handlingError(answer) != OK) return handlingError(answer);
		JSONObject profile = (JSONObject)answer.get("selectedProfile");
		realUsername = (String)profile.get("name");
		sessionID = (String)answer.get("accessToken");
        _UUID = (String)profile.get("id");
		info.put("selectedProfile", profile);
		if(keepLoggedIn) {
			info.put("sessionID", sessionID);
			save();
		}
		return OK;
	}

}
