package com.gboxsw.miniac;

import com.gboxsw.miniac.converters.BooleanToTextConverter;
import com.gboxsw.miniac.converters.DoubleToTextConverter;
import com.gboxsw.miniac.converters.IntToTextConverter;
import com.gboxsw.miniac.converters.LongToTextConverter;
import com.gboxsw.miniac.converters.StringConverter;

/**
 * Collection of commonly used converters.
 */
public class Converters {

	/**
	 * Default boolean to text converter.
	 */
	public static final BooleanToTextConverter BOOL2TEXT = new BooleanToTextConverter("1", "0", false);

	/**
	 * Default long to text converter.
	 */
	public static final LongToTextConverter LONG2TEXT = new LongToTextConverter();

	/**
	 * Default int to text converter.
	 */
	public static final IntToTextConverter INT2TEXT = new IntToTextConverter();

	/**
	 * Default double to text converter.
	 */
	public static final DoubleToTextConverter DOUBLE2TEXT = new DoubleToTextConverter();

	/**
	 * Default string converter.
	 */
	public static final StringConverter STRING2TEXT = new StringConverter();
}
