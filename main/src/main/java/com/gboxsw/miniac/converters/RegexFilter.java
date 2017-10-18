package com.gboxsw.miniac.converters;

import java.util.logging.*;
import java.util.regex.*;

import com.gboxsw.miniac.*;

/**
 * Bidirectional regular expression filter.
 */
public class RegexFilter implements Converter<String, String> {

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(RegexFilter.class.getName());

	/**
	 * The compiled regular expression.
	 */
	private final Pattern pattern;

	/**
	 * Identifier of capturing group used as output value of the filter.
	 */
	private final Object capturingGroup;

	/**
	 * Constructs the filter.
	 * 
	 * @param regex
	 *            the regular expression that captures filtered (output) value.
	 * @param groupName
	 *            the name group that contains filtered (output) value.
	 */
	public RegexFilter(String regex, String groupName) {
		this(regex, (Object) groupName);
		if (groupName == null) {
			throw new NullPointerException("The group name cannot be null.");
		}
	}

	/**
	 * Constructs the filter.
	 * 
	 * @param regex
	 *            the regular expression that captures filtered (output) value.
	 * @param groupIndex
	 *            the index of group that contains filtered (output) value.
	 */
	public RegexFilter(String regex, int groupIndex) {
		this(regex, (Object) groupIndex);
	}

	/**
	 * Constructs the filter.
	 * 
	 * @param regex
	 *            the regular expression used to capture the filtered (output)
	 *            value.
	 * @param capturingGroup
	 *            the identifier of capturing group.
	 */
	private RegexFilter(String regex, Object capturingGroup) {
		this.pattern = Pattern.compile(regex);
		this.capturingGroup = capturingGroup;
	}

	@Override
	public String convertSourceToTarget(String value) {
		if (value == null) {
			return null;
		}

		Matcher matcher = pattern.matcher(new String(value));
		if (!matcher.matches()) {
			logger.log(Level.FINE, "The conversion input does not match the regular expression.");
			return null;
		}

		try {
			String result = null;
			if (capturingGroup instanceof String) {
				result = matcher.group((String) capturingGroup);
			} else {
				result = matcher.group((Integer) capturingGroup);
			}

			if (result != null) {
				return result;
			}
		} catch (Exception e) {
			logger.log(Level.FINE, "Regular expression is matched, but retrieval of capturing group failed.", e);
		}

		return null;
	}

	@Override
	public String convertTargetToSource(String value) {
		return convertSourceToTarget(value);
	}
}
