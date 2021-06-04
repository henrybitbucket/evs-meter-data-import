/**
 * 
 */
package com.pa.evs.dto;

/**
 *
 */
public class LoginReq extends ApiRequest {
	private String username;
	private String password;
	private String lang;

	/**
	 * Constructor
	 */
	public LoginReq() {
	}
	
	public LoginReq(String username, String password, String lang) {
		super();
		this.username = username;
		this.password = password;
		this.lang = lang;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * @param lang
	 *            the lang to set
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}
}
