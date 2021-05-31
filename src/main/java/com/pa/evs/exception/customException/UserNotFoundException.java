package com.pa.evs.exception.customException;

public class UserNotFoundException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6356405804474090925L;

	public UserNotFoundException(String message){
        super(message);
    }
}
