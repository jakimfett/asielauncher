package pl.asiekierka.AsieLauncher.launcher;

public abstract class Authentication {
	protected String realUsername, sessionID, error;
	
	public Authentication() {
	}

	public abstract boolean authenticate(String username, String password);
	public boolean requiresPassword() {
		return true;
	}
	public String getUsername() { return realUsername; }
	public String getSessionID() { return sessionID; }
	public String getErrorMessage() { return error; }
}
