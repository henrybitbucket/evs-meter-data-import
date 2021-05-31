package com.pa.evs.exception.customException;

public class AuthenticationException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6963520853267769540L;

	public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
