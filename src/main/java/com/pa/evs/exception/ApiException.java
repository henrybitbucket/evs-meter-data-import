package com.pa.evs.exception;

import com.pa.evs.enums.ResponseEnum;

public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 3184453122619195508L;
	
	private boolean isOther;
    
    public ApiException(ResponseEnum responseEnum) {
        super(responseEnum.name());
    }
    
    public ApiException(String msg) {
    	super(msg);
    	this.isOther = true;
    }

	public boolean isOther() {
		return isOther;
	}

	public void setOther(boolean isOther) {
		this.isOther = isOther;
	}    
}
