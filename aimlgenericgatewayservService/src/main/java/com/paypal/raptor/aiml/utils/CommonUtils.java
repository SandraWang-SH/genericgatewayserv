package com.paypal.raptor.aiml.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.InflaterInputStream;


/**
 * @author: qingwu
 **/
public class CommonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);

	private static final int LOG_FPTI_MAX_LENGTH = 10 * 1024 * 1024;

	public static <K, V> void putIfNotNull(K key, V value, Map<K, V> map) {
		if (null != value && null != key) {
			map.put(key, value);
		}
	}

	/**
	 * compress data and encode via base64
	 *
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static String compressStringAndBase64Encode(String data) throws IOException {
		/** temporarily annotation
		ByteArrayOutputStream out = new ByteArrayOutputStream(data.length());
		DeflaterOutputStream dout = new DeflaterOutputStream(out);
		dout.write(data.getBytes());
		dout.close();
		byte[] compressedByteArray = out.toByteArray();
		if (compressedByteArray == null) {
			return null;
		}
		String serializedOutput = com.ebay.kernel.util.Base64.encode(compressedByteArray);
		 */
		if(data.length() > LOG_FPTI_MAX_LENGTH){
            LOGGER.error("input data size [{}] is too large " , data.length());
			return "";
        }
		return data;
	}

	public static String deCompressBase64EncodeString(String input) throws IOException {
		byte[] data = com.ebay.kernel.util.Base64.decode(input);
		byte[] readBuffer = new byte[1000];
		ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
		InflaterInputStream inputStream = new InflaterInputStream(arrayInputStream);
		int read = inputStream.read(readBuffer);
		byte[] actuallyRead = Arrays.copyOf(readBuffer, read);
		return new String(actuallyRead, StandardCharsets.UTF_8);
	}
}
