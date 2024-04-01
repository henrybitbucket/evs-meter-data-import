package com.pa.evs.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 * 
 * <!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient -->
		<dependency>
			<groupId>org.apache.httpcomponents</groupId>
			<artifactId>httpclient</artifactId>
			<version>4.5.4</version><!--$NO-MVN-MAN-VER$-->
		</dependency>

		<!-- https://mvnrepository.com/artifact/commons-io/commons-io -->
		<dependency>
			<groupId>commons-io</groupId>
			<artifactId>commons-io</artifactId>
			<version>2.5</version>
		</dependency>

		<!-- https://mvnrepository.com/artifact/org.apache.commons/commons-lang3 -->
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-lang3</artifactId>
			<version>3.0</version>
		</dependency>
				<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-web</artifactId>
			<version>4.3.14.RELEASE</version>
		</dependency>

		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-databind</artifactId>
			<version>2.8.10</version>
		</dependency>
 * </pre>
 * 
 */
public final class ApiUtils {


	public static final String HOST = "http://localhost:8080";

	public static final String CHAR_SET = "UTF-8";

	public static final String PATHS = "paths";

	public static final String IMPORT = "import";

	public static final String ROOT = System.getProperty("user.dir").replaceAll("\\\\", "/") + "/src/main/java";

	static final Log LOG = LogFactory.getLog(ApiUtils.class.getSimpleName());

	private ApiUtils() {
	}

