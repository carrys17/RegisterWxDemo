package com.example.admin.registerwxdemo;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 用于数据的AES加密，其中AES_KEY为秘钥
 * 
 * @author Administrator
 * 
 */
public class AESUtils {
	private final static String HEX = "0123456789ABCDEF";
	private static final String CBC_PKCS5_PADDING = "AES/CBC/NoPadding";// AES是加密方式
																		// CBC是工作模式
																		// PKCS5Padding是填充模式
	private static final String AES = "AES";// AES 加密
	private static final String SHA1PRNG = "SHA1PRNG";// // SHA1PRNG 强随机种子算法,
														// 要区别4.2以上版本的调用方法

	private final static String AES_IV = "a14521b6c96266hg";
	private final static String AES_KEY = "09f5e8f7fc1a0d27";

	/**
	 * 用于补齐字节，确保字节数为16的倍数
	 * 
	 * @param src
	 * @return
	 */
	public static byte[] multiple(byte[] src) {
		int len = src.length;
		int n = len % 16;
		byte[] ret = new byte[src.length + 16 - n];

		for (int i = 0; i < src.length; i++) {
			ret[i] = src[i];
		}

		for (int i = src.length; i < (16 - n); i++) {
			ret[i] = '\0';
		}
		return ret;
	}

	/**
	 * byte[]转hex
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}

		return stringBuilder.toString();
	}

	/**
	 * 16进制字符串转byte[]
	 * 
	 * @param hexString
	 * @return
	 */
	public static byte[] hexStringToBytes(String hexString) {
		try {
			if (hexString == null || hexString.equals("")) {
				return null;
			}
			hexString = hexString.toUpperCase();
			int length = hexString.length() / 2;
			char[] hexChars = hexString.toCharArray();
			byte[] result = new byte[length];
			for (int i = 0; i < length; i++) {
				int pos = i * 2;
				result[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
			}
			return result;
		} catch (Exception e) {

		}
		return null;

	}

	/**
	 * char转byte
	 * 
	 * @param c
	 * @return
	 */
	public static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	/**
	 * AES加密字符串
	 * 
	 * @param content
	 *            需要被加密的字符串
	 * @param password
	 *            加密需要的密码
	 * @return 密文
	 */
	public static byte[] encrypt(String content) {
		try {
			// KeyGenerator kgen = KeyGenerator.getInstance("AES"); //
			// 创建AES的Key生产者
			// kgen.init(128, new SecureRandom(AES_KEY.getBytes()));//
			// 利用用户密码作为随机数初始化出
			// //128位的key生产者
			// //加密没关系，SecureRandom是生成安全随机数序列，password.getBytes()是种子，只要种子相同，序列就一样，所以解密只要有password就行
			// SecretKey secretKey = kgen.generateKey();// 根据用户密码，生成一个密钥
			// byte[] enCodeFormat = secretKey.getEncoded();//
			// 返回基本编码格式的密钥，如果此密钥不支持编码，则返回null。
			// SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");//
			// 转换为AES专用密钥
			SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);// 创建密码器
			byte[] byteContent = content.getBytes("utf-8");
			byteContent = multiple(byteContent);
			cipher.init(Cipher.ENCRYPT_MODE, key,
					new IvParameterSpec(AES_IV.getBytes()));// 初始化为加密模式的密码器
			byte[] result = cipher.doFinal(byteContent);// 加密

			// return Base64.encodeBase64(result);
			return result;

		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 生成随机数，可以当做动态的密钥 加密和解密的密钥必须一致，不然将不能解密
	 */
	public static String generateKey() {
		try {
			SecureRandom localSecureRandom = SecureRandom.getInstance(SHA1PRNG);
			byte[] bytes_key = new byte[20];
			localSecureRandom.nextBytes(bytes_key);
			String str_key = toHex(bytes_key);
			return str_key;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 对密钥进行处理
	private static byte[] getRawKey(byte[] seed) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance(AES);
		// for android
		SecureRandom sr = null;
		// 在4.2以上版本中，SecureRandom获取方式发生了改变
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			sr = SecureRandom.getInstance(SHA1PRNG, "Crypto");
		} else {
			sr = SecureRandom.getInstance(SHA1PRNG);
		}
		// for Java
		// sr = SecureRandom.getInstance(SHA1PRNG);
		sr.setSeed(seed);
		kgen.init(128, sr); // 256 bits or 128 bits,192bits
		// AES中128位密钥版本有10个加密循环，192比特密钥版本有12个加密循环，256比特密钥版本则有14个加密循环。
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();
		return raw;
	}

	/*
	 * 解密
	 */
	public static byte[] decrypt(byte[] encrypted) throws Exception {
		// byte[] raw = getRawKey(key.getBytes());
		// SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
		// encrypted = Base64.decodeBase64(encrypted);
		SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec,
				new IvParameterSpec(AES_IV.getBytes()));
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}

	// 二进制转字符
	public static String toHex(byte[] buf) {
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * buf.length);
		for (int i = 0; i < buf.length; i++) {
			appendHex(result, buf[i]);
		}
		return result.toString();
	}

	private static void appendHex(StringBuffer sb, byte b) {
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}

}
