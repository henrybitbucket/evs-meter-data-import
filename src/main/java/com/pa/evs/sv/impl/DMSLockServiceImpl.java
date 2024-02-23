package com.pa.evs.sv.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.sv.DMSLockService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.DESUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;

@Service
public class DMSLockServiceImpl implements DMSLockService {

	static final Logger LOGGER = LoggerFactory.getLogger(DMSLockServiceImpl.class);
	
	static final ObjectMapper MAPPER = new ObjectMapper();
	String token;
	RestTemplate resttemplate = ApiUtils.getRestTemplate();
	
	@Override
	public Object search() {
		
		// TODO -> search pagin table dms_lock
		return getChinaLockPadLock();
	}
	
	public Object getChinaLockPadLock() {
		try {
			
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			String url = DESUtil.getInstance().decrypt(AppProps.get("APP_PAS_LIST_LOCKS"));
			HttpEntity<Object> entity = new HttpEntity<Object>(null);
			return MAPPER.readValue(resttemplate.exchange(url.replace("${token}", token), HttpMethod.GET, entity, String.class).getBody(), Map.class);
		} catch (Exception e) {
			Map<String, Object> res = new LinkedHashMap<>();
			res.put("code", -1);
			res.put("data", null);
			res.put("info", e.getMessage());
			return res;
		}
	}
	
	@Override
	public Object syncLock(Long vendorId) {
		
		// Vendors : ISRAALI_LOCK, CHINA_LOCK_PADLOCK
		// if vendorId -> get all vendor -> sync all
		
		// if vendor = CHINA_LOCK_PADLOCK
		Object data = getChinaLockPadLock();
		// save db ....
		
		
		// if vendor = ISRAALI_LOCK
		// chua co san.
		
		return null;
	}
	
	@PostConstruct
	public void init() {
		try {
			loginPAS();
		} finally {
			SchedulerHelper.scheduleJob("0 0/3 * * * ? *", () -> {
				loginPAS();
			}, "APP_PAS_LOGIN");
		}
		
		// sync on init
		syncLock(null);
	}
	
	@SuppressWarnings("unchecked")
	void loginPAS() {
		try {
			HttpEntity<Object> entity = new HttpEntity<Object>(null);
			String url = DESUtil.getInstance().decrypt(AppProps.get("APP_PAS_LOGIN"));
			Map<String, Object> res = MAPPER.readValue(resttemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody(), Map.class);
			Map<String, Object> data = (Map<String, Object>) res.get("data");
			token = (String) data.get("token");
			System.out.println(token);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
}
