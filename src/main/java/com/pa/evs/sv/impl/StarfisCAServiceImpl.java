package com.pa.evs.sv.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.sv.StarfishCAService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.RSAUtil;

@Component
public class StarfisCAServiceImpl implements StarfishCAService {

	static final Logger LOG = LogManager.getLogger(StarfisCAServiceImpl.class);
	RestTemplate restTemplate = getRestTemplate();
	
	// End entity profile set Certificate Authority (caId) and Certificate Profile
	// End Entity set End entity profile
	// starfish.ca-request.caId
	// starfish.ca-request.certProfileId
	// starfish.ca-request.endEntityProfileId
	// https://powerautomationsg.atlassian.net/browse/MMS-419
	
	@Override
	public Map<String, Object> requestCA(InputStream csr, String entityUsername, String certProfileId, String caId) throws IOException {
		
		String caCequestUrl = AppProps.get("starfish.ca-request.url", "https://starfishdemo.local:8443/starfish/certificateRequest");
		return requestCA(365, caCequestUrl, csr, null, entityUsername, certProfileId, caId);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> requestCA(Integer validityDays, String caCequestUrl, InputStream csr, String endEntityProfileId, String entityUsername, String certProfileId, String caId) throws IOException {
		
		String entityId = null;
		certProfileId = StringUtils.isNotBlank(certProfileId) ? certProfileId : AppProps.get("starfish.ca-request.certProfileId", "7");
		if (StringUtils.isNotBlank(entityUsername)) {
			entityId = getEntityIdByUsername(entityUsername);
			if (StringUtils.isBlank(entityId)) {
				createEntity(entityUsername, AppProps.get("starfish.ca-request.endEntityProfileId", "432"));
				entityId = getEntityIdByUsername(entityUsername);
			}
		}
		
		if (validityDays == null || validityDays <= 0) {
			validityDays = 365;
		}
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			IOUtils.copy(csr, bos);
			
			String content = new String(new String(bos.toByteArray())
					.replaceAll("\r*\n", "")
					.replace("\r", "")
					.replaceAll("\\s+","")
					.replaceAll("-----([^\\-])+-----", ""));
			//-----BEGIN CERTIFICATE REQUEST-----
			//MIICuDCCAaACAQAwdTEQMA4GA1UEBwwHQmVvZ3JhZDELMAkGA1UEBhMCUlMxHDAaBgNVBAMME1dlYlNlcnZlciBWZWxpa2kgMDExJzAlBgNVBAoMHk5ldFNlVCBHbG9iYWwgU29sdXRpb25zIGQuby5vLjENMAsGA1UECwwEUEtJIDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJTzmkyX9Kw8JJBTeeVsEgA5/955a4JGDncFo8NsuZ/U0dOJYvOR+3q4cgoCmOTCLecOPCsAACsmM43NUkUXaw+95F5JBjFC9FFoEQ0CQeBUzAsxqlE1AcCfNxH7ibEI/WLCfVv5ehYbQFynIFtdxInC/ChiRbIFyglpcYeqF+7kq5I2ioFXo9qF6GkP+Me2r9UIyYdHOV3YqDIbqYeyI/nbBSNk3zpUKtP1TdUYvrGzX5NYB6LnCocQgn0ecOiR9t76HuBtBg1ptKzFkGJe4eWmwDiyt7z+fpPB420xgDauZbwf104T7D7mXTHWY1NyqAPHguvn9zl7A/b6HQSo0wMCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAiZod7V6kjWuSgK+j1/vLjUu/9lLcKVLHTLz7IWbX931gtZX0+Utt6ngq3KKu66BMbDUTu7M75zQYOwIrX91fGAFnyzoHjbm33iElxguoSbpWt8dPD3wLMAR+m1vblWv7Fa99e/UT/G3wZj+zBHbIj40AEBK3cbbdvE+bQuwxFYBHYJjHKiujmFmqu0Uahlri4yO0fNhdSPn2sHPJUV+gDd3QOpQrHw8YVXTrhvjp4S+oBRxFjDu7j/iHNEy2XxnysF6n7axJIx2dNQYL5d/QzarJFbFFoYQNE1vbVsARe9wsKjDSSpd5vS9IyQwIwA81sPihseuMlaP8xXG6HpgPow==
			//-----END CERTIFICATE REQUEST-----
			
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
			Date startDate = c.getTime();
			c.add(Calendar.DAY_OF_YEAR, validityDays);
			Date endDate = c.getTime();
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("userId", Integer.parseInt(StringUtils.isNotBlank(entityId) ? entityId : AppProps.get("starfish.ca-request.userId", "33")));
			//  https://starfishdemo.local:8443/starfish/certificateProfile participantKind : "endEntity"
			data.put("certProfileId", Integer.parseInt(certProfileId));
			data.put("caId", StringUtils.isNotBlank(caId) ? caId : Integer.parseInt(AppProps.get("starfish.ca-request.caId", "411")));
			data.put("request", content);
			data.put("notBefore", sf.format(startDate));
			data.put("notAfter", sf.format(endDate));
			
			HttpEntity<Object> entity = new HttpEntity<>(data, headers);
			
			LOG.info("invoking request CA entityUsername=" + entityUsername + " request= " + entity);
			
			Map<String, Object> body = restTemplate.exchange(AppProps.get("starfish.ca-request.url", "https://starfishdemo.local:8443/starfish/certificateRequest"), HttpMethod.POST, entity, Map.class).getBody();
			Map<String, Object> ca = (Map<String, Object>) body.get("data");
			if (ca == null) {
				throw new RuntimeException("request CA error entityUsername=" + entityUsername + " ressponse=" + body);
			}
			return (Map<String, Object>) ca.get("digitalCertificate");
		}
	}
	
