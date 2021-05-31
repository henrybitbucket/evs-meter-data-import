package com.pa.evs.exception.customException;


public class NotFoundWorkflowException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2250087281062028962L;

	public NotFoundWorkflowException(String message){
        super(message);
    }
}
