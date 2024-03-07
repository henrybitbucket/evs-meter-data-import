package com.pa.evs.sv;

public interface NotificationService {

	String sendEmail(String message, String email, String subject);
	
	String sendSMS(String message, String phoneNumber);
}
