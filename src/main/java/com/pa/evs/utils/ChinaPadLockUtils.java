package com.pa.evs.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class ChinaPadLockUtils {

	static final Logger LOGGER = LoggerFactory.getLogger(ChinaPadLockUtils.class);
	
	private ChinaPadLockUtils() {}
//	fetch("http://123.56.250.3/index/login/checkuser.html", {
//	  "headers": {
//	    "accept": "*/*",
//	    "accept-language": "en-US,en;q=0.9,vi;q=0.8",
//	    "cache-control": "no-cache",
//	    "content-type": "application/x-www-form-urlencoded; charset=UTF-8",
//	    "pragma": "no-cache",
//	    "x-kl-ajax-request": "Ajax_Request",
//	    "x-requested-with": "XMLHttpRequest",
//	    "Referer": "http://123.56.250.3/index/login/index.html",
//	    "Referrer-Policy": "strict-origin-when-cross-origin"
//	  },
//	  "body": "username=84374206818&password=S84374206818!p",
//	  "method": "POST"
//	});
	public static String loginChinaLockServer(String lcPhone, String password) {

		if ("true".equalsIgnoreCase(AppProps.get("DMS_IGNORE_LOCK_SERVER_USER", "false"))) {
			return null;
		}
		try {
			if (StringUtils.isBlank(lcPhone)) {
				return null;
			}
//			userId = buildSvUsername(userId);
//			userId = userId.replace("+", "");
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
			payload.add("username", lcPhone);
			payload.add("password", password);
			HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
			ResponseEntity<String> res = ApiUtils.getRestTemplate().exchange(
					AppProps.get("PAS_CN_H_MS", "http://123.56.250.3") + "/index/login/checkuser.html", HttpMethod.POST,
					entity, String.class);
			LOGGER.info("loginChinaLockServer rs " + res);
			List<String> ccs = res.getHeaders() == null ? null : res.getHeaders().get("Set-Cookie");
			if (ccs == null) {
				return null;
			}
			return ccs.stream().filter(c -> c != null && c.startsWith("PHPSESSID="))
					.map(c -> c.replaceAll("PHPSESSID=([^ =;]+).*$", "$1")).findFirst().orElse(null);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

//fetch("http://123.56.250.3/index/employee/editemployee.html", {
//"headers": {
//  "accept": "*/*",
//  "accept-language": "en-US,en;q=0.9,vi;q=0.8",
//  "cache-control": "no-cache",
//  "content-type": "application/x-www-form-urlencoded; charset=UTF-8",
//  "pragma": "no-cache",
//  "x-kl-ajax-request": "Ajax_Request",
//  "x-requested-with": "XMLHttpRequest",
//  "cookie": "PHPSESSID=ev9hlcbdorovchu4gmvrv583q0",
//  "Referer": "http://123.56.250.3/index/employee/index.html?v=4.0",
//  "Referrer-Policy": "strict-origin-when-cross-origin"
//},
//"body": "id=-1&number=123456789&username=dms.app.guest1&password=12345678%40Xx&nickname=HR&post=Test&role_id=218&department_id=149&area_id=633&status=1&is_admin=1&is_phone=0&start_time=2024-04-01&end_time=2024-04-27&white_list=8314%2C8326%2C8196%2C8200",
//"method": "POST"
//});	
	public static String createUserChinaLockServer(String lcPhone, String phone, String password) {

		if ("true".equalsIgnoreCase(AppProps.get("DMS_IGNORE_LOCK_SERVER_USER", "false"))) {
			return "1";
		}
		if (StringUtils.isBlank(lcPhone)) {
			return null;
		}
//		userId = buildSvUsername(userId);
		HttpHeaders headers = new HttpHeaders();
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		try {

			Calendar c = Calendar.getInstance();
			Date start = c.getTime();
			c.add(Calendar.YEAR, 2);
			Date end = c.getTime();
			String ck = "PHPSESSID=" + loginChinaLockServer(AppProps.get("PAS_CN_US_MS", "84374206818"),
					AppProps.get("PAS_CN_PD_MS", "S84374206818!p"));

			deleteUserChinaLockServer(lcPhone, ck);
			
//			Random random = new Random();

			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.add("cookie", ck);
			headers.add("Referer",
					AppProps.get("PAS_CN_H_MS", "http://123.56.250.3") + "/index/employee/index.html?v=4.0");

			payload.add("id", "-1");
			payload.add("number", lcPhone);
			payload.add("username", lcPhone);
			payload.add("password", password);
			// payload.add("nickname", "PA-" + email + " " + random.nextInt(10) + "-" + random.nextInt(10));// update need some modification
			payload.add("nickname", "PA " + (phone == null ? lcPhone : phone));
			payload.add("post", "Test");
			payload.add("role_id", "218");// Role Admin
			payload.add("department_id", "149");// Office
			payload.add("area_id", "633");// test6
			payload.add("status", "1");// Enable
			payload.add("is_admin", "1");// Administrator
			payload.add("is_phone", "0");// Administrator
			payload.add("start_time", new SimpleDateFormat("yyyy-MM-dd").format(start));
			payload.add("end_time", new SimpleDateFormat("yyyy-MM-dd").format(end));
			payload.add("white_list", "8314,8326,8196,8200");// Unlock the range: Region -> Republic of Singapore ->
																// Changi Airport
			HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
			ResponseEntity<String> res = ApiUtils.getRestTemplate().exchange(
					AppProps.get("PAS_CN_H_MS", "http://123.56.250.3") + "/index/employee/editemployee.html",
					HttpMethod.POST, entity, String.class);
			LOGGER.info("createUserChinaLockServer rs " + res);// body 2 -> exists user

			return res.getBody();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	public static String deleteUserChinaLockServer(String lcPhone, String ck) {

		if ("true".equalsIgnoreCase(AppProps.get("DMS_IGNORE_LOCK_SERVER_USER", "false"))) {
			return null;
		}
		if (StringUtils.isBlank(lcPhone)) {
			return null;
		}
//		userId = buildSvUsername(userId);
		HttpHeaders headers = new HttpHeaders();
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		try {

			ck = StringUtils.isBlank(ck)
					? "PHPSESSID=" + loginChinaLockServer(AppProps.get("PAS_CN_US_MS", "84374206818"),
							AppProps.get("PAS_CN_PD_MS", "S84374206818!p"))
					: ck;
			String id = getChinaLockServerUserIdByUserPhone(lcPhone, ck);
			if (StringUtils.isBlank(id)) {
				return "true";
			}
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.add("cookie", ck);
			payload.add("id", id);
			HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
			ResponseEntity<String> res = ApiUtils.getRestTemplate().exchange(
					AppProps.get("PAS_CN_H_MS", "http://123.56.250.3") + "/index/employee/deleteemployee.html",
					HttpMethod.POST, entity, String.class);
			LOGGER.info("deleteUserChinaLockServer rs " + res);

			return res.getBody();
		} catch (Exception e) {
			LOGGER.error("deleteUserChinaLockServer error " + e.getMessage());
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getTokenAppChinaLockServer(String lcPhone, String password) {
		if ("true".equalsIgnoreCase(AppProps.get("DMS_IGNORE_LOCK_SERVER_USER", "false"))) {
			return null;
		}
		try {
			if (StringUtils.isBlank(lcPhone)) {
				return null;
			}
//			userId = buildSvUsername(userId);
			HttpHeaders headers = new HttpHeaders();
			HttpEntity<Object> entity = new HttpEntity<>(headers);
			String url = AppProps.get("PAS_CN_H_MS", "http://123.56.250.3") + "/app_login?name=" + lcPhone + "&password="
					+ password + "&phoneid=1";
			ResponseEntity<String> res = ApiUtils.getRestTemplate().exchange(url, HttpMethod.GET, entity, String.class);
			LOGGER.info("getTokenAppChinaLockServer rs " + res);
			Map<String, Object> obj = ApiUtils.json2Object(res.getBody(), Map.class);
			return obj.get("data") == null ? null : (String) ((Map) obj.get("data")).get("token");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	public static String getChinaLockServerUserIdByUserPhone(String lcPhone, String ck) {
		if ("true".equalsIgnoreCase(AppProps.get("DMS_IGNORE_LOCK_SERVER_USER", "false"))) {
			return null;
		}
		try {
			if (StringUtils.isBlank(lcPhone)) {
				return null;
			}
//			userId = buildSvUsername(userId);
			HttpHeaders headers = new HttpHeaders();
			headers.add("cookie",
					StringUtils.isBlank(ck)
							? "PHPSESSID=" + loginChinaLockServer(AppProps.get("PAS_CN_US_MS", "84374206818"),
									AppProps.get("PAS_CN_PD_MS", "S84374206818!p"))
							: ck);
			HttpEntity<Object> entity = new HttpEntity<>(headers);
			String url = AppProps.get("PAS_CN_H_MS", "http://123.56.250.3")
					+ "/index/employee/index.html?number=&username=" + lcPhone
					+ "&nickname=&role_id=&department_id=&area_id=";
			ResponseEntity<String> res = ApiUtils.getRestTemplate().exchange(url, HttpMethod.GET, entity, String.class);
			LOGGER.info("getChinaLockServerUserIdByPhone rs " + res.getHeaders());
			Elements es = Jsoup.parse(res.getBody()).select("table#editable tbody tr button.btnDisplayDetail");
			if (es.size() == 1 && es.get(0).attr("onclick") != null && es.get(0).attr("onclick").indexOf(lcPhone) > 1) {
				return es.get(0).attr("onclick").replaceAll(".*openInfo\\( *['\"] *([^'\" ]+) *['\"] *,.*", "$1");
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	static String buildSvUsername(String userId) {
		if (userId.length() < 10) {
			userId = "0000000000" + userId;
			return "1" + userId.substring(userId.length() - 10);
		}
		return userId;
	}
	
	public static void main(String[] args) {

		System.out.println("openInfo('NBzg6NmDPj0cv5Daj6LQeQ==','65123123121')"
				.replaceAll(".*openInfo\\( *['\"] *([^'\" ]+) *['\"] *,.*", "$1"));
	}
}
