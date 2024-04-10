package com.pa.evs.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.io.pem.PemReader;
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
	private static Certificate generateCertificate(String key) throws Exception {

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

	public static String initSignedRequest(String privateKeyPath, String payload, String signatureAlgorithm) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		
		LOG.debug("original initSignedRequest, privateKeyPath: {}, payload: {}, signatureAlgorithm: {}", privateKeyPath, payload, signatureAlgorithm);
		
		if (!"true".equalsIgnoreCase(AppProps.get("USE_VENDOR_KEY", "false"))) {
			signatureAlgorithm = "SHA256withECDSA";
			privateKeyPath = AppProps.get("evs.pa.privatekey.path"); 
		}
		
		LOG.debug("initSignedRequest, privateKeyPath: {}, payload: {}, signatureAlgorithm: {}", privateKeyPath, payload, signatureAlgorithm);
		
		Base64.Encoder encoder = Base64.getEncoder();
		PEMReader pemReader = new PEMReader(new FileReader(privateKeyPath));
		Security.addProvider(new BouncyCastleProvider());
		pemReader.readObject();
		KeyPair keyPair = (KeyPair) pemReader.readObject();
		Signature signature = Signature.getInstance(signatureAlgorithm);
		signature.initSign(keyPair.getPrivate());
		signature.update(payload.getBytes());
		return encoder.encodeToString(signature.sign());
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
	
	public static void main(String[] args) throws Exception {
		String pl = "{\"id\":\"89049032000001000000128255791124\",\"cmd\":\"PW1\"}";
		System.out.println(initSignedRequest("D:\\BUS\\THN\\pa-evs\\src\\main\\resources\\sv-ca\\server.key", pl, "SHA256withECDSA"));;
	}
}
