package com.pa.evs.ctrl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.sv.AppEventLogService;

@RestController
public class ShutdownController implements ApplicationContextAware {
    
    private ApplicationContext context;
    
	@Autowired
	AppEventLogService appEventLogService;
    
    @PostMapping("/shutdownContext")
    public void shutdownContext() {
        ((ConfigurableApplicationContext) context).close();
        System.exit(0);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.context = ctx;
        
    }

    @PostConstruct
    public void onStart() {
    	appEventLogService.saveStartTime();
    }
    
    @PreDestroy
    public void onStop() {
    	appEventLogService.saveStopTime();;
    }
}
