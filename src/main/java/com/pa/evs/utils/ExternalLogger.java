package com.pa.evs.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Marker;
import org.springframework.jms.core.JmsTemplate;

import com.pa.evs.JmsConfig;

public class ExternalLogger implements org.slf4j.Logger {
	
	org.slf4j.Logger originalLoger = null;
	
	JmsTemplate jmsTemplate;
	
	boolean isJmsReady = false;
	
	private boolean isJmsReady() {
		if (!isJmsReady) {
			isJmsReady = JmsConfig.JMS_READY > 0;
		}
		return "true".equalsIgnoreCase(AppProps.get("use.jmslogger", "true")) && isJmsReady;
	}
	
	public static org.slf4j.Logger getLogger(Class<?> clzz) {
		return getLogger(clzz.getName());
	}
	
	public static org.slf4j.Logger getLogger(String name) {
		ExternalLogger logger = new ExternalLogger();
		logger.originalLoger = org.slf4j.LoggerFactory.getLogger(name);
		logger.name = name;
		return logger;
	}
	
	void send(String type, Object format, Object... arguments) {
		try {
			if (jmsTemplate == null) {
				jmsTemplate = AppProps.getContext().getBean(JmsTemplate.class);
			}
			if (jmsTemplate == null) {
				return;
			}
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("type", type);
			payload.put("name", name);
			payload.put("format", format);
			payload.put("thName", Thread.currentThread().getName());
			StringBuilder args = new StringBuilder();
			for (int i = 0; i < arguments.length; i++) {
				Object arg = arguments[i];
				if (arg instanceof Throwable) {
					args.append("\n").append(ExceptionUtils.getStackTrace((Throwable) arg));
				} else {
					args.append(arg);
				}
				if (i < arguments.length - 1) {
					args.append("<<<log4j2>>>");
				}
			}
			payload.put("arguments", args.toString());
			jmsTemplate.convertAndSend("logQ", payload);
		} catch (Exception e) {
			originalLoger.error(e.getMessage(), e);
		}
	}
	
	private String name;
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public void trace(String msg) {
		if (isJmsReady()) {
			send("trace", msg);			
		} else {
			originalLoger.trace(msg);
		}
	}

	@Override
	public void trace(String format, Object arg) {
		if (isJmsReady()) {
			send("trace", format, arg);	
		} else {
			originalLoger.trace(format, arg);
		}
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if (isJmsReady()) {
			send("trace", format, arg1, arg2);
		} else {
			originalLoger.trace(format, arg1, arg2);
		}
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (isJmsReady()) {
			send("trace", format, arguments);
		} else {
			originalLoger.trace(format, arguments);
		}
	}

	@Override
	public void trace(String msg, Throwable t) {
		if (isJmsReady()) {
			send("trace", msg, t);
		} else {
			originalLoger.trace(msg, t);
		}
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return true;
	}

	@Override
	public void trace(Marker marker, String msg) {
		
	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		
	}

	@Override
	public void trace(Marker marker, String format, Object... argArray) {
		
	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public void debug(String msg) {
		if (isJmsReady()) {
			send("debug", msg);
		} else {
			originalLoger.debug(msg);
		}
	}

	@Override
	public void debug(String format, Object arg) {
		if (isJmsReady()) {
			send("debug", format, arg);
		} else {
			originalLoger.debug(format, arg);
		}
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if (isJmsReady()) {
			send("debug", format, arg1, arg2);
		} else {
			originalLoger.debug(format, arg1, arg2);
		}
	}

	@Override
	public void debug(String format, Object... arguments) {
		if (isJmsReady()) {
			send("debug", format, arguments);
		} else {
			originalLoger.debug(format, format, arguments);
		}
	}

	@Override
	public void debug(String msg, Throwable t) {
		if (isJmsReady()) {
			send("debug", msg, t);
		} else {
			originalLoger.debug(msg, t);
		}
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return true;
	}

	@Override
	public void debug(Marker marker, String msg) {
	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
	}

	@Override
	public void debug(Marker marker, String format, Object... arguments) {
	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public void info(String msg) {
		if (isJmsReady()) {
			send("info", msg);
		} else {
			originalLoger.info(msg);
		}
	}

	@Override
	public void info(String format, Object arg) {
		if (isJmsReady()) {
			send("info", format, arg);
		} else {
			originalLoger.info(format, arg);
		}
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		if (isJmsReady()) {
			send("info", format, arg1, arg2);
		} else {
			originalLoger.info(format, arg1, arg2);
		}
	}

	@Override
	public void info(String format, Object... arguments) {
		if (isJmsReady()) {
			send("info", format, arguments);
		} else {
			originalLoger.info(format, format, arguments);
		}
	}

	@Override
	public void info(String msg, Throwable t) {
		if (isJmsReady()) {
			send("info", msg, t);
		} else {
			originalLoger.info(msg, t);
		}
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return true;
	}

	@Override
	public void info(Marker marker, String msg) {
		
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		
	}

	@Override
	public void info(Marker marker, String format, Object... arguments) {
		
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		
	}

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	public void warn(String msg) {
		if (isJmsReady()) {
			send("warn", msg);
		} else {
			originalLoger.warn(msg);
		}
	}

	@Override
	public void warn(String format, Object arg) {
		if (isJmsReady()) {
			send("warn", format, arg);
		} else {
			originalLoger.warn(format, arg);
		}
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (isJmsReady()) {
			send("warn", format, arguments);
		} else {
			originalLoger.warn(format, arguments);
		}
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if (isJmsReady()) {
			send("warn", format, arg1, arg2);
		} else {
			originalLoger.warn(format, arg1, arg2);
		}
	}

	@Override
	public void warn(String msg, Throwable t) {
		if (isJmsReady()) {
			send("warn", msg, t);
		} else {
			originalLoger.warn(msg, t);
		}
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return true;
	}

	@Override
	public void warn(Marker marker, String msg) {
		
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		
	}

	@Override
	public void warn(Marker marker, String format, Object... arguments) {
		
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public void error(String msg) {
		if (isJmsReady()) {
			send("error", msg);
		} else {
			originalLoger.error(msg);
		}
	}

	@Override
	public void error(String format, Object arg) {
		if (isJmsReady()) {
			send("error", format, arg);
		} else {
			originalLoger.error(format, arg);
		}
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		if (isJmsReady()) {
			send("error", format, arg1, arg2);
		} else {
			originalLoger.error(format, arg1, arg2);
		}
	}

	@Override
	public void error(String format, Object... arguments) {
		if (isJmsReady()) {
			send("error", format, arguments);
		} else {
			originalLoger.error(format, arguments);
		}
	}

	@Override
	public void error(String msg, Throwable t) {
		if (isJmsReady()) {
			send("error", msg, t);
		} else {
			originalLoger.error(msg, t);
		}
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return true;
	}

	@Override
	public void error(Marker marker, String msg) {
		
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		
	}

	@Override
	public void error(Marker marker, String format, Object... arguments) {
		
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		
	}

}
