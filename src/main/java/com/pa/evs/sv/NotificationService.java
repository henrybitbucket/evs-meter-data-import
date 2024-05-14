package com.pa.evs.sv;

import java.util.Map;

public interface NotificationService {

	String sendEmail(String message, String email, String subject);
	
	String sendSMS(String message, String phoneNumber);

	void sendSMS(Map<String, Object> payload);
}