	@SuppressWarnings("unchecked")
	private String getEntityIdByUsername(String username) {
		try {
			String payloadStr = "{\r\n"
					+ "    \"userName\": \"SSL Server Demo\",\r\n"
					+ "    \"subjectName\": {\r\n"
					+ "       	\"commonName\": \"\",\r\n"
					+ "        \"givenName\": [],\r\n"
					+ "        \"surname\": [],\r\n"
					+ "        \"organization\": [],\r\n"
					+ "        \"organizationIdentifier\": [],\r\n"
					+ "        \"organizationUnit\": [],\r\n"
					+ "        \"businessCategory\": [],\r\n"
					+ "        \"location\": [],\r\n"
					+ "        \"email\": [],\r\n"
					+ "        \"state\": [],\r\n"
					+ "        \"country\": [],\r\n"
					+ "        \"street\": [],\r\n"
					+ "        \"postalCode\": [],\r\n"
					+ "        \"serialNumber\": [],\r\n"
					+ "        \"ipAddress\": [],\r\n"
					+ "        \"msPrincipalName\": [],\r\n"
					+ "        \"guid\": [],\r\n"
					+ "        \"dnsName\": [],\r\n"
					+ "        \"uid\": \"\",\r\n"
					+ "        \"personalNumber\": \"\",\r\n"
					+ "        \"jurisdictionLocalityName\": [],\r\n"
					+ "        \"jurisdictionStateOrProvinceName\": [],\r\n"
					+ "        \"jurisdictionCountryName\": [],\r\n"
					+ "        \"uniformResourceIdentifier\": []\r\n"
					+ "    }\r\n"
					+ "}";
			
			Map<String, Object> pl = new ObjectMapper().readValue(payloadStr, Map.class);
			pl.put("userName", username);
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> entity = new HttpEntity<>(pl, headers);
			
			LOG.info("invoking getEntityIdByUsername entityUsername=" + username + " request= " + entity);
			
			Map<String, Object> body = restTemplate.exchange(AppProps.get("starfish.findUsers.url", "https://starfishdemo.local:8443/starfish/findUsers?maxResults=100"), HttpMethod.POST, entity, Map.class).getBody();
			Map<String, Object> ca = (Map<String, Object>) body.get("data");
			if (ca == null) {
				return null;
			}
			Map<String, Object> result = (Map<String, Object>) ca.get("result");
			if (result == null) {
				return null;
			}
			List<Map<String, Object>> entities = (List<Map<String, Object>>) result.get("endEntities");
			if (entities == null || entities.isEmpty()) {
				return null;
			}
			return entities.get(0).get("endEntityId") + "";
		} catch (Exception e) {
			LOG.error("getEntityIdByUsername error " + e.getMessage(), e);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createEntity(String username, String endEntityProfileId) {
		
		try {
			String payloadStr = "{\r\n"
					+ "  \"id\": 0,\r\n"
					+ "  \"type\": \"endEntity\",\r\n"
					+ "  \"userName\": \"" + username + "\",\r\n"
					+ "  \"subjectDn\": \"\",\r\n"
					+ "  \"endEntityProfileId\": " + Integer.parseInt(endEntityProfileId) + ",\r\n"
					+ "  \"certificates\": [],\r\n"
					+ "  \"tokenData\": [],\r\n"
					+ "  \"requests\": [],\r\n"
					+ "  \"extendedAttributes\": [],\r\n"
					+ "  \"subjectName\": {\r\n"
					+ "    \"commonName\": \"" + username + "\",\r\n"
					+ "    \"givenName\": [],\r\n"
					+ "    \"surname\": [],\r\n"
					+ "    \"organization\": [\r\n"
					+ "      \"PA\"\r\n"
					+ "    ],\r\n"
					+ "    \"organizationIdentifier\": [],\r\n"
					+ "    \"organizationUnit\": [\r\n"
					+ "      \"BU500\"\r\n"
					+ "    ],\r\n"
					+ "    \"businessCategory\": [],\r\n"
					+ "    \"location\": [],\r\n"
					+ "    \"email\": [],\r\n"
					+ "    \"pseudonym\": [],\r\n"
					+ "    \"dc\": [],\r\n"
					+ "    \"serialNumber\": [],\r\n"
					+ "    \"state\": [],\r\n"
					+ "    \"country\": [\r\n"
					+ "      \"SG\"\r\n"
					+ "    ],\r\n"
					+ "    \"street\": [],\r\n"
					+ "    \"postalCode\": [],\r\n"
					+ "    \"ipAddress\": [],\r\n"
					+ "    \"msPrincipalName\": [],\r\n"
					+ "    \"guid\": [],\r\n"
					+ "    \"dnsName\": [],\r\n"
					+ "    \"uid\": \"\",\r\n"
					+ "    \"personalNumber\": \"\",\r\n"
					+ "    \"jurisdictionLocalityName\": [],\r\n"
					+ "    \"jurisdictionStateOrProvinceName\": [],\r\n"
					+ "    \"jurisdictionCountryName\": [],\r\n"
					+ "    \"uniformResourceIdentifier\": [],\r\n"
					+ "    \"houseIdentifier\": []\r\n"
					+ "  }\r\n"
					+ "}";
					
			Map<String, Object> pl = new ObjectMapper().readValue(payloadStr, Map.class);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> entity = new HttpEntity<>(pl, headers);
			LOG.info("invoking createEntity entityUsername=" + username + " request= " + entity);
			
			Map<String, Object> body = restTemplate.exchange(AppProps.get("starfish.endEntity.url", "https://starfishdemo.local:8443/starfish/endEntity"), HttpMethod.POST, entity, Map.class).getBody();
			LOG.info("createEntity entityUsername=" + username + " ressponse=" + body);
		} catch (Exception e) {
			LOG.info("createEntity error: " + e.getMessage(), e);
		}
	}
	
	private RestTemplate getRestTemplate() {

		org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
		try {
			org.apache.http.conn.ssl.TrustStrategy acceptingTrustStrategy = new org.apache.http.conn.ssl.TrustStrategy() {
				public boolean isTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
					return true;
				}
			};
			
			java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
			keyStore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("NetSeT-User Demo0000000058.pfx"), "79565965".toCharArray());
//			java.security.KeyStore keyStore = java.security.KeyStore.getInstance("JKS");
//			keyStore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("NetSeT-User Demo0000000058.jks"), "79565965".toCharArray());
			
			javax.net.ssl.SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
					.loadKeyMaterial(keyStore, "79565965".toCharArray())
					.loadTrustMaterial(null, acceptingTrustStrategy).build();
			
			HttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
	                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext).build())
	                .build();
			
