package com.pa.evs.exception.customException;

public class DuplicateUserException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5095171623857716158L;

	public DuplicateUserException(String message){
        super(message);
    }
}
