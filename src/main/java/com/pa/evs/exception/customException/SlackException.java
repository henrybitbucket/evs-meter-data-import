package com.pa.evs.exception.customException;

public class SlackException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6813233313745956338L;

	public SlackException(String message){
        super(message);
    }
}