			org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient = org.apache.hc.client5.http.impl.classic.HttpClients
					.custom()
					.setConnectionManager(connManager)
					.build();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			restTemplate.setRequestFactory(requestFactory);
			return restTemplate;
		} catch (Exception e) {
			return restTemplate;
		}
	}
	
	@Override
	public String formatCA(String base64) throws Exception {
		
		StringWriter writer = new StringWriter();
		PemWriter pemWriter = new PemWriter(writer);
		pemWriter.writeObject(new PemObject("CERTIFICATE", RSAUtil.generateCertificate(base64).getEncoded()));
		pemWriter.flush();
		pemWriter.close();
		return writer.toString();
	}
	
	public static void main(String[] args) throws Exception {
		
		InputStream csr = new FileInputStream("D:\\home\\evs-data\\csr_vendor_1_SHA256withECDSA_1712724921565.csr");
		StarfisCAServiceImpl starfishCAService = new StarfisCAServiceImpl();
//		starfishCAService.requestCA(csr, "BIE3IEYAAMAHOABUAA.MCU.MMS.sg", "7", null);
//		
//		csr = new FileInputStream("D:\\home\\pi\\00000000becf3452-6cdffb90000f.thesmarthome.sg-eSE.csr");
//		Map<String, Object> ca = starfishCAService.requestCA(csr,"BIE3IEYAAMAHOABUAA.MCU.MMS.sg", "7", null);
//		Map<String, Object> awsFormatCrt = new LinkedHashMap<>();
//		
//		StringWriter writer = new StringWriter();
//		PemWriter pemWriter = new PemWriter(writer);
//		pemWriter.writeObject(new PemObject("CERTIFICATE", RSAUtil.generateCertificate((String) ca.get("content")).getEncoded()));
//		pemWriter.flush();
//		pemWriter.close();
//		
//		awsFormatCrt.put("Certificate", writer.toString());
//		
//		Map<String, Object> m = SimpleMap.init("cas", new ObjectMapper().writeValueAsString(awsFormatCrt))
//				.more("startDate", Instant.parse(ca.get("notBefore") + "Z").toEpochMilli())
//				.more("endDate", Instant.parse(ca.get("notAfter") + "Z").toEpochMilli());
//		System.out.println(m);
//		
//		System.out.println(starfishCAService.formatCA("MIIEajCCAlKgAwIBAgIJEPdJq9n+V6QdMA0GCSqGSIb3DQEBCwUAMBIxEDAOBgNVBAMMB1Jvb3QgQ0EwHhcNMjUwMzAzMDg0NzUwWhcNMzAwMzAzMDg0NzUwWjBfMQswCQYDVQQGEwJTRzELMAkGA1UEYQwCUEExDjAMBgNVBAsMBUJVNTAwMQswCQYDVQQKDAJQQTEmMCQGA1UEAwwdQklFM0lFWUFBTUFIT0FCVUFBLk1DVS5NTVMuc2cwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASSbHmwiHAREEijMVvp30oeAe6oDyMOZEyr1QsmdJaBt0t3/CBXqOUtt9cjA0HwrxBSJfjytvP4ET5rw7+gtRNeo4IBPzCCATswCQYDVR0TBAIwADAOBgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwHwYDVR0jBBgwFoAUJJN8rYQmeiWBJ7f+808QG/438KkwHQYDVR0OBBYEFFZ9upTRZpweypbv5Q90GiG0t9LDMDAGA1UdHwQpMCcwJaAjoCGGH2h0dHA6Ly9sb2NhbGhvc3QvY3JsL1Jvb3RDQS5jcmwwPgYIKwYBBQUHAQEEMjAwMC4GCCsGAQUFBzAChiJodHRwOi8vbG9jYWxob3N0L2NhY2VydC9Sb290Q0EuY2VyMFcGA1UdIARQME4wTAYNKwYBBAGD0DMKAQMFAzA7MDkGCCsGAQUFBwIBFi1odHRwOi8vY2EudGVzdC5yZWxpZWZ2YWxpZGF0aW9uLmNvbS5iZC9jcGNwcy8wDQYJKoZIhvcNAQELBQADggIBAF+Wybu07wr+xl8lxSE0rAg/nWAoRUZ/gloE4nSLQhZV5tamxdzL9NXvEYCGGyuE1GeEELeIpv44tKgSLV1JKyim28E+z2hAA7+r7i6v6E1hlC2IYhjZHqcyCUeuUU1NoEC/zCIcq2PEUYDzA5JbdAmrD0HmjkDEnyvHwLOV3r+JpurMeK/F+nM6u3x+TqsJn29l62XlAmYo98zeZQaoNm6pzrtSiMpRBpLqYcHaQsJrOzSt/kT24tw9+bkXHPH6Ke4tGm7shCcFQahkIyPTGv7CSp33ZNFLfeo8mpLaKekxftsFyvDXuldV5IatNqwicnJDYXMSzBJL9ExHuqrsZcJ+IyZ9fs4dEUJ0GIVXLabef138Hw/Zy3X2yiWQAA/ytBXL6uSfncPc45QDxyvR2fF55ThMLhUgXxEiVWIFLKgMFqK+gvr0/ySeS1OFZ+ZHNj9fRBWp1VLEcQdt5cvPDorgSgJ2/KM6UAdH1Wq7eEdBjd9F4sZqtuqovQ8gfcR9mW39rEiFv6rFn5UcwUBpyeIZ4zyBXGOrzohPMOhTRkDFj9UVyrmIUis4KFLvaEXydSl55rglQMvOtoj1edShkzrCpEwn6YqHNVcBfrcv+vxO2rcLlsGFl1YkZRpGgMjnGFoBKvcBfp7+lqxE1KxFU8S0Y2R7YGwZ7yFZphc8LkO1"));
		
		Map<String, Object> ca = starfishCAService.requestCA(csr, "SecurityAutoConfiguration" + ".MCU.MMS.sg", null, null);
		System.out.println(Base64.getEncoder().encodeToString(starfishCAService.formatCA(ca.get("content") + "").getBytes()));
	}
}
