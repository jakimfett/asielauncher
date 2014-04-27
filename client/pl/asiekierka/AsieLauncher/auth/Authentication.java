package pl.asiekierka.AsieLauncher.auth;

public abstract class Authentication {
	protected String realUsername, sessionID, error, _UUID;
	
	public static final int OK = 0;
	public static final int GENERAL_ERROR = 1;
	public static final int LOGIN_INVALID = 2;
	public static final int SERVER_DOWN = 3;
	
	public Authentication() {
	}

	public abstract int authenticate(String username, String password);
	public boolean requiresPassword() {
		return true;
	}
	public String getUsername() { return realUsername; }
	public String getSessionToken() { return sessionID; }
	public String getErrorMessage() { return error; }
    public String getUUID() {return _UUID;}
}
