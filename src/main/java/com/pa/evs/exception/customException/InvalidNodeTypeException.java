package com.pa.evs.exception.customException;

public class InvalidNodeTypeException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6339147823164867275L;

	public InvalidNodeTypeException(String message){
        super(message);
    }
}
