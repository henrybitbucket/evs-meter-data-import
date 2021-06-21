package com.pa.evs.utils;

import com.pa.evs.sv.impl.CommonServiceImpl;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
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

/**
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

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CommonServiceImpl.class);

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
	
	public static void main(String[] args) throws Exception {

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
		
		generateCertificate("-----BEGIN CERTIFICATE-----\r\n"
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
		FileReader fileReader = new FileReader("D://BIERWXAABMAGSAEAAA.csr");
		PemReader pemReader = new PemReader(fileReader);
		PKCS10CertificationRequest csr =
				new PKCS10CertificationRequest(pemReader.readPemObject().getContent());
		pemReader.close();
		fileReader.close();
		StringWriter output = new StringWriter();
		PemWriter pemWriter = new PemWriter(output);

		PemObject pkPemObject = new PemObject("PUBLIC KEY", csr.getPublicKey().getEncoded());
		pemWriter.writeObject(pkPemObject);
		pemWriter.close();
		System.out.println(output.getBuffer());

		String sig = "MGUCMQD4ygjt/jUo5JOzhW1xP22CiyFIlfYFO6bvL0iAEJsIR4ZrxklQkUytMO0K7kkAdm0CMBtndtZIIDy3o+1dp0i/AH8gXUlzyL7d7yk79MQlRX00KQZ9xJ3bZYbPvZTedkuegQ==";
		String message = "{\"id\":\"BIERWXAABMAGSAEAAA\",\"type\":\"MDT\",\"data\":[{\"uid\":\"BIERWXAABMAGSAEAAA\",\"msn\":\"201906000032\",\"kwh\":\"1.1\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"239.6\",\"pf\":\"10.0\",\"dt\":\"2021-06-12T16:29:38\"}]}";
		Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
		ecdsaVerify.initVerify(csr.getPublicKey());
		ecdsaVerify.update(message.getBytes("UTF-8"));
		boolean result1 = ecdsaVerify.verify(Base64.getDecoder().decode(sig));
		System.out.println(result1);

	}

	public static String initSignedRequest(String privateKeyPath, String payload) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Base64.Encoder encoder = Base64.getEncoder();
		PEMReader pemReader = new PEMReader(new FileReader(privateKeyPath));
		Security.addProvider(new BouncyCastleProvider());
		KeyPair keyPair = (KeyPair) pemReader.readObject();
		Signature signature = Signature.getInstance("SHA256withECDSA");
		signature.initSign(keyPair.getPrivate());
		signature.update(payload.getBytes());
		return encoder.encodeToString(signature.sign());
	}

	public static boolean verifySign(String csrPath, String payload, String sig) {
		LOG.debug("VerifySign, csrPath: {}, payload: {}, signHex: {}", csrPath, payload, sig);
		try(FileReader fileReader = new FileReader(csrPath);
			PemReader pemReader = new PemReader(fileReader)) {
			PKCS10CertificationRequest csr = new PKCS10CertificationRequest(pemReader.readPemObject().getContent());
			Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
			ecdsaVerify.initVerify(csr.getPublicKey());
			ecdsaVerify.update(payload.getBytes("UTF-8"));
			return ecdsaVerify.verify(Base64.getDecoder().decode(sig));
		} catch (Exception e) {
			LOG.error("Verify sign fail: ", e);
			return false;
		}
	}
}
