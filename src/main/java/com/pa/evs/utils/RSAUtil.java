package com.pa.evs.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import com.pa.evs.sv.impl.EVSPAServiceImpl;

/**
 * 
	openssl ec -in server.key  -text -noout -> get type ASN1 OID: secp384r1
	openssl req -text -noout -verify -in server.csr -> get Signature Algorithm: ecdsa-with-SHA256
	openssl ec -pubin -in pub.pub -text
	openssl req -out server.csr -key server.key -new -sha256 -> set Signature Algorithm: ecdsa-with-SHA256
	
	# check match combo private-key, csr, crt
	openssl pkey -in server.key -pubout -outform pem | sha256sum
	openssl x509 -in certificate.crt -pubkey -noout -outform pem | sha256sum
	openssl req -in server.csr -pubkey -noout -outform pem | sha256sum
	
	https://8gwifi.org/PemParserFunctions.jsp
	PKCS#1 RSAPublicKey (PEM header: BEGIN RSA PUBLIC KEY)
	PKCS#8 EncryptedPrivateKeyInfo (PEM header: BEGIN ENCRYPTED PRIVATE KEY)
	PKCS#8 PrivateKeyInfo (PEM header: BEGIN PRIVATE KEY)
	X.509 SubjectPublicKeyInfo (PEM header: BEGIN PUBLIC KEY)
	CSR PEM header : (PEM header:----BEGIN NEW CERTIFICATE REQUEST-----)
	DSA PrivateKeyInfo (PEM header: (-----BEGIN DSA PRIVATE KEY----)
	RSA Public Key
	
	-----BEGIN RSA PUBLIC KEY-----
	-----END RSA PUBLIC KEY-----
	Encrypted Private Key
	
	-----BEGIN RSA PRIVATE KEY-----
	Proc-Type: 4,ENCRYPTED
	-----END RSA PRIVATE KEY-----
	CRL
	
	-----BEGIN X509 CRL-----
	-----END X509 CRL-----
	CRT
	
	-----BEGIN CERTIFICATE-----
	-----END CERTIFICATE-----
	CSR
	
	-----BEGIN CERTIFICATE REQUEST-----
	-----END CERTIFICATE REQUEST-----
	NEW CSR
	
	-----BEGIN NEW CERTIFICATE REQUEST-----
	-----END NEW CERTIFICATE REQUEST-----
	PEM
	
	-----END RSA PRIVATE KEY-----
	-----BEGIN RSA PRIVATE KEY-----
	PKCS7
	
	-----BEGIN PKCS7-----
	-----END PKCS7-----
	PRIVATE KEY
	
	-----BEGIN PRIVATE KEY-----
	-----END PRIVATE KEY-----
	DSA KEY
	
	-----BEGIN DSA PRIVATE KEY-----
	-----END DSA PRIVATE KEY-----
	Elliptic Curve
	
	-----BEGIN EC PRIVATE KEY-----
	-----BEGIN EC PRIVATE KEY-----
	PGP Private Key
	
	-----BEGIN PGP PRIVATE KEY BLOCK-----
	-----END PGP PRIVATE KEY BLOCK-----
	PGP Public Key
	
	-----BEGIN PGP PUBLIC KEY BLOCK-----
	-----END PGP PUBLIC KEY BLOCK-----
 *
 */
