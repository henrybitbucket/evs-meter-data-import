package com.pa.evs.exception;

public class InvalidException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3184453122619195508L;
    
    public InvalidException(String msg) {
        super(msg);
    }
    
    public InvalidException(String msg, Exception e) {
        super(msg, e);
    }

}
