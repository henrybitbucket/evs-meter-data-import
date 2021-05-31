package com.pa.evs.utils;

import com.pa.evs.dto.ResponseDto;


public interface ApiResponse {
    @SuppressWarnings("rawtypes")
	public <T> ResponseDto response(String message, boolean success, T response);
}