public class RSAUtil {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);

	static {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}
	
	private RSAUtil() {}

	/**
		String privateKey =  "-----BEGIN PRIVATE KEY-----\n"
	            + "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAM7t8Ub1DP+B91NJ\n"
	            + "nC45zqIvd1QXkQ5Ac1EJl8mUglWFzUyFbhjSuF4mEjrcecwERfRummASbLoyeMXl\n"
	            + "eiPg7jvSaz2szpuV+afoUo9c1T+ORNUzq31NvM7IW6+4KhtttwbMq4wbbPpBfVXA\n"
	            + "IAhvnLnCp/VyY/npkkjAid4c7RoVAgMBAAECgYBcCuy6kj+g20+G5YQp756g95oN\n"
	            + "dpoYC8T/c9PnXz6GCgkik2tAcWJ+xlJviihG/lObgSL7vtZMEC02YXdtxBxTBNmd\n"
	            + "upkruOkL0ElIu4S8CUwD6It8oNnHFGcIhwXUbdpSCr1cx62A0jDcMVgneQ8vv6vB\n"
	            + "/YKlj2dD2SBq3aaCYQJBAOvc5NDyfrdMYYTY+jJBaj82JLtQ/6K1vFIwdxM0siRF\n"
	            + "UYqSRA7G8A4ga+GobTewgeN6URFwWKvWY8EGb3HTwFkCQQDgmKtjjJlX3BotgnGD\n"
	            + "gdxVgvfYG39BL2GnotSwUbjjce/yZBtrbcClfqrrOWWw7lPcX1d0v8o3hJfLF5dT\n"
	            + "6NAdAkA8qAQYUCSSUwxJM9u0DOqb8vqjSYNUftQ9dsVIpSai+UitEEx8WGDn4SKd\n"
	            + "V8kupy/gJlau22uSVYI148fJSCGRAkBz+GEHFiJX657YwPI8JWHQBcBUJl6fGggi\n"
	            + "t0F7ibceOkbbsjU2U4WV7sHyk8Cei3Fh6RkPf7i60gxPIe9RtHVBAkAnPQD+BmND\n"
	            + "By8q5f0Kwtxgo2+YkxGDP5bxDV6P1vd2C7U5/XxaN53Kc0G8zu9UlcwhZcQ5BljH\n"
	            + "N24cUWZOo+60\n"
	            + "-----END PRIVATE KEY-----";
		
		PrivateKey prkey = generatePrivate(privateKey);
		PublicKey plKey = generatePublic(prkey);
		
		String signature = generateSignature("test message".getBytes(StandardCharsets.UTF_8), privateKey);
		System.out.println(verifySignature("test message".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(signature), plKey));
	 * @param messages
	 * @param publicKey
	 * @return
	 * @throws Exception
	 */
	public static String generateSignature(byte[] messages, PrivateKey publicKey) throws Exception {

		Signature ecdsaSign = Signature.getInstance("SHA256with" + publicKey.getAlgorithm());
		ecdsaSign.initSign(publicKey);
		ecdsaSign.update(messages);
		byte[] signature = ecdsaSign.sign();
		return Base64.getEncoder().encodeToString(signature);
	}
	
	public static boolean verifySignature(byte[] messages, byte[] signature, PublicKey publicKey) throws Exception {

		Signature ecdsaSign = Signature.getInstance("SHA256with" + publicKey.getAlgorithm());
		ecdsaSign.initVerify(publicKey);
		ecdsaSign.update(messages);
		return ecdsaSign.verify(signature);
	}

	/**
		PrivateKey prkey =  generatePrivate("-----BEGIN PRIVATE KEY-----\n"
	            + "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAM7t8Ub1DP+B91NJ\n"
	            + "nC45zqIvd1QXkQ5Ac1EJl8mUglWFzUyFbhjSuF4mEjrcecwERfRummASbLoyeMXl\n"
	            + "eiPg7jvSaz2szpuV+afoUo9c1T+ORNUzq31NvM7IW6+4KhtttwbMq4wbbPpBfVXA\n"
	            + "IAhvnLnCp/VyY/npkkjAid4c7RoVAgMBAAECgYBcCuy6kj+g20+G5YQp756g95oN\n"
	            + "dpoYC8T/c9PnXz6GCgkik2tAcWJ+xlJviihG/lObgSL7vtZMEC02YXdtxBxTBNmd\n"
	            + "upkruOkL0ElIu4S8CUwD6It8oNnHFGcIhwXUbdpSCr1cx62A0jDcMVgneQ8vv6vB\n"
	            + "/YKlj2dD2SBq3aaCYQJBAOvc5NDyfrdMYYTY+jJBaj82JLtQ/6K1vFIwdxM0siRF\n"
	            + "UYqSRA7G8A4ga+GobTewgeN6URFwWKvWY8EGb3HTwFkCQQDgmKtjjJlX3BotgnGD\n"
	            + "gdxVgvfYG39BL2GnotSwUbjjce/yZBtrbcClfqrrOWWw7lPcX1d0v8o3hJfLF5dT\n"
	            + "6NAdAkA8qAQYUCSSUwxJM9u0DOqb8vqjSYNUftQ9dsVIpSai+UitEEx8WGDn4SKd\n"
	            + "V8kupy/gJlau22uSVYI148fJSCGRAkBz+GEHFiJX657YwPI8JWHQBcBUJl6fGggi\n"
	            + "t0F7ibceOkbbsjU2U4WV7sHyk8Cei3Fh6RkPf7i60gxPIe9RtHVBAkAnPQD+BmND\n"
	            + "By8q5f0Kwtxgo2+YkxGDP5bxDV6P1vd2C7U5/XxaN53Kc0G8zu9UlcwhZcQ5BljH\n"
	            + "N24cUWZOo+60\n"
	            + "-----END PRIVATE KEY-----");
	 * @return
	 * @throws Exception
	 */
	private static PrivateKey generatePrivate(String key) throws Exception {

		byte[] keyBytes = loadKeyFromString(key);
		if (key.contains(" EC ")) {
			return KeyFactory.getInstance("EC", "BC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		}
		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
	}

	static PublicKey generatePublic(Certificate certificate) throws Exception {
		return certificate.getPublicKey();
	}
	
	static PublicKey generatePublic(String key) throws Exception {

		byte[] keyBytes = loadKeyFromString(key);
		if (key.contains(" EC ")) {
			return KeyFactory.getInstance("EC", "BC").generatePublic(new X509EncodedKeySpec(keyBytes));
		}
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
	}
	
	/**
		Certificate cert = generateCertificate("-----BEGIN CERTIFICATE-----\r\n"
				+ "MIICIjCCAcmgAwIBAgIRAL4F0GnBtCPxQRgUU0uvMVEwCgYIKoZIzj0EAwIwcTEL\r\n"
				+ "MAkGA1UEBhMCU0cxGTAXBgNVBAoMEFBvd2VyIEF1dG9tYXRpb24xDjAMBgNVBAsM\r\n"
				+ "BUJVNTAwMQswCQYDVQQIDAJOQTEWMBQGA1UEAwwNUEFST09UQ0EgRkZGRjESMBAG\r\n"
				+ "A1UEBwwJQWxleGFuZHJhMB4XDTIwMDkyNzAyMTIyOVoXDTMwMDkyNzAzMTIyOVow\r\n"
				+ "cTELMAkGA1UEBhMCU0cxGTAXBgNVBAoMEFBvd2VyIEF1dG9tYXRpb24xDjAMBgNV\r\n"
				+ "BAsMBUJVNTAwMQswCQYDVQQIDAJOQTEWMBQGA1UEAwwNUEFST09UQ0EgRkZGRjES\r\n"
				+ "MBAGA1UEBwwJQWxleGFuZHJhMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwYpE\r\n"
				+ "jfK1cDEbDwbn6mc6M8AI2N2j5yiG6E0KIt8BI1jLPKP8BUE7BZvVjlLVVUsVmdYi\r\n"
				+ "QSQJm6GUtANIV3V+V6NCMEAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUYPtt\r\n"
				+ "6x7XitoZRrI73i2G2Y57UTMwDgYDVR0PAQH/BAQDAgGGMAoGCCqGSM49BAMCA0cA\r\n"
				+ "MEQCICJI8XnvdkfKcD2WsatohMFVaPe5ctVEbVTDNMOPaDr9AiA5pDQAlEIuFjyD\r\n"
				+ "ulDUqPmt2SKNz1SA1PFfBelT9sES8A==\r\n"
				+ "-----END CERTIFICATE-----");
	 * @return
	 * @throws Exception
	 */
	public static Certificate generateCertificate(String key) throws Exception {

		byte[] keyBytes = loadKeyFromString(key);
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		return certFactory.generateCertificate(new ByteArrayInputStream(keyBytes));
	}
	
	private static PublicKey generatePublic(PrivateKey privateKey) throws Exception {

		RSAPrivateCrtKey privk = (RSAPrivateCrtKey)privateKey;
	    RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
	    KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	/**
		String privateKey = "-----BEGIN PRIVATE KEY-----\n"
	            + "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAM7t8Ub1DP+B91NJ\n"
	            + "nC45zqIvd1QXkQ5Ac1EJl8mUglWFzUyFbhjSuF4mEjrcecwERfRummASbLoyeMXl\n"
	            + "eiPg7jvSaz2szpuV+afoUo9c1T+ORNUzq31NvM7IW6+4KhtttwbMq4wbbPpBfVXA\n"
	            + "IAhvnLnCp/VyY/npkkjAid4c7RoVAgMBAAECgYBcCuy6kj+g20+G5YQp756g95oN\n"
	            + "dpoYC8T/c9PnXz6GCgkik2tAcWJ+xlJviihG/lObgSL7vtZMEC02YXdtxBxTBNmd\n"
	            + "upkruOkL0ElIu4S8CUwD6It8oNnHFGcIhwXUbdpSCr1cx62A0jDcMVgneQ8vv6vB\n"
	            + "/YKlj2dD2SBq3aaCYQJBAOvc5NDyfrdMYYTY+jJBaj82JLtQ/6K1vFIwdxM0siRF\n"
	            + "UYqSRA7G8A4ga+GobTewgeN6URFwWKvWY8EGb3HTwFkCQQDgmKtjjJlX3BotgnGD\n"
	            + "gdxVgvfYG39BL2GnotSwUbjjce/yZBtrbcClfqrrOWWw7lPcX1d0v8o3hJfLF5dT\n"
	            + "6NAdAkA8qAQYUCSSUwxJM9u0DOqb8vqjSYNUftQ9dsVIpSai+UitEEx8WGDn4SKd\n"
	            + "V8kupy/gJlau22uSVYI148fJSCGRAkBz+GEHFiJX657YwPI8JWHQBcBUJl6fGggi\n"
	            + "t0F7ibceOkbbsjU2U4WV7sHyk8Cei3Fh6RkPf7i60gxPIe9RtHVBAkAnPQD+BmND\n"
	            + "By8q5f0Kwtxgo2+YkxGDP5bxDV6P1vd2C7U5/XxaN53Kc0G8zu9UlcwhZcQ5BljH\n"
	            + "N24cUWZOo+60\n"
	            + "-----END PRIVATE KEY-----";
		loadKeyFromString(privateKey)
	 * @param key
	 * @return
	 */
	static byte[] loadKeyFromString(String key) {
		return Base64.getDecoder().decode(
				key
				.replaceAll("\r*\n", "")
				.replace("\r", "")
				.replaceAll("\\s+","")
				.replaceAll("-----([^\\-])+-----", "")
				);
	}
	
	public static void main1(String[] args) throws Exception {

//		PrivateKey prkey =  generatePrivate("-----BEGIN PRIVATE KEY-----\n"
//	            + "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAM7t8Ub1DP+B91NJ\n"
//	            + "nC45zqIvd1QXkQ5Ac1EJl8mUglWFzUyFbhjSuF4mEjrcecwERfRummASbLoyeMXl\n"
//	            + "eiPg7jvSaz2szpuV+afoUo9c1T+ORNUzq31NvM7IW6+4KhtttwbMq4wbbPpBfVXA\n"
//	            + "IAhvnLnCp/VyY/npkkjAid4c7RoVAgMBAAECgYBcCuy6kj+g20+G5YQp756g95oN\n"
//	            + "dpoYC8T/c9PnXz6GCgkik2tAcWJ+xlJviihG/lObgSL7vtZMEC02YXdtxBxTBNmd\n"
//	            + "upkruOkL0ElIu4S8CUwD6It8oNnHFGcIhwXUbdpSCr1cx62A0jDcMVgneQ8vv6vB\n"
//	            + "/YKlj2dD2SBq3aaCYQJBAOvc5NDyfrdMYYTY+jJBaj82JLtQ/6K1vFIwdxM0siRF\n"
//	            + "UYqSRA7G8A4ga+GobTewgeN6URFwWKvWY8EGb3HTwFkCQQDgmKtjjJlX3BotgnGD\n"
//	            + "gdxVgvfYG39BL2GnotSwUbjjce/yZBtrbcClfqrrOWWw7lPcX1d0v8o3hJfLF5dT\n"
//	            + "6NAdAkA8qAQYUCSSUwxJM9u0DOqb8vqjSYNUftQ9dsVIpSai+UitEEx8WGDn4SKd\n"
//	            + "V8kupy/gJlau22uSVYI148fJSCGRAkBz+GEHFiJX657YwPI8JWHQBcBUJl6fGggi\n"
//	            + "t0F7ibceOkbbsjU2U4WV7sHyk8Cei3Fh6RkPf7i60gxPIe9RtHVBAkAnPQD+BmND\n"
//	            + "By8q5f0Kwtxgo2+YkxGDP5bxDV6P1vd2C7U5/XxaN53Kc0G8zu9UlcwhZcQ5BljH\n"
//	            + "N24cUWZOo+60\n"
//	            + "-----END PRIVATE KEY-----");
//		
//		generateCertificate("-----BEGIN CERTIFICATE-----\r\n"
//				+ "MIICIjCCAcmgAwIBAgIRAL4F0GnBtCPxQRgUU0uvMVEwCgYIKoZIzj0EAwIwcTEL\r\n"
//				+ "MAkGA1UEBhMCU0cxGTAXBgNVBAoMEFBvd2VyIEF1dG9tYXRpb24xDjAMBgNVBAsM\r\n"
//				+ "BUJVNTAwMQswCQYDVQQIDAJOQTEWMBQGA1UEAwwNUEFST09UQ0EgRkZGRjESMBAG\r\n"
//				+ "A1UEBwwJQWxleGFuZHJhMB4XDTIwMDkyNzAyMTIyOVoXDTMwMDkyNzAzMTIyOVow\r\n"
//				+ "cTELMAkGA1UEBhMCU0cxGTAXBgNVBAoMEFBvd2VyIEF1dG9tYXRpb24xDjAMBgNV\r\n"
//				+ "BAsMBUJVNTAwMQswCQYDVQQIDAJOQTEWMBQGA1UEAwwNUEFST09UQ0EgRkZGRjES\r\n"
//				+ "MBAGA1UEBwwJQWxleGFuZHJhMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwYpE\r\n"
//				+ "jfK1cDEbDwbn6mc6M8AI2N2j5yiG6E0KIt8BI1jLPKP8BUE7BZvVjlLVVUsVmdYi\r\n"
//				+ "QSQJm6GUtANIV3V+V6NCMEAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUYPtt\r\n"
//				+ "6x7XitoZRrI73i2G2Y57UTMwDgYDVR0PAQH/BAQDAgGGMAoGCCqGSM49BAMCA0cA\r\n"
//				+ "MEQCICJI8XnvdkfKcD2WsatohMFVaPe5ctVEbVTDNMOPaDr9AiA5pDQAlEIuFjyD\r\n"
//				+ "ulDUqPmt2SKNz1SA1PFfBelT9sES8A==\r\n"
//				+ "-----END CERTIFICATE-----");

		/*PublicKey plKey = generatePublic(prkey);

		String signature = generateSignature("test message".getBytes(StandardCharsets.UTF_8), prkey);
		System.out.println(verifySignature("test message".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(signature), plKey));*/

		/*Base64.Encoder encoder = Base64.getEncoder();
		PEMReader pemReader = new PEMReader(new FileReader("D://server.key"));
		Security.addProvider(new BouncyCastleProvider());
		KeyPair keyPair = (KeyPair) pemReader.readObject();
		Signature signature = Signature.getInstance("SHA256withECDSA");
		signature.initSign(keyPair.getPrivate());
		signature.update("ff".getBytes());
		String signedRequest = encoder.encodeToString(signature.sign());
		System.out.println(signedRequest);

		StringWriter output = new StringWriter();
		PemObject pkPemObject = new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded());
		PemWriter pemWriter = new PemWriter(output);
		pemWriter.writeObject(pkPemObject);
		pemWriter.close();
		System.out.println(output.getBuffer());*/

		/*PEMReader pemReader = new PEMReader(new FileReader("D://sv-crt-new.crt"));
		Security.addProvider(new BouncyCastleProvider());
		X509CertificateObject cert = (X509CertificateObject) pemReader.readObject();
		PublicKey pk = cert.getPublicKey();
		StringWriter output = new StringWriter();
		PemObject pkPemObject = new PemObject("PUBLIC KEY", pk.getEncoded());
		PemWriter pemWriter = new PemWriter(output);
		pemWriter.writeObject(pkPemObject);
		pemWriter.close();
		System.out.println(output.getBuffer());*/
		
		
		
//		String payload = "{\"p1\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNaRENDQWdtZ0F3SUJBZ0lRUHl6L3hsM2k1Mk54WjFWZC9sdjFJREFLQmdncWhrak9QUVFEQWpCZk1Rc3cKQ1FZRFZRUUdFd0pUUnpFTE1Ba0dBMVVFQ2d3Q1VFRXhEakFNQmdOVkJBc01CVUpWTlRBd01Rc3dDUVlEVlFRSQpEQUpPUVRFU01CQUdBMVVFQXd3SlVFRkRRU0JHUmtaR01SSXdFQVlEVlFRSERBbEJiR1Y0WVc1a2NtRXdIaGNOCk1qRXdOakV4TURRd09UQXhXaGNOTWpJd05qRXhNRFV3T1RBeFdqQnRNUXN3Q1FZRFZRUUdFd0pUUnpFTE1Ba0cKQTFVRUNBd0NUa0V4RWpBUUJnTlZCQWNNQ1ZOcGJtZGhjRzl5WlRFTE1Ba0dBMVVFQ2d3Q1VFRXhEakFNQmdOVgpCQXNNQlVKVk5UQXdNU0F3SGdZRFZRUUREQmR6WlhKMlpYSXVhR1JpYzIxaGNuUm9iMjFsTG1OdmJUQjJNQkFHCkJ5cUdTTTQ5QWdFR0JTdUJCQUFpQTJJQUJCTVpncVpySTJSVjN3OWluRnpSYTVnU2lPSTB5b0Z5U1cvYzBvUUgKd0lhWFdjdlU2aWhXUlJsOGlxWlh3RWtYdTgvN2dqV1VuL2lYOXRPOXJXaXVCSXU4eENDTzdrN2FSaDloY3BaZwpoa0tkSjhCSzFFeVVqM0JqcDFZWnI5cDF5Nk44TUhvd0NRWURWUjBUQkFJd0FEQWZCZ05WSFNNRUdEQVdnQlRFCmV1ZVhJQTJTdUVFSlRJd29xTVZoQmhtRUxqQWRCZ05WSFE0RUZnUVVuTElyRncxb2kwWWtGY01Rb2V5UFRxeTUKY1E4d0RnWURWUjBQQVFIL0JBUURBZ1dnTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQlFjRApBakFLQmdncWhrak9QUVFEQWdOSkFEQkdBaUVBcEg2NkNRRHlDeVFUYkZnVUhlRThyK0lYelBvYU9HdFNqR2duCm9yQ0lXNXNDSVFDektsbE1GNStiRE9IVlphZTdpYkFlZzlrcXBWR1U1R2lMYlF0emg5RDhFQT09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K\",\"id\":\"BIERWXAABMADGAFHAA\",\"cmd\":\"ACT\"}";
//		System.out.println("payload: " + payload);
//		String sig = initSignedRequest("D://server_master.key", payload);
//		System.out.println("Sig: " + sig);
//
//		FileReader fileReader = new FileReader("D://master.csr");
//		PemReader pemReader = new PemReader(fileReader);
//		PKCS10CertificationRequest csr =
//				new PKCS10CertificationRequest(pemReader.readPemObject().getContent());
//		pemReader.close();
//		fileReader.close();
//		StringWriter output = new StringWriter();
//		PemWriter pemWriter = new PemWriter(output);
//
//		PemObject pkPemObject = new PemObject("PUBLIC KEY", csr.getPublicKey().getEncoded());
//		pemWriter.writeObject(pkPemObject);
//		pemWriter.close();
//		System.out.println(output.getBuffer());
//
//
//		Signature ecdsaVerify = Signature.getInstance("ecdsa-with-SHA256");
//		ecdsaVerify.initVerify(csr.getPublicKey());
//		ecdsaVerify.update(payload.getBytes("UTF-8"));
//		boolean result1 = ecdsaVerify.verify(Base64.getDecoder().decode(sig));
//		System.out.println(result1);
		
		String csrPath = "D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\server.csr";
		System.out.println(getSignatureAlgorithm(csrPath));
		System.out.println(getKeyType("D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\server.csr"));
		System.out.println(getKeyType("D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\CSR.csr"));

	}
	
	public static String getKeyType(String csrPath) {
		
		// check EC/RSA
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			CMD.exec("openssl req -text -noout -in " + csrPath + " -verify 2>&1", null, bos);
			String str = bos.toString();
			if (str.contains("ASN1 OID:")) {
				Matcher m = Pattern.compile("ASN1 OID: ([a-zA-Z0-9]+)").matcher(str);
				m.find();
				return (str.contains("NIST CURVE") ? "EC-" : "") +  m.group(1).trim();
			}
			if (str.contains("RSA Public-Key")) {
				Matcher m = Pattern.compile("RSA Public-Key: \\(([0-9]+) ").matcher(str);
				m.find();
				return "RSA" + m.group(1).trim();
			}
			return null;
		} catch (Exception e) {
			LOG.error("getKeyType fail: ", e);
		}
		return null;
	}
	
	public static String getSignatureAlgorithm(String csrPath) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			CMD.exec("openssl req -text -noout -in " + csrPath + " -verify 2>&1 | grep \"Signature Algorithm\"", null, bos);
			String alg = bos.toString().replace("Signature Algorithm:", "").trim();
			if (alg.toUpperCase().contains("-WITH-")) {
				return alg.toUpperCase().replaceAll("([a-zA-Z0-9]+)-WITH-([a-zA-Z0-9]+)", "$2with$1");	
			}
			return alg;
		} catch (Exception e) {
			LOG.error("getSignatureAlgorithm fail: ", e);
		}
		return null;
	}
	
	public static boolean validateServerKeyAndCsrKey(String keyPath, String csrPath) {
		String str1 = "";
		String str2 = "";
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			CMD.exec("openssl pkey -in " + keyPath + " -pubout -outform pem | sha256sum", null, bos);
			str1 = bos.toString().trim();
		} catch (Exception e) {
			LOG.error("getKeyType fail: ", e);
		}
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			CMD.exec("openssl req -in " + csrPath + " -pubkey -noout -outform pem | sha256sum", null, bos);
			str2 = bos.toString().trim();
		} catch (Exception e) {
			LOG.error("getSignatureAlgorithm fail: ", e);
		}
		return StringUtils.equals(str1, str2);
	}
	
	public static String getCSRInfo(String csrPath) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			CMD.exec("openssl req -text -noout -in " + csrPath + " -verify 2>&1", null, bos);
			return bos.toString();
		} catch (Exception e) {
			LOG.error("getCSRInfo fail: ", e);
		}
		return null;
	}

	public static String initSignedRequest(String privateKeyPath, String payload, String signatureAlgorithm) {
		
		try {
			LOG.debug("original initSignedRequest, privateKeyPath: {}, payload: {}, signatureAlgorithm: {}", privateKeyPath, payload, signatureAlgorithm);
			
			if (!"true".equalsIgnoreCase(AppProps.get("USE_VENDOR_KEY", "false"))) {
				signatureAlgorithm = "SHA256withECDSA";
				privateKeyPath = AppProps.get("evs.pa.privatekey.path"); 
			}
			
			LOG.debug("initSignedRequest, privateKeyPath: {}, payload: {}, signatureAlgorithm: {}", privateKeyPath, payload, signatureAlgorithm);
			
			Base64.Encoder encoder = Base64.getEncoder();
			PEMReader pemReader = new PEMReader(new FileReader(privateKeyPath));
			Security.addProvider(new BouncyCastleProvider());
			KeyPair keyPair = (KeyPair) pemReader.readObject();
			Signature signature = Signature.getInstance(signatureAlgorithm);
			signature.initSign(keyPair.getPrivate());
			signature.update(payload.getBytes());
			return encoder.encodeToString(signature.sign());
		} catch (Exception e) {
			LOG.error("initSignedRequest fail: ", e);
		}
		return "";
	}

	public static boolean verifySign(String csrPath, String payload, String sig, String signatureAlgorithm) {
		if (!"true".equalsIgnoreCase(AppProps.get("USE_VENDOR_KEY", "false"))) {
			signatureAlgorithm = "SHA256withECDSA";
		}
		
		LOG.debug("VerifySign, csrPath: {}, payload: {}, signHex: {}, signatureAlgorithm: {}", csrPath, payload, sig, signatureAlgorithm);
		try(FileReader fileReader = new FileReader(csrPath);
			PemReader pemReader = new PemReader(fileReader)) {
			PKCS10CertificationRequest csr = new PKCS10CertificationRequest(pemReader.readPemObject().getContent());
			Signature ecdsaVerify = Signature.getInstance(signatureAlgorithm);
			ecdsaVerify.initVerify(csr.getPublicKey());
			ecdsaVerify.update(payload.getBytes("UTF-8"));
			return ecdsaVerify.verify(Base64.getDecoder().decode(sig));
		} catch (Exception e) {
			LOG.error("Verify sign fail: ", e);
			return false;
		}
	}
	
	public static void main6(String[] args) throws Exception {
		// MGQCMDj9+UUcehFegfjXFmLE3hUYzwKeflf58HCWlUqPQzffeAnFkZ0agaW9PF1sCEDMGwIwW4hY2GEXNe5sUEMn8cmY7IYbxyEfXAPMKpzNQ+vkgJL29cGgPi37aVA09/NbT5hZ
		// MGUCMQCNNW9oRWCE3Lj2qF3jF/BlrQSK2tnc1lh00w/4oda8AoqkkbhjAySr36ALrfjJJI0CMDUM3e3rHpChFeCvpRu9D5pdpP5Fy2hi4v6OaCHtP/RaWlcHUqmwZXMw+z0BN4CxkQ==
		
		
		boolean val = validateServerKeyAndCsrKey("D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\server.key", "D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\server.csr");
		System.out.println(val);
		
		String payload = "{\"id\":\"89049032000001000000128255791124\",\"cmd\":\"PW1\"}";
		System.out.println(initSignedRequest("D:/home/evs-data/master_key_vendor_1_SHA256withECDSA_1712724921565.key", payload, "SHA256withECDSA"));
		
		Certificate c = generateCertificate("-----BEGIN CERTIFICATE-----\r\n"
				+ "MIICXDCCAgOgAwIBAgIQYNmtuKXYxTHMxPob0dkMQTAKBggqhkjOPQQDAjBfMQsw\r\n"
				+ "CQYDVQQGEwJTRzELMAkGA1UECgwCUEExDjAMBgNVBAsMBUJVNTAwMQswCQYDVQQI\r\n"
				+ "DAJOQTESMBAGA1UEAwwJUEFDQSBGRkZGMRIwEAYDVQQHDAlBbGV4YW5kcmEwHhcN\r\n"
				+ "MjEwNzI2MDE0NjU0WhcNMjMwNzI2MDI0NjU0WjBnMQswCQYDVQQGEwJTRzELMAkG\r\n"
				+ "A1UECAwCTkExEjAQBgNVBAcMCVNpbmdhcG9yZTELMAkGA1UECgwCUEExDjAMBgNV\r\n"
				+ "BAsMBUJVNTAwMRowGAYDVQQDDBFtYXN0ZXIuZXZzLmNvbS5zZzB2MBAGByqGSM49\r\n"
				+ "AgEGBSuBBAAiA2IABK24Ek7o762rmjOlV+nRYG/qqHhuxYPa+PGTjw2KtdzH20w2\r\n"
				+ "9GwbwLuLhZn9sa1/q243hOBQrmM9Lt+e37j0BR36UlF30EJ5gnE+wu4TcrJ2Njcs\r\n"
				+ "kAbzdsFWQ56TfhA5F6N8MHowCQYDVR0TBAIwADAfBgNVHSMEGDAWgBTEeueXIA2S\r\n"
				+ "uEEJTIwoqMVhBhmELjAdBgNVHQ4EFgQUituPGcm7xBe4tUurRPm7edMtGgwwDgYD\r\n"
				+ "VR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAKBggq\r\n"
				+ "hkjOPQQDAgNHADBEAiAFZfMhH0VekhKPoJmQrJSV/dgnZvOfWtDTa73puF8p2wIg\r\n"
				+ "EgWEufejkciy1sjPyig/le8QEKhUQMdjwGYwF1MIHf0=\r\n"
				+ "-----END CERTIFICATE-----");
		PublicKey publicKey = c.getPublicKey();
		System.out.println(publicKey);
		
		StringWriter writer = new StringWriter();
		PemWriter pemWriter = new PemWriter(writer);
		pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
		pemWriter.flush();
		pemWriter.close();
		
		System.out.println(writer.toString());
		System.out.println("---------------");
		
//		PEMReader pemReader = new PEMReader(new FileReader("D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\server.key"));
//		Security.addProvider(new BouncyCastleProvider());
//		pemReader.readObject();
//		KeyPair keyPair = (KeyPair) pemReader.readObject();
//		PublicKey publicKey = keyPair.getPublic();
//		System.out.println(publicKey);
		
//		StringWriter writer = new StringWriter();
//		PemWriter pemWriter = new PemWriter(writer);
//		pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
//		pemWriter.flush();
//		pemWriter.close();
//		
//		System.out.println(writer.toString());
//		publicKey = null;
		
		
		
//		String sig = "MGUCMQCrldDRA9Wh+/8w+t1fRdIZuG9AVdOfDwWZNMPTMysG1gVy/SxQJcRsrYIzoaziMucCMGXiCdxGQHYSV8OZytyKTvJLzuM2B5+g51pbuGu3LT9lSkDqu/ocvqOMtup+Kr5YEw==";
//		payload = "{\"id\":\"BIE2IEYAAMAJWABIAA\",\"cmd\":\"PW1\"}";
//		Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
//		ecdsaVerify.initVerify(publicKey);
//		ecdsaVerify.update(payload.getBytes("UTF-8"));
//		System.out.println(ecdsaVerify.verify(Base64.getDecoder().decode(sig)));
		
		// Certificate certificate = generateCertificate("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNWVENDQWZ5Z0F3SUJBZ0lRR1MvZUp0TTVZNjd6M0VFY1NIVVpKVEFLQmdncWhrak9QUVFEQWpCU01Rc3cKQ1FZRFZRUUdFd0pUUnpFTE1Ba0dBMVVFQ2d3Q1VFRXhEakFNQmdOVkJBc01CVUpWTlRBd01SSXdFQVlEVlFRRApEQWxRUVVOQklFWkdSa1l4RWpBUUJnTlZCQWNNQ1VGc1pYaGhibVJ5WVRBZUZ3MHlNekV3TVRjeE1EVXhNalphCkZ3MHlOREE1TURFeE1UVXhNalphTUcweEN6QUpCZ05WQkFZVEFsTkhNUXN3Q1FZRFZRUUlEQUpPUVRFU01CQUcKQTFVRUJ3d0pVMmx1WjJGd2IzSmxNUXN3Q1FZRFZRUUtEQUpRUVRFT01Bd0dBMVVFQ3d3RlFsVTFNREF4SURBZQpCZ05WQkFNTUYzTmxjblpsY2k1b1pHSnpiV0Z5ZEdodmJXVXVZMjl0TUhZd0VBWUhLb1pJemowQ0FRWUZLNEVFCkFDSURZZ0FFRXhtQ3Btc2paRlhmRDJLY1hORnJtQktJNGpUS2dYSkpiOXpTaEFmQWhwZFp5OVRxS0ZaRkdYeUsKcGxmQVNSZTd6L3VDTlpTZitKZjIwNzJ0YUs0RWk3ekVJSTd1VHRwR0gyRnlsbUNHUXAwbndFclVUSlNQY0dPbgpWaG12Mm5YTG8zd3dlakFKQmdOVkhSTUVBakFBTUI4R0ExVWRJd1FZTUJhQUZOY1A4SWJLVGh1TTd6RlNrVkphCk1ESnNEcXJGTUIwR0ExVWREZ1FXQkJTY3Npc1hEV2lMUmlRVnd4Q2g3STlPckxseER6QU9CZ05WSFE4QkFmOEUKQkFNQ0JhQXdIUVlEVlIwbEJCWXdGQVlJS3dZQkJRVUhBd0VHQ0NzR0FRVUZCd01DTUFvR0NDcUdTTTQ5QkFNQwpBMGNBTUVRQ0lEYjhsTXlGTFpMN3Q5cCtWbzE1SU5oZ2xFYkJ4MWVrUmhsdDNPb2VMVmw5QWlCRzZES0xvT2hPClZNK0x5Q2hqNEJOK0JENWZicTBpbnpCLzB6cWJSWE9GN1E9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==");
		
		Certificate certificate = generateCertificate("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNXRENDQWYyZ0F3SUJBZ0lSQUlJN0ZHWE9iY01XYkg3ZDczdUdXYzR3Q2dZSUtvWkl6ajBFQXdJd1VqRUwKTUFrR0ExVUVCaE1DVTBjeEN6QUpCZ05WQkFvTUFsQkJNUTR3REFZRFZRUUxEQVZDVlRVd01ERVNNQkFHQTFVRQpBd3dKVUVGRFFTQkdSa1pHTVJJd0VBWURWUVFIREFsQmJHVjRZVzVrY21Fd0hoY05NalF4TURFMk1UY3lOVEl4CldoY05NalV3T1RBeE1UZ3lOVEl4V2pCdE1Rc3dDUVlEVlFRR0V3SlRSekVMTUFrR0ExVUVDQXdDVGtFeEVqQVEKQmdOVkJBY01DVk5wYm1kaGNHOXlaVEVMTUFrR0ExVUVDZ3dDVUVFeERqQU1CZ05WQkFzTUJVSlZOVEF3TVNBdwpIZ1lEVlFRRERCZHpaWEoyWlhJdWFHUmljMjFoY25Sb2IyMWxMbU52YlRCMk1CQUdCeXFHU000OUFnRUdCU3VCCkJBQWlBMklBQkJNWmdxWnJJMlJWM3c5aW5GelJhNWdTaU9JMHlvRnlTVy9jMG9RSHdJYVhXY3ZVNmloV1JSbDgKaXFaWHdFa1h1OC83Z2pXVW4vaVg5dE85cldpdUJJdTh4Q0NPN2s3YVJoOWhjcFpnaGtLZEo4QksxRXlVajNCagpwMVlacjlwMXk2TjhNSG93Q1FZRFZSMFRCQUl3QURBZkJnTlZIU01FR0RBV2dCVFhEL0NHeWs0YmpPOHhVcEZTCldqQXliQTZxeFRBZEJnTlZIUTRFRmdRVW5MSXJGdzFvaTBZa0ZjTVFvZXlQVHF5NWNROHdEZ1lEVlIwUEFRSC8KQkFRREFnV2dNQjBHQTFVZEpRUVdNQlFHQ0NzR0FRVUZCd01CQmdnckJnRUZCUWNEQWpBS0JnZ3Foa2pPUFFRRApBZ05KQURCR0FpRUFqU1V1cHJ4a2FMdG14V2FKbjlVTUJnWmFlNFFBclF6Nlh1V1k3QzUzU28wQ0lRQ2hEdjNZCjlzMit1dUZaMVNCcytWckxBbWxBaGNpNnJlVXY2Y29zNFc3bEN3PT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=");
		
		certificate = generateCertificate("MIICXDCCAgOgAwIBAgIQYNmtuKXYxTHMxPob0dkMQTAKBggqhkjOPQQDAjBfMQswCQYDVQQGEwJTRzELMAkGA1UECgwCUEExDjAMBgNVBAsMBUJVNTAwMQswCQYDVQQIDAJOQTESMBAGA1UEAwwJUEFDQSBGRkZGMRIwEAYDVQQHDAlBbGV4YW5kcmEwHhcNMjEwNzI2MDE0NjU0WhcNMjMwNzI2MDI0NjU0WjBnMQswCQYDVQQGEwJTRzELMAkGA1UECAwCTkExEjAQBgNVBAcMCVNpbmdhcG9yZTELMAkGA1UECgwCUEExDjAMBgNVBAsMBUJVNTAwMRowGAYDVQQDDBFtYXN0ZXIuZXZzLmNvbS5zZzB2MBAGByqGSM49AgEGBSuBBAAiA2IABK24Ek7o762rmjOlV+nRYG/qqHhuxYPa+PGTjw2KtdzH20w29GwbwLuLhZn9sa1/q243hOBQrmM9Lt+e37j0BR36UlF30EJ5gnE+wu4TcrJ2NjcskAbzdsFWQ56TfhA5F6N8MHowCQYDVR0TBAIwADAfBgNVHSMEGDAWgBTEeueXIA2SuEEJTIwoqMVhBhmELjAdBgNVHQ4EFgQUituPGcm7xBe4tUurRPm7edMtGgwwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAKBggqhkjOPQQDAgNHADBEAiAFZfMhH0VekhKPoJmQrJSV/dgnZvOfWtDTa73puF8p2wIgEgWEufejkciy1sjPyig/le8QEKhUQMdjwGYwF1MIHf0=");
//		PublicKey publicKey = certificate.getPublicKey();
//		StringWriter writer = new StringWriter();
//		PemWriter pemWriter = new PemWriter(writer);
//		pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
//		pemWriter.flush();
//		pemWriter.close();
//		
//		System.out.println(writer.toString());
		
		String sig = "MGYCMQCCgVZ2GAXCoqwJfkuUJmClV5RCSaa+pDaMFE0QWO/HiLlHBbMzapCTWr1P9XI6UbUCMQC5ZywaWmiN/TC/i6uXwOUaVRjNR8W5q138n3ihusm+cH+XZUyKVOxaKbINWbTTszg=";
		payload = "{\"p1\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNXRENDQWYyZ0F3SUJBZ0lSQUlJN0ZHWE9iY01XYkg3ZDczdUdXYzR3Q2dZSUtvWkl6ajBFQXdJd1VqRUwKTUFrR0ExVUVCaE1DVTBjeEN6QUpCZ05WQkFvTUFsQkJNUTR3REFZRFZRUUxEQVZDVlRVd01ERVNNQkFHQTFVRQpBd3dKVUVGRFFTQkdSa1pHTVJJd0VBWURWUVFIREFsQmJHVjRZVzVrY21Fd0hoY05NalF4TURFMk1UY3lOVEl4CldoY05NalV3T1RBeE1UZ3lOVEl4V2pCdE1Rc3dDUVlEVlFRR0V3SlRSekVMTUFrR0ExVUVDQXdDVGtFeEVqQVEKQmdOVkJBY01DVk5wYm1kaGNHOXlaVEVMTUFrR0ExVUVDZ3dDVUVFeERqQU1CZ05WQkFzTUJVSlZOVEF3TVNBdwpIZ1lEVlFRRERCZHpaWEoyWlhJdWFHUmljMjFoY25Sb2IyMWxMbU52YlRCMk1CQUdCeXFHU000OUFnRUdCU3VCCkJBQWlBMklBQkJNWmdxWnJJMlJWM3c5aW5GelJhNWdTaU9JMHlvRnlTVy9jMG9RSHdJYVhXY3ZVNmloV1JSbDgKaXFaWHdFa1h1OC83Z2pXVW4vaVg5dE85cldpdUJJdTh4Q0NPN2s3YVJoOWhjcFpnaGtLZEo4QksxRXlVajNCagpwMVlacjlwMXk2TjhNSG93Q1FZRFZSMFRCQUl3QURBZkJnTlZIU01FR0RBV2dCVFhEL0NHeWs0YmpPOHhVcEZTCldqQXliQTZxeFRBZEJnTlZIUTRFRmdRVW5MSXJGdzFvaTBZa0ZjTVFvZXlQVHF5NWNROHdEZ1lEVlIwUEFRSC8KQkFRREFnV2dNQjBHQTFVZEpRUVdNQlFHQ0NzR0FRVUZCd01CQmdnckJnRUZCUWNEQWpBS0JnZ3Foa2pPUFFRRApBZ05KQURCR0FpRUFqU1V1cHJ4a2FMdG14V2FKbjlVTUJnWmFlNFFBclF6Nlh1V1k3QzUzU28wQ0lRQ2hEdjNZCjlzMit1dUZaMVNCcytWckxBbWxBaGNpNnJlVXY2Y29zNFc3bEN3PT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=\",\"id\":\"BIE2IEYAAMAJWABIAA\",\"cmd\":\"ACT\"}";
		Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
		ecdsaVerify.initVerify(publicKey);
		ecdsaVerify.update(payload.getBytes("UTF-8"));
		System.out.println(ecdsaVerify.verify(Base64.getDecoder().decode(sig)) == true);
		System.out.println(1);
	}
	
	public static void main3(String[] a) throws Exception {
		org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
		org.apache.http.conn.ssl.TrustStrategy acceptingTrustStrategy = new org.apache.http.conn.ssl.TrustStrategy() {
			public boolean isTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
				return true;
			}
		};
		
		java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
		keyStore.load(new java.io.FileInputStream("F:\\vmw\\Starfish Demo Postman\\NetSeT-User Demo0000000058.pfx"), "79565965".toCharArray());
		
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
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("userId", 33);
		data.put("request", "MIICuDCCAaACAQAwdTEQMA4GA1UEBwwHQmVvZ3JhZDELMAkGA1UEBhMCUlMxHDAaBgNVBAMME1dlYlNlcnZlciBWZWxpa2kgMDExJzAlBgNVBAoMHk5ldFNlVCBHbG9iYWwgU29sdXRpb25zIGQuby5vLjENMAsGA1UECwwEUEtJIDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJTzmkyX9Kw8JJBTeeVsEgA5/955a4JGDncFo8NsuZ/U0dOJYvOR+3q4cgoCmOTCLecOPCsAACsmM43NUkUXaw+95F5JBjFC9FFoEQ0CQeBUzAsxqlE1AcCfNxH7ibEI/WLCfVv5ehYbQFynIFtdxInC/ChiRbIFyglpcYeqF+7kq5I2ioFXo9qF6GkP+Me2r9UIyYdHOV3YqDIbqYeyI/nbBSNk3zpUKtP1TdUYvrGzX5NYB6LnCocQgn0ecOiR9t76HuBtBg1ptKzFkGJe4eWmwDiyt7z+fpPB420xgDauZbwf104T7D7mXTHWY1NyqAPHguvn9zl7A/b6HQSo0wMCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAiZod7V6kjWuSgK+j1/vLjUu/9lLcKVLHTLz7IWbX931gtZX0+Utt6ngq3KKu66BMbDUTu7M75zQYOwIrX91fGAFnyzoHjbm33iElxguoSbpWt8dPD3wLMAR+m1vblWv7Fa99e/UT/G3wZj+zBHbIj40AEBK3cbbdvE+bQuwxFYBHYJjHKiujmFmqu0Uahlri4yO0fNhdSPn2sHPJUV+gDd3QOpQrHw8YVXTrhvjp4S+oBRxFjDu7j/iHNEy2XxnysF6n7axJIx2dNQYL5d/QzarJFbFFoYQNE1vbVsARe9wsKjDSSpd5vS9IyQwIwA81sPihseuMlaP8xXG6HpgPow==");
		data.put("certProfileId", 5);
		data.put("caId", 9);
		data.put("notBefore", "2024-12-26");
		data.put("notAfter", "2025-12-26");
		HttpEntity<Object> entity = new HttpEntity<>(data, headers);
		
