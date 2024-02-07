package com.pa.evs.utils;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * 
 * @author thanh
 *
 */
public class DESUtil {

	private static DESUtil instance;

	private Cipher cipherEnc;

	private Cipher cipherDec;

	private DESUtil() {
	}

	public byte[] decrypt(byte[] input) {

		try {
			return cipherDec.doFinal(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String decrypt(String input) {

		try {
			return new String(cipherDec.doFinal(Base64.getDecoder().decode(input)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] encrypt(byte[] input) {

		try {
			return cipherEnc.doFinal(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String encrypt(String input) {

		try {
			return Base64.getEncoder().encodeToString(cipherEnc.doFinal(input.getBytes()));
		} catch (Exception e) {
			return null;
			//throw new CommonExeption(e);
		}
	}

	public static DESUtil getInstance() {
		return instance;
	}

	static {
		instance = new DESUtil();
		try {
			final String transformation = "DESede/ECB/PKCS5Padding";
			String desKey = "0987654321abcdef0123456789abcdef0123456789abcdef"; // user value (24 bytes)
			byte[] keyBytes = DatatypeConverter.parseHexBinary(desKey);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("DESede");
			SecretKey key = factory.generateSecret(new DESedeKeySpec(keyBytes));
			instance.cipherEnc = Cipher.getInstance(transformation);
			instance.cipherDec = Cipher.getInstance(transformation);
			instance.cipherEnc.init(Cipher.ENCRYPT_MODE, key);
			instance.cipherDec.init(Cipher.DECRYPT_MODE, key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		//"ab80445f817e3124"
		System.out.println(DESUtil.getInstance().encrypt("http://123.56.250.3/app_login?name=18210102630&password=Sp@12345&phoneid=1"));
		System.out.println(DESUtil.getInstance().encrypt("http://123.56.250.3/app_getarealocks?token=${token}"));
	}
}