package com.pa.evs.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pa.evs.sv.AuthenticationService;

@Component
public class DMSAccessPermissionJobs {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DMSAccessPermissionJobs.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	
	@Value("${spring.profiles.active}")
	private String activatedProfile;
	
	@Autowired
	AuthenticationService authenticationService;
	
    @Scheduled(fixedRate = 1*60*1000)
    public void getEmails() {
    	if ("prod".equalsIgnoreCase(activatedProfile)) {
    		try {
                LOG.debug("Start getting emails at: {}", dateFormat.format(new Date()));
                authenticationService.accessEmailAndProcessEmail();
                LOG.debug("End getting emails at: {}", dateFormat.format(new Date()));
            } catch (Exception e) {
            	LOG.error("Error on getting emails: {}", e.getMessage());
            }
    	}
    }
    
    @Scheduled(fixedRate = 10*60*1000)
    public void createAccessPermission() {
    	if ("prod".equalsIgnoreCase(activatedProfile)) {
    		try {
                LOG.debug("Start creating access permisison at: {}", dateFormat.format(new Date()));
                authenticationService.processToCreateAccessPermission();
                LOG.debug("End creating access permisison at: {}", dateFormat.format(new Date()));
            } catch (Exception e) {
            	LOG.error("Error on creating access permisison: {}", e.getMessage());
            }	
    	}
        
    }
}