//		Object x = restTemplate.exchange("https://starfishdemo.local:8443/starfish/certificateRequest", HttpMethod.POST, entity, Object.class).getBody();
//		System.out.println(x);
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Reader reader = new java.io.InputStreamReader(new ByteArrayInputStream("MIICuDCCAaACAQAwdTEQMA4GA1UEBwwHQmVvZ3JhZDELMAkGA1UEBhMCUlMxHDAaBgNVBAMME1dlYlNlcnZlciBWZWxpa2kgMDExJzAlBgNVBAoMHk5ldFNlVCBHbG9iYWwgU29sdXRpb25zIGQuby5vLjENMAsGA1UECwwEUEtJIDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJTzmkyX9Kw8JJBTeeVsEgA5/955a4JGDncFo8NsuZ/U0dOJYvOR+3q4cgoCmOTCLecOPCsAACsmM43NUkUXaw+95F5JBjFC9FFoEQ0CQeBUzAsxqlE1AcCfNxH7ibEI/WLCfVv5ehYbQFynIFtdxInC/ChiRbIFyglpcYeqF+7kq5I2ioFXo9qF6GkP+Me2r9UIyYdHOV3YqDIbqYeyI/nbBSNk3zpUKtP1TdUYvrGzX5NYB6LnCocQgn0ecOiR9t76HuBtBg1ptKzFkGJe4eWmwDiyt7z+fpPB420xgDauZbwf104T7D7mXTHWY1NyqAPHguvn9zl7A/b6HQSo0wMCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAiZod7V6kjWuSgK+j1/vLjUu/9lLcKVLHTLz7IWbX931gtZX0+Utt6ngq3KKu66BMbDUTu7M75zQYOwIrX91fGAFnyzoHjbm33iElxguoSbpWt8dPD3wLMAR+m1vblWv7Fa99e/UT/G3wZj+zBHbIj40AEBK3cbbdvE+bQuwxFYBHYJjHKiujmFmqu0Uahlri4yO0fNhdSPn2sHPJUV+gDd3QOpQrHw8YVXTrhvjp4S+oBRxFjDu7j/iHNEy2XxnysF6n7axJIx2dNQYL5d/QzarJFbFFoYQNE1vbVsARe9wsKjDSSpd5vS9IyQwIwA81sPihseuMlaP8xXG6HpgPow==".getBytes()));
		
		reader = new java.io.InputStreamReader(new ByteArrayInputStream(("-----BEGIN CERTIFICATE REQUEST-----\r\n"
				+ "MIICuDCCAaACAQAwdTEQMA4GA1UEBwwHQmVvZ3JhZDELMAkGA1UEBhMCUlMxHDAaBgNVBAMME1dlYlNlcnZlciBWZWxpa2kgMDExJzAlBgNVBAoMHk5ldFNlVCBHbG9iYWwgU29sdXRpb25zIGQuby5vLjENMAsGA1UECwwEUEtJIDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJTzmkyX9Kw8JJBTeeVsEgA5/955a4JGDncFo8NsuZ/U0dOJYvOR+3q4cgoCmOTCLecOPCsAACsmM43NUkUXaw+95F5JBjFC9FFoEQ0CQeBUzAsxqlE1AcCfNxH7ibEI/WLCfVv5ehYbQFynIFtdxInC/ChiRbIFyglpcYeqF+7kq5I2ioFXo9qF6GkP+Me2r9UIyYdHOV3YqDIbqYeyI/nbBSNk3zpUKtP1TdUYvrGzX5NYB6LnCocQgn0ecOiR9t76HuBtBg1ptKzFkGJe4eWmwDiyt7z+fpPB420xgDauZbwf104T7D7mXTHWY1NyqAPHguvn9zl7A/b6HQSo0wMCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAiZod7V6kjWuSgK+j1/vLjUu/9lLcKVLHTLz7IWbX931gtZX0+Utt6ngq3KKu66BMbDUTu7M75zQYOwIrX91fGAFnyzoHjbm33iElxguoSbpWt8dPD3wLMAR+m1vblWv7Fa99e/UT/G3wZj+zBHbIj40AEBK3cbbdvE+bQuwxFYBHYJjHKiujmFmqu0Uahlri4yO0fNhdSPn2sHPJUV+gDd3QOpQrHw8YVXTrhvjp4S+oBRxFjDu7j/iHNEy2XxnysF6n7axJIx2dNQYL5d/QzarJFbFFoYQNE1vbVsARe9wsKjDSSpd5vS9IyQwIwA81sPihseuMlaP8xXG6HpgPow==\r\n"
				+ "-----END CERTIFICATE REQUEST-----").getBytes()));
		PemReader pemReader = new PemReader(reader);
		PKCS10CertificationRequest csr =
				new PKCS10CertificationRequest(pemReader.readPemObject().getContent());
		System.out.println(csr);
	}
	
	public static void main(String[] args) throws Exception {
		Certificate c = generateCertificate("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNWVENDQWZ5Z0F3SUJBZ0lRR1MvZUp0TTVZNjd6M0VFY1NIVVpKVEFLQmdncWhrak9QUVFEQWpCU01Rc3cKQ1FZRFZRUUdFd0pUUnpFTE1Ba0dBMVVFQ2d3Q1VFRXhEakFNQmdOVkJBc01CVUpWTlRBd01SSXdFQVlEVlFRRApEQWxRUVVOQklFWkdSa1l4RWpBUUJnTlZCQWNNQ1VGc1pYaGhibVJ5WVRBZUZ3MHlNekV3TVRjeE1EVXhNalphCkZ3MHlOREE1TURFeE1UVXhNalphTUcweEN6QUpCZ05WQkFZVEFsTkhNUXN3Q1FZRFZRUUlEQUpPUVRFU01CQUcKQTFVRUJ3d0pVMmx1WjJGd2IzSmxNUXN3Q1FZRFZRUUtEQUpRUVRFT01Bd0dBMVVFQ3d3RlFsVTFNREF4SURBZQpCZ05WQkFNTUYzTmxjblpsY2k1b1pHSnpiV0Z5ZEdodmJXVXVZMjl0TUhZd0VBWUhLb1pJemowQ0FRWUZLNEVFCkFDSURZZ0FFRXhtQ3Btc2paRlhmRDJLY1hORnJtQktJNGpUS2dYSkpiOXpTaEFmQWhwZFp5OVRxS0ZaRkdYeUsKcGxmQVNSZTd6L3VDTlpTZitKZjIwNzJ0YUs0RWk3ekVJSTd1VHRwR0gyRnlsbUNHUXAwbndFclVUSlNQY0dPbgpWaG12Mm5YTG8zd3dlakFKQmdOVkhSTUVBakFBTUI4R0ExVWRJd1FZTUJhQUZOY1A4SWJLVGh1TTd6RlNrVkphCk1ESnNEcXJGTUIwR0ExVWREZ1FXQkJTY3Npc1hEV2lMUmlRVnd4Q2g3STlPckxseER6QU9CZ05WSFE4QkFmOEUKQkFNQ0JhQXdIUVlEVlIwbEJCWXdGQVlJS3dZQkJRVUhBd0VHQ0NzR0FRVUZCd01DTUFvR0NDcUdTTTQ5QkFNQwpBMGNBTUVRQ0lEYjhsTXlGTFpMN3Q5cCtWbzE1SU5oZ2xFYkJ4MWVrUmhsdDNPb2VMVmw5QWlCRzZES0xvT2hPClZNK0x5Q2hqNEJOK0JENWZicTBpbnpCLzB6cWJSWE9GN1E9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==");
		System.out.println(c);
		
		c = generateCertificate("eyJDZXJ0aWZpY2F0ZSI6Ii0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlGSkRDQ0F3eWdBd0lCQWdJSkUvRUpibWxtRG1UaU1BMEdDU3FHU0liM0RRRUJDd1VBTUJJeEVEQU9CZ05WCkJBTU1CMUp2YjNRZ1EwRXdIaGNOTWpVd01qSTNNRGt5T1RRMldoY05NekF3TWpJM01Ea3lPVFEyV2pBNU1Rc3cKQ1FZRFZRUUdFd0pFUlRFUU1BNEdBMVVFQ2d3SFZtVnlhV1J2Y3pFWU1CWUdBMVVFQXd3UFUxTk1JRk5sY25abApjaUJFWlcxdk1IWXdFQVlIS29aSXpqMENBUVlGSzRFRUFDSURZZ0FFRXhtQ3Btc2paRlhmRDJLY1hORnJtQktJCjRqVEtnWEpKYjl6U2hBZkFocGRaeTlUcUtGWkZHWHlLcGxmQVNSZTd6L3VDTlpTZitKZjIwNzJ0YUs0RWk3ekUKSUk3dVR0cEdIMkZ5bG1DR1FwMG53RXJVVEpTUGNHT25WaG12Mm5YTG80SUNBakNDQWY0d0NRWURWUjBUQkFJdwpBREFPQmdOVkhROEJBZjhFQkFNQ0JhQXdIUVlEVlIwbEJCWXdGQVlJS3dZQkJRVUhBd0lHQ0NzR0FRVUZCd01CCk1COEdBMVVkSXdRWU1CYUFGQ1NUZksyRUpub2xnU2UzL3ZOUEVCditOL0NwTUIwR0ExVWREZ1FXQkJTY3Npc1gKRFdpTFJpUVZ3eENoN0k5T3JMbHhEekF3QmdOVkhSOEVLVEFuTUNXZ0k2QWhoaDlvZEhSd09pOHZiRzlqWVd4bwpiM04wTDJOeWJDOVNiMjkwUTBFdVkzSnNNRDRHQ0NzR0FRVUZCd0VCQkRJd01EQXVCZ2dyQmdFRkJRY3dBb1lpCmFIUjBjRG92TDJ4dlkyRnNhRzl6ZEM5allXTmxjblF2VW05dmRFTkJMbU5sY2pCZkJnTlZIU0FFV0RCV01GUUcKRFNzR0FRUUJnOUF6Q2dFREJRSXdRekJCQmdnckJnRUZCUWNDQVJZMWFIUjBjRG92TDJOaExtUm5kSEoxYzNRdQpkR1Z6ZEM1eVpXeHBaV1oyWVd4cFpHRjBhVzl1TG1OdmJTNWlaQzlqY0dOd2N5OHdnYTRHQTFVZEVRU0JwakNCCm80SUpiRzlqWVd4b2IzTjBnaFlxTG5abGNtbGtiM011YVc1MFpYSnVZV3d1WTI5dGdnMHFMblpsY21sa2IzTXUKWTI5dGdob3FMbWx1ZEdWeWJtRnNMbTVsZEhObGRHZHNiMkpoYkM1eWM0SVJLaTV1WlhSelpYUm5iRzlpWVd3dQpjbk9DRW5OMFlYSm1hWE5vWkdWdGJ5NXNiMk5oYklJTEtpNXVaWFJ6WlhRdWNuT0NCeW91Ykc5allXeUNGaW91CmFXNTBaWEp1WVd3dWRtVnlhV1J2Y3k1amIyMHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnSUJBS0QrTU5tMTRZSzcKMEpmL082cGU1RVRFYkQ1NFgwZys1cUU5UDlicGdaQjZyVTh1elp3YnFlaHpWOWVlTnZuTFB2YTJRU2xOc3JTagpncFhXRzF3OUM4NWZkR0NGNGtib2RQVkdzRS95QzNYeFU4N1FaRzBkd0N1eHNISXhtdC8zUHd6cWoycVJ0WURvCjhxajcvNkZodlBLYkU2R3NOakYwQURWVEpFMGtOMXBHQXM4YjF4dzkwaWozN29lbWt0aUZvZnc3WW5pMitwTzkKalpmNlVuSWRMNmNtVnE2L0ljRGcycC83S1htQjhLVEZTR3E2TG43T3lhZTBoSEtpVWUycHJmUjYwdTZYUDJRSAowTUtYSU43OWhQdjBPNG5LMTFSUFNUMElCYXRjakZNYXduL0o2L0ZtM0p3RS9wMTYzbjVFbGVTUEZqd3NYVm5ICnloT3M4VWNkY0ZRQTdMNjVXY0NOazZPMWlvWWZEN0tsdnV4WERrNUViVW02WTd3Ti9uaEo2T1FSdUphbXlIQUYKQ2FqZVVnSGRUaXhvL1UwV2xQRDRTcHFac1ZoUkdUZzF4MFk1ZDFBRkVxZU95NWplUkp0WXVLMHZYbVRNcDlEagp4QXZ5ZFFseEpHUmo3YVNOb29xUitZWU9PL1JLakp5Q0dGVUZCT0wyQUNiOEpvSXVnZHFqQlROUTJzbjh3ejdICk56NnB0RXFuSVg0aHdUOVE2UktiaWdhVlVRMS9hNEFKeVhTSnFkMzIwc2tpejlERjhaQmFOTlIycGRxMVFXTHYKcnhROFJzK284Vk5ITFNHKytZbitPaHdWZkFHUHFQU3l0VXJFZUdvUmttdDROUDY2TG54MDVJc2ZGaW5Yd0hQWQoybWtkZGt1OGxLYUdQZDBHUklQNmZDMHRwd05ZWC90QQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCiJ9");
		System.out.println(c);
	}
}
