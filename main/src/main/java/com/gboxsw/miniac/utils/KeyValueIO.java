package com.gboxsw.miniac.utils;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import com.gboxsw.miniac.Application;

/**
 * The collection of helper methods simplifying work with key-value pairs.
 */
public class KeyValueIO {

	/**
	 * Private constructor.
	 */
	private KeyValueIO() {

	}

	/**
	 * Reads key-value pairs (map) from a text file. Invalid lines are silently
	 * skipped.
	 * 
	 * @param source
	 *            the source UTF-8 encoded text file.
	 * @return the map with key-value pairs.
	 */
	public static Map<String, String> read(File source) {
		if (source == null) {
			throw new NullPointerException("The source cannot be null.");
		}

		Map<String, String> result = new HashMap<>();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8))) {
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

				result.put(key, value);
			}
		} catch (Exception e) {
			throw new RuntimeException("Loading of key-value pairs failed.", e);
		}

		return result;
	}

	/**
	 * Copy all key-value pairs from a text file to a map. Invalid lines are
	 * silently skipped.
	 * 
	 * @param source
	 *            the source UTF-8 encoded text file.
	 * @param target
	 *            the target (output) map.
	 */
	public static void copy(File source, Map<String, String> target) {
		if (target == null) {
			throw new NullPointerException("The target cannot be null.");
		}

		target.putAll(read(source));
	}

	/**
	 * Copy all key-value pairs from a text file to a map. Invalid lines are
	 * silently skipped.
	 * 
	 * @param source
	 *            the source UTF-8 encoded text file.
	 * @param target
	 *            the target (output) map.
	 * @param typePreserved
	 *            indicates whether type of value associated with the key in the
	 *            target should be preserved.
	 */
	public static void copy(File source, Map<String, Object> target, boolean typePreserved) {
		if (target == null) {
			throw new NullPointerException("The target cannot be null.");
		}

		for (Map.Entry<String, String> kvPair : read(source).entrySet()) {
			String key = kvPair.getKey();

			Object value;
			if (typePreserved) {
				Object oldValue = target.get(key);
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

			target.put(key, value);
		}
	}

	/**
	 * Copy all key-value pairs from a text file to key-value pairs of an
	 * application. Invalid lines are silently skipped.
	 * 
	 * @param source
	 *            the source UTF-8 encoded text file.
	 * @param target
	 *            the target (output) application.
	 * @param typePreserved
	 *            indicates whether type of value associated with the key in the
	 *            target should be preserved.
	 */
	public static void copy(File source, Application target, boolean typePreserved) {
		if (target == null) {
			throw new NullPointerException("The target cannot be null.");
		}

		for (Map.Entry<String, String> kvPair : read(source).entrySet()) {
			String key = kvPair.getKey();

			Object value;
			if (typePreserved) {
				Object oldValue = target.getKeyValue(key);
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

			target.setKeyValue(key, value);
		}
	}

	/**
	 * Writes key-value pairs to a sanitized string. The key-value pairs are
	 * separated by tabs. The key and the value are separated by the sign '='.
	 * All white spaces are also replaced by the space character.
	 * 
	 * @param source
	 *            the source map.
	 * @return the string with key-value pairs.
	 */
	public static String write(Map<String, Object> source) {
		if (source == null) {
			throw new NullPointerException("The source cannot be null.");
		}

		boolean first = true;
		StringBuilder output = new StringBuilder();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			if ((entry.getKey() == null) || (entry.getValue() == null)) {
				continue;
			}

			// append item delimiter
			if (!first) {
				output.append('\t');
			}

			if (entry.getKey().indexOf('=') >= 0) {
				throw new IllegalArgumentException("The key cannot contain the character \'=\'.");
			}

			// append key-value pairs with = as separator
			appendSanitized(entry.getKey().trim(), output);
			output.append('=');
			String value = entry.getValue().toString().trim();
			appendSanitized(value, output);

			first = false;
		}

		return output.toString();
	}

	/**
	 * Reads key-value pairs (map) from a string. Invalid key-value pairs are
	 * silently skipped.
	 * 
	 * @param source
	 *            the string with encoded key-value pairs.
	 * @return the map with key-value pairs.
	 * 
	 */
	public static Map<String, String> read(String source) {
		if (source == null) {
			throw new NullPointerException("The source cannot be null.");
		}

		Map<String, String> result = new HashMap<>();

		int readIdx = 0;
		int kvStartIdx = 0;
		while (readIdx <= source.length()) {
			char c = (readIdx < source.length()) ? source.charAt(readIdx) : '\t';
			if (c == '\t') {
				if (kvStartIdx < readIdx) {
					int sepIdx = source.indexOf('=', kvStartIdx);
					if ((kvStartIdx <= sepIdx) && (sepIdx < readIdx)) {
						result.put(source.substring(kvStartIdx, sepIdx).trim(),
								source.substring(sepIdx + 1, readIdx).trim());
					}
				}

				kvStartIdx = readIdx + 1;
			}

			readIdx++;
		}

		return result;
	}

	/**
	 * Appends encoded string to a string builder.
	 * 
	 * @param s
	 *            the string to be encoded.
	 * @param output
	 *            the output string builder.
	 */
	private static void appendSanitized(String s, StringBuilder output) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n') {
				output.append(' ');
			} else if (c == '\r') {
				output.append(' ');
			} else if (c == '\t') {
				output.append(' ');
			} else {
				output.append(c);
			}
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
	private static Object convertValue(String value, Class<?> type) {
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