	public static RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		try {
			/**
			 * - consider build trusted strategy to accept TRUSTED CERT - currently accept
			 * all
			 */
			TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
					return true;
				}
			};
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

			// stop redirect follow
			LaxRedirectStrategy laxRedirectStrategy = new LaxRedirectStrategy() {

				@Override
				protected boolean isRedirectable(String method) {
					//
					return super.isRedirectable(method);
				}
			};
			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf)
					.setRedirectStrategy(laxRedirectStrategy).build();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			restTemplate.setRequestFactory(requestFactory);
			
			MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
			FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
			formHttpMessageConverter.setCharset(Charset.forName(CHAR_SET));
			StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(
					Charset.forName(CHAR_SET));
			stringHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_HTML, MediaType.ALL));

			MediaType mediaType = new MediaType("application", "x-www-form-urlencoded", Charset.forName(CHAR_SET));
			MediaType mediaTypeRelated = new MediaType("multipart", "related", Charset.forName(CHAR_SET));
			formHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(mediaType, MediaType.MULTIPART_FORM_DATA,
					mediaTypeRelated, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN));
			restTemplate.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Arrays
					.asList(stringHttpMessageConverter, jackson2HttpMessageConverter, formHttpMessageConverter, new ByteArrayHttpMessageConverter())));
		} catch (Exception e) {
			//
		}
		return restTemplate;
	}

	public static <T> ResponseEntity<T> call(String url, HttpHeaders headers, Object payload, HttpMethod httpMethod,
			String cookie, Class<T> clazzResponse, Object... args) {

		LinkedMultiValueMap<String, String> data = new LinkedMultiValueMap<>();
		for (Object ar : args) {
			prepareData(data, ar);
		}

		HttpEntity<?> requestEntity;
		if (httpMethod == HttpMethod.GET) {
			requestEntity = new HttpEntity<>(null);
			logCall(url, null);
			return logRp(getRestTemplate().exchange(url, httpMethod, requestEntity, clazzResponse, data), url);
		}
		if (headers == null) {
			headers = new HttpHeaders();
		}
		if (headers.getContentType() == null) {
			headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE + "; charset=UTF-8");
		}

		if (StringUtils.isNotEmpty(cookie)) {
			headers.add("cookie", cookie);
		}
		addUserAgent(headers);
		requestEntity = new HttpEntity<>(payload != null ? payload : data, headers);
		logCall(url, headers);
		try {
			return logRp(getRestTemplate().exchange(url, httpMethod, requestEntity, clazzResponse), url);
		} catch (HttpClientErrorException e) {
			return logRp(handle4xx(e, clazzResponse), url);
		}

	}
	
	private static void prepareData(LinkedMultiValueMap<String, String> data, Object ar) { 
		
		if (ar == null) {
			return;
		}

		if (ar instanceof String) {
			String arg = (String) ar;
			if (StringUtils.isNotBlank(arg) && arg.contains("=")) {
				String k = arg.trim().replaceAll("(^[^=]+)=(.*\r*\n*)+", "$1");
				String v = arg.trim().replaceAll("(^[^=]+=)((?:(.*\r*\n*)+))", "$2");
				if ("null".equalsIgnoreCase(v) || StringUtils.isBlank(v)) {
					v = "";
				}
				data.add(k, v);
			}
		}
		
		if (ar instanceof String[]) {
			String[] kv = (String[]) ar;
			if (kv.length > 1 && kv[1] != null && !"null".equalsIgnoreCase(kv[1])) {
				data.add(kv[0], kv[1]);
			}
		}
	}

	public static <T> ResponseEntity<T> post(String url, HttpHeaders headers, Object payload, Class<T> clazzResponse,
			Object... args) {
		return call(url, headers, payload, HttpMethod.POST, null, clazzResponse, args);
	}

	public static <T> ResponseEntity<T> post(String url, Class<T> clazzResponse, Object... args) {
		return call(url, null, null, HttpMethod.POST, null, clazzResponse, args);
	}
	
	public static <T> ResponseEntity<T> put(String url, HttpHeaders headers, Object payload, Class<T> clazzResponse,
			Object... args) {
		return call(url, headers, payload, HttpMethod.PUT, null, clazzResponse, args);
	}

	public static <T> ResponseEntity<T> put(String url, Class<T> clazzResponse, Object... args) {
		return call(url, null, null, HttpMethod.PUT, null, clazzResponse, args);
	}

	/**
	 * login facebook must be add cookie: wd=1600x442
	 * 
	 * @param url
	 * @param cookie
	 * @param clazzResponse
	 * @param args
	 * @return
	 */
	public static <T> ResponseEntity<T> post(String url, HttpHeaders headers, Object payLoad, String cookie,
			Class<T> clazzResponse, Object... args) {
		return call(url, headers, payLoad, HttpMethod.POST, cookie, clazzResponse, args);
	}

	public static <T> ResponseEntity<T> post(String url, String cookie, Class<T> clazzResponse, Object... args) {
		return call(url, null, null, HttpMethod.POST, cookie, clazzResponse, args);
	}

	/**
	 * 
	 * @param url
	 * @param clazzResponse
	 * @param args
	 *            eg: url = abc.com/{arg1}?xyz={arg2}
	 * @return
	 */
	public static <T> ResponseEntity<T> get(String url, Class<T> clazzResponse, Object... args) {
		return get(url, new HttpHeaders(), clazzResponse, args);
	}

	public static <T> ResponseEntity<T> get(String url, HttpHeaders headers, Class<T> clazzResponse, Object... args) {

		if (headers == null) {
			headers = new HttpHeaders();
		}
		addUserAgent(headers);
		HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);
		if (args == null || args.length == 0) {
			args = getArgs(url);
		}
		logCall(url, headers);
		try {
			return logRp(getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, clazzResponse, args), url);
		} catch (HttpClientErrorException e) {
			return logRp(handle4xx(e, clazzResponse), url);
		} catch (ResourceAccessException e) {
			LOG.error(e.getMessage());
			throw new IllegalStateException(e.getMessage());
		}

	}

	private static void logCall(Object url, HttpHeaders headers) {
		//
	}

	private static <T> ResponseEntity<T> logRp(ResponseEntity<T> rp, String url) {
		return rp;
	}

	public static <T> ResponseEntity<T> get(String url, HttpHeaders headers, String cookie, Class<T> clazzResponse,
			Object... args) {

		if (headers == null) {
			headers = new HttpHeaders();
		}
		headers.add("cookie", cookie);
		addUserAgent(headers);
		HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);
		if (args == null || args.length == 0) {
			args = getArgs(url);
		}
		try {
			return logRp(getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, clazzResponse, args), url);
		} catch (HttpClientErrorException e) {
			return logRp(handle4xx(e, clazzResponse), url);
		}

	}

	public static <T> ResponseEntity<T> get(String url, String cookie, Class<T> clazzResponse, Object... args) {
		return get(url, new HttpHeaders(), cookie, clazzResponse, args);

	}

	private static void addUserAgent(HttpHeaders headers) {
		headers.add("user-agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
	}

	@SuppressWarnings("unchecked")
	public static <T> ResponseEntity<T> handle4xx(HttpClientErrorException ex, Class<T> clazz) {
		HttpHeaders httpHeaders = ex.getResponseHeaders();
		T body = null;
		if (MediaType.APPLICATION_JSON.equals(httpHeaders.getContentType())) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				body = mapper.readValue(ex.getResponseBodyAsByteArray(), clazz);
			} catch (IOException e1) {
				throw ex;
			}
		} else {
			
			if (clazz == String.class) {
				body = (T) ex.getResponseBodyAsString();
			}
		}
		return new ResponseEntity<>(body, httpHeaders, ex.getStatusCode());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ResponseEntity<T> handle5xx(HttpServerErrorException ex, Class<T> clazz) {
		HttpHeaders httpHeaders = ex.getResponseHeaders();
		T body = null;
		if (MediaType.APPLICATION_JSON.equals(httpHeaders.getContentType())) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				body = mapper.readValue(ex.getResponseBodyAsByteArray(), clazz);
			} catch (IOException e1) {
				throw ex;
			}
		} else {
			
			if (clazz == String.class) {
				body = (T) ex.getResponseBodyAsString();
			}
		}
		return new ResponseEntity<>(body, httpHeaders, ex.getStatusCode());
	}

	/**
	 * fields=feed{picture,message,full_picture,caption,name,link}
	 * 
	 * @param url
	 * @return
	 */
	private static Object[] getArgs(String url) {

		Pattern p = Pattern.compile("\\{[^\\{\\}]*\\}");
		List<Object> rp = new ArrayList<>();
		Matcher m = p.matcher(url);
		while (m.find()) {
			rp.add(m.group(0));
		}
		return rp.toArray(new Object[rp.size()]);
	}

	public static class QWrapper {

		final Map<String, Object> inner;

		public QWrapper(Object obj) {
			inner = toMap(obj);
		}

		public QWrapper query(String key) {
			return new QWrapper(inner.get(key));
		}

		@SuppressWarnings("unchecked")
		public <T> T query(String key, Class<T> clazz) {
			Object obj = inner.get(key);
			if (obj == null) {
				return null;
			}
			if (clazz.isAssignableFrom(obj.getClass())) {
				return (T) inner.get(key);
			}
			return null;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T query(String key, Class<T> clazz, T defaultValue) {
			Object obj = inner.get(key);
			if (obj == null) {
				return defaultValue;
			}
			if (clazz.isAssignableFrom(obj.getClass())) {
				return (T) inner.get(key);
			}
			return defaultValue;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T queryListRandomOne(Class<T> clazz, String... key) {
			
			if (key == null) {
				return null;
			}
			
			Map<String, Object> temp = inner;
			for (int i = 0; i < key.length; i++) {
				Object obj = temp.get(key[i]);
				if (i == key.length - 1) {
					
					if (obj instanceof List) {
				
						List<T> arr = (List<T>) obj;
						if (arr.isEmpty()) {
							return null;
						}
						if (arr.size() == 1) {
							return arr.get(0);
						}
						Random random = new Random();
						int index = 0;
						
						try {
							index = random.nextInt(arr.size() - 1);
						} catch (Exception e) {
							System.out.println(index + " " + arr.size());
							throw e;
						}
						try {
							
							return arr.get(index);
						} catch (Exception e) {
							System.out.println(index + " " + arr.size());
							throw e;
						}
					} else {
						return null;
					}
				}
				temp = (Map<String, Object>) obj;
			}
			return null;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T query(Class<T> clazz, String... key) {
			
			if (key == null) {
				return null;
			}
			
			Map<String, Object> temp = inner;
			for (int i = 0; i < key.length; i++) {
				Object obj = temp.get(key[i]);
				if (i == key.length - 1) {
					return (T) obj;
				}
				temp = (Map<String, Object>) obj;
			}
			return null;
		}

		@SuppressWarnings({ "unchecked" })
		private Map<String, Object> toMap(Object obj) {
			if (obj instanceof Map) {
				return (Map<String, Object>) obj;
			}
			return new HashMap<>();
		}

		@Override
		public String toString() {
			return "QWrapper [inner=" + inner + "]";
		}
	}

	public static byte[] readBytes(InputStream in) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int t = in.read(buffer, 0, buffer.length);
		while (t != -1) {
			bos.write(buffer, 0, t);
			t = in.read(buffer, 0, buffer.length);
		}
		return bos.toByteArray();
	}

	public static String getActionFormHtml(String htmlBody) {

		String rx = "<[^<>]+action=\"([^\"]+)\"[^<>]+>";
		Pattern p = Pattern.compile(rx);
		Matcher m = p.matcher(htmlBody);
		while (m.find()) {
			String formTag = m.group(0);
			if (StringUtils.isNotBlank(formTag)) {
				return formTag.replaceAll(rx, "$1");
			}
		}
		return "";
	}

	public static String getValueInput(String name, String html, String defaultValue, boolean pre) {

		if (defaultValue == null) {
			defaultValue = "";
		}
		String prefix = "";
		if (pre) {
			prefix = name + "=";
		}
		String rx = "(<[^<>]+name=\"" + name + "\"[^<>]+>)";
		Pattern p = Pattern.compile(rx);
		Matcher m = p.matcher(html);
		if (m.find()) {
			String inputTag = m.group(0);
			if (!inputTag.contains("value")) {
				return prefix + defaultValue;
			}
			rx = "<[^<>]+value=\"([^\"]*)\"[^<>]+>";
			String vl = inputTag.replaceAll(rx, "$1");
			if (StringUtils.isBlank(vl)) {
				return prefix + defaultValue;
			}
			return prefix + vl;
		}
		return prefix + defaultValue;
	}

	public static String getValueInput(String name, String html, boolean pre) {
		return getValueInput(name, html, null, pre);
	}

	public static String getValueInput(String name, String html) {
		return getValueInput(name, html, null, true);
	}
	
	public static String covertNormalizerString(String str) {
		try {
			String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
			Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
			return pattern.matcher(temp).replaceAll("").toLowerCase().replaceAll(" ", "-").replaceAll("đ", "d").replaceAll("[^a-zA-Z0-9\\-]", "");
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static String toStringJson(Object obj) {
		
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static <T> T json2Object(String src, Class<T> type) {
		try {
			return new ObjectMapper().readValue(src, type);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static <T> T json2Object(String src, Class<T> type, T defaultValue) {
		try {
			return new ObjectMapper().readValue(src, type);
		} catch (Exception e) {
			
		}
		return defaultValue;
	}
}
