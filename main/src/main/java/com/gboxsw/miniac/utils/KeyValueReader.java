package com.gboxsw.miniac.utils;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import com.gboxsw.miniac.Application;

/**
 * Reader of key-value pairs. This is a helper class that simplifies setup of
 * application.
 */
public class KeyValueReader {

	/**
	 * Indicates whether type of loaded value is preserved in case when the key
	 * has already an associated value.
	 */
	private boolean typePreserved = false;

	/**
	 * Charset of input text files.
	 */
	private Charset charset = StandardCharsets.UTF_8;

	/**
	 * Returns whether type of loaded value is preserved in case when the key
	 * has already an associated value.
	 * 
	 * @return true, if the type is preserved, false otherwise.
	 */
	public boolean isTypePreserved() {
		return typePreserved;
	}

	/**
	 * Sets whether type of loaded value is preserved in case when the key has
	 * already an associated value.
	 * 
	 * @param typePreserved
	 *            true, if the type should be preserved, false otherwise.
	 */
	public void setTypePreserved(boolean typePreserved) {
		this.typePreserved = typePreserved;
	}

	/**
	 * Returns charset of input text files.
	 * 
	 * @return the charset.
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Sets the charset of input text files.
	 * 
	 * @param charset
	 *            the charset.
	 */
	public void setCharset(Charset charset) {
		if (charset == null) {
			throw new NullPointerException("Charset cannot be null.");
		}

		this.charset = charset;
	}

	/**
	 * Reads key-value pairs from a text file and put them to key-value pairs of
	 * an application. Invalid key-value pairs in the input file are silently
	 * skipped.
	 * 
	 * @param file
	 *            the source file.
	 * @param output
	 *            the output application.
	 */
	public void readFile(File file, Application output) {
		Map<String, String> kvPairs = new LinkedHashMap<>();
		readFile(file, kvPairs);
		for (Map.Entry<String, String> kvPair : kvPairs.entrySet()) {
			String key = kvPair.getKey();

			Object value;
			if (typePreserved) {
				Object oldValue = output.getKeyValue(key);
				if (oldValue != null) {
					try {
						value = convertValue(kvPair.getValue(), oldValue.getClass());
					} catch (Exception e) {
						throw new RuntimeException("Conversion of value associated to the key " + key + " failed.", e);
					}
				} else {
					value = kvPair.getValue();
				}
			} else {
				value = kvPair.getValue();
			}

			output.setKeyValue(key, value);
		}
	}

	/**
	 * Reads key-value pairs from a text file and put them as entries to a map.
	 * Invalid key-value pairs in the input file are silently skipped.
	 * 
	 * @param file
	 *            the source file.
	 * @param output
	 *            the output map.
	 */
	public void readFile(File file, Map<String, String> output) {
		if (file == null) {
			throw new NullPointerException("File cannot be null.");
		}

		if (output == null) {
			throw new NullPointerException("Output cannot be null.");
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				int separatorIndex = line.indexOf('=');
				if (separatorIndex < 0) {
					continue;
				}

				String key = line.substring(0, separatorIndex).trim();
				if (key.isEmpty()) {
					continue;
				}

				String value = line.substring(separatorIndex + 1).trim();

				output.put(key, value);
			}
		} catch (Exception e) {
			throw new RuntimeException("Loading of key-value pairs failed.", e);
		}
	}

	/**
	 * Converts string value to equivalent value of given type.
	 * 
	 * @param value
	 *            the value to be converted.
	 * @param type
	 *            the type of output value.
	 * @return the converted value.
	 * 
	 * @throws UnsupportedOperationException
	 *             when conversion is not supported.
	 */
	private Object convertValue(String value, Class<?> type) {
		if (type == String.class) {
			return value;
		}

		if (type == Integer.class) {
			return Integer.parseInt(value);
		}

		if (type == Double.class) {
			return Double.parseDouble(value);
		}

		if (type == Byte.class) {
			return Byte.parseByte(value);
		}

		if (type == Short.class) {
			return Short.parseShort(value);
		}

		if (type == Long.class) {
			return Long.parseLong(value);
		}

		if (type == Float.class) {
			return Float.parseFloat(value);
		}

		throw new UnsupportedOperationException("Conversion to " + type.getName() + " is not supported.");
	}
}
