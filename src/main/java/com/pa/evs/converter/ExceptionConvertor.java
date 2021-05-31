package com.pa.evs.converter;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;

@Component
public class ExceptionConvertor {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionConvertor.class);
	
    public ResponseDto<?> createResponseDto(Exception ex) {
    	
    	
    	if (ex instanceof ApiException && ((ApiException)ex).isOther()) {
    		return ResponseDto.<Object>builder().success(false)
    				.errorCode(ResponseEnum.BAD_REQUEST.getErrorCode())
                    .errorDescription(ex.getMessage()).build();
    	}
    	
    	logger.error(ex.getMessage(), ex);
        ResponseEnum responseEnum = getResponseEnum(ex.getMessage());
        return ResponseDto.<Object>builder().success(false).errorCode(responseEnum.getErrorCode())
                .errorDescription(responseEnum.getErrorDescription()).build();
    }

    private ResponseEnum getResponseEnum(String message) {
        if (message != null && EnumUtils.isValidEnum(ResponseEnum.class, message.toUpperCase())) {
            return ResponseEnum.valueOf(message);
        }
        return ResponseEnum.SYSTEM_ERROR;
    }
}
