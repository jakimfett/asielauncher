package pl.asiekierka.AsieLauncher;

public abstract class Authentication {
	protected String realUsername, sessionID, error;
	
	public Authentication() {
	}

	public abstract boolean authenticate(String username, String password);
	
	public String getUsername() { return realUsername; }
	public String getSessionID() { return sessionID; }
	public String getErrorMessage() { return error; }
}
