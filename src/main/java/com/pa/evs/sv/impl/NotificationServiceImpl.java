package com.pa.evs.sv.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.pa.evs.model.NotificationLog;
import com.pa.evs.repository.NotificationLogRepository;
import com.pa.evs.sv.NotificationService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.ExternalLogger;
import com.pa.evs.utils.SecurityUtils;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
public class NotificationServiceImpl implements NotificationService {

	private static final org.slf4j.Logger LOG = ExternalLogger.getLogger(NotificationServiceImpl.class);
	com.amazonaws.services.simpleemail.AmazonSimpleEmailService sesClient = null;
	software.amazon.awssdk.services.sns.SnsClient snsClient = null;
	
	@Value("${s3.access.id}") private String accessID;

	@Value("${s3.access.key}") private String accessKey;
	
	@Autowired NotificationLogRepository notificationLogRepository;
	
	@Override
	public String sendEmail(String message, String email, String subject) {
		
		try {
			com.amazonaws.services.simpleemail.model.SendEmailRequest request = new com.amazonaws.services.simpleemail.model.SendEmailRequest()
					.withDestination(new com.amazonaws.services.simpleemail.model.Destination().withToAddresses(email))
					.withMessage(new com.amazonaws.services.simpleemail.model.Message()
							.withBody(new com.amazonaws.services.simpleemail.model.Body().withHtml(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(message)))
//			                  .withText(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(TEXTBODY)))
							.withSubject(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(subject)))
			.withSource(AppProps.get("AWS_SES_FROM", "evs2ops@evs.com.sg"))
			;
			String res = sesClient.sendEmail(request).getSdkResponseMetadata().getRequestId();
			AppProps.getContext().getBean(this.getClass()).saveLog(
					NotificationLog.builder()
					.type("EMAIL")
					.content(message)
					.to(email)
					.track(res)
					.build()
					);
			return res;

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void sendSMS(Map<String, Object> payload) {
		String phoneNumber = (String) payload.get("To");
		String message = (String) payload.get("Body");
		String sid = (String) payload.get("SID");
		String createdAt = (String) payload.get("CreatedAt");
		try {
			software.amazon.awssdk.services.sns.model.PublishRequest request = software.amazon.awssdk.services.sns.model.PublishRequest.builder().message(message).phoneNumber(phoneNumber).build();
			software.amazon.awssdk.services.sns.model.PublishResponse result = snsClient.publish(request);
			LOG.info("SMS -> " + phoneNumber + " -> " + result.messageId() + " Message sent. Status was " + result.sdkHttpResponse().statusCode());
			String res = result.messageId();
			AppProps.getContext().getBean(this.getClass()).saveLog(
					NotificationLog.builder()
					.type("SMS")
					.content(message)
					.to(phoneNumber)
					.sid(sid)
					.createdAt(createdAt)
					.track(res)
					.build()
					);
		} catch (SnsException e) {
			LOG.info("SMS -> " + phoneNumber + " -> " + e.awsErrorDetails().errorMessage());
			LOG.error(e.getMessage(), e);
			throw e;
		}		
	}
	
	// https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/sns/src/main/java/com/example/sns/PublishTextSMS.java
	@Override
	public String sendSMS(String message, String phoneNumber) {
		try {
			software.amazon.awssdk.services.sns.model.PublishRequest request = software.amazon.awssdk.services.sns.model.PublishRequest.builder().message(message).phoneNumber(phoneNumber).build();
			software.amazon.awssdk.services.sns.model.PublishResponse result = snsClient.publish(request);
			LOG.info("SMS -> " + phoneNumber + " -> " + result.messageId() + " Message sent. Status was " + result.sdkHttpResponse().statusCode());
			String res = result.messageId();
			AppProps.getContext().getBean(this.getClass()).saveLog(
					NotificationLog.builder()
					.type("SMS")
					.content(message)
					.to(phoneNumber)
					.track(res)
					.build()
					);
			return res;
		} catch (SnsException e) {
			LOG.info("SMS -> " + phoneNumber + " -> " + e.awsErrorDetails().errorMessage());
			LOG.error(e.getMessage(), e);
			throw e;
		}
	}

	@PostConstruct
	public void init() {

		new Thread(() -> {
	        try {
	    		software.amazon.awssdk.auth.credentials.AwsCredentialsProvider a = new software.amazon.awssdk.auth.credentials.AwsCredentialsProvider() {
	    			@Override
	    			public software.amazon.awssdk.auth.credentials.AwsCredentials resolveCredentials() {
	    				return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessID, accessKey);
	    			}};
	    		snsClient = software.amazon.awssdk.services.sns.SnsClient.builder().region(software.amazon.awssdk.regions.Region.AP_SOUTHEAST_1)
	    				.credentialsProvider(a).build();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
	        try {
	        	com.amazonaws.auth.AWSCredentialsProvider a = new com.amazonaws.auth.AWSCredentialsProvider() {
					@Override
					public AWSCredentials getCredentials() {
						return new BasicAWSCredentials(accessID, accessKey);
					}
					@Override
					public void refresh() {
					}
	        	};
	        	sesClient = com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder.standard()
	        			.withCredentials(a)
	        	        .withRegion(com.amazonaws.regions.Regions.AP_SOUTHEAST_1).build();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}).start();
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveLog(NotificationLog log) {
		try {
			log.setCreatedBy(SecurityUtils.getEmail());
			notificationLogRepository.save(log);
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}
	
	// https://d-966757d1f7.awsapps.com/start/#/?tab=accounts henrytang Thn123@456_Kru 
	public static void main(String[] args) {

		args = new String[] {"Test aws sms", "+84933520892"};
		final String usage = "\n" + "Usage: " + "   <message> <phoneNumber>\n\n" + "Where:\n"
				+ "   message - The message text to send.\n\n"
				+ "   phoneNumber - The mobile phone number to which a message is sent (for example, +1XXX5550100). \n\n";

		if (args.length != 2) {
			System.out.println(usage);
			System.exit(1);
		}

		String message = args[0];
		String phoneNumber = args[1];
		software.amazon.awssdk.auth.credentials.AwsCredentialsProvider a = new software.amazon.awssdk.auth.credentials.AwsCredentialsProvider() {
			@Override
			public software.amazon.awssdk.auth.credentials.AwsCredentials resolveCredentials() {
				return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("AKIA5FJTG4T6ZJXTTU5I", "+D8Z177y+t29cybFj4elx4D6l2dUUZyDWXFXPAw7");
			}};
		software.amazon.awssdk.services.sns.SnsClient snsClient = software.amazon.awssdk.services.sns.SnsClient.builder().region(software.amazon.awssdk.regions.Region.AP_SOUTHEAST_1)
				.credentialsProvider(a).build();
		NotificationServiceImpl sv = new NotificationServiceImpl();
		sv.snsClient = snsClient;
		System.out.println(sv.sendSMS(message, phoneNumber));
		snsClient.close();
	}
	
	public static void main1(String[] args) {

		String message = "<html><body>MMS-1234576</body></html>";
		String email = "ttx.pipo.uit@gmail.com";
		com.amazonaws.services.simpleemail.model.SendEmailRequest request = new com.amazonaws.services.simpleemail.model.SendEmailRequest()
				.withDestination(new com.amazonaws.services.simpleemail.model.Destination().withToAddresses(email))
				.withMessage(new com.amazonaws.services.simpleemail.model.Message()
						.withBody(new com.amazonaws.services.simpleemail.model.Body().withHtml(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(message)))
//		                  .withText(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(TEXTBODY)))
						.withSubject(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData("MMS")))
		 .withSource("evs2ops@evs.com.sg")
		;
		
    	com.amazonaws.services.simpleemail.AmazonSimpleEmailService sesClient = com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder.standard()
    			.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("AKIA5FJTG4T6ZJXTTU5I", "+D8Z177y+t29cybFj4elx4D6l2dUUZyDWXFXPAw7")))
    	        .withRegion(com.amazonaws.regions.Regions.AP_SOUTHEAST_1).build();
		sesClient.sendEmail(request).getSdkResponseMetadata().getRequestId();
	}
}
