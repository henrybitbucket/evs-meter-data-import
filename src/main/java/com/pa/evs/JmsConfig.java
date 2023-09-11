package com.pa.evs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.sv.impl.EVSPAServiceImpl;
import com.pa.evs.utils.ExternalLogger;

import lombok.extern.log4j.Log4j2;

@Configuration
@EnableJms
@Log4j2
public class JmsConfig {
	
	public static long JMS_READY = -1;
	
	static final ObjectMapper MAPPER = new ObjectMapper();

    @Bean
    org.springframework.context.ApplicationListener<org.springframework.boot.web.context.WebServerInitializedEvent> webServerInitializedListener() {
        return (event) -> {
        	JMS_READY = System.currentTimeMillis();
        };
    }
    
	@Bean
	public ActiveMQConnectionFactory connectionFactory() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL("vm://localhost?broker.persistent=false");
		return connectionFactory;
	}

	@Bean
	public JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(connectionFactory());
		return template;
	}

	@Bean(name = "jmsContainerFactory")
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory());
		factory.setConcurrency("1-5");
		return factory;
	}

	@Component("ajms")
	@org.springframework.core.annotation.Order(value = org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
	@org.springframework.context.annotation.DependsOn({"jmsTemplate", "connectionFactory", "jmsContainerFactory"})
	public static class AJMS {
		
		private static final org.slf4j.Logger TMPLOG = ExternalLogger.getLogger(EVSPAServiceImpl.class);
		private static final Map<String, org.slf4j.Logger> LOGS = new ConcurrentHashMap<>();

		@Value("${testJms:true}")
		String testJms;
		
		@Autowired
		public JmsTemplate jmsTemplate;

		@JmsListener(destination = "logQ", containerFactory = "jmsContainerFactory")
		public void listen(final ActiveMQMapMessage message) throws Exception {
			
			Thread.currentThread().setName("jms-" + Thread.currentThread().getId() + " - " + message.getString("thName"));
			String logger = message.getString("name");
			String format = message.getString("format");
			if (format == null) {
				format = "";
			}
			String arguments = message.getString("arguments");
			if (logger != null) {
				org.slf4j.Logger log = LOGS.computeIfAbsent(logger, org.slf4j.LoggerFactory::getLogger);
				int noOfArgFormats = format.split("\\{\\}").length - 1;
				Object[] arr = arguments != null ? arguments.split("<<<log4j2>>>") : new String[0];
				for (int i = 0; i < arr.length - noOfArgFormats; i++) {
					format = format + "{} ";
				}
				switch (message.getString("type")) {
				case "info":
					log.info(format, arr);
					break;
				case "debug":
					log.debug(format, arr);
					break;
				case "error":
					 log.error(format, arr);
					break;					
				default:
					break;
				}
			}
		}

		// @PostConstruct
		public void init() {
			log.info("Starting... jms");
			if (!"true".equalsIgnoreCase(testJms)) {
				return;
			}
			log.info("Sending... jms");
			new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(3000l);
					} catch (InterruptedException e) {
						log.error(e.getMessage(), e);
					}
					TMPLOG.error("abc", new RuntimeException(""));
				}
			}).start();
		}
	}
	
	public static void main(String ...a) {
		org.slf4j.LoggerFactory.getLogger("").error("abc {}", "1\n3\n");
	}
}