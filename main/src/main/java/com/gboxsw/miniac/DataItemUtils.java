package com.gboxsw.miniac;

/**
 * The class contains helper methods for working with data items.
 */
public final class DataItemUtils {

	/**
	 * Returns whether value of the data item is not null and is equal to given
	 * primitive value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param value
	 *            the value.
	 * @return true, if the value of the data item is not null and is equal to
	 *         given value, false otherwise.
	 */
	public static boolean equals(DataItem<? extends Number> dataItem, byte value) {
		Number dataItemValue = dataItem.getValue();
		if (dataItemValue == null) {
			return false;
		} else {
			return dataItemValue.byteValue() == value;
		}
	}

	/**
	 * Returns whether value of the data item is not null and is equal to given
	 * primitive value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param value
	 *            the value.
	 * @return true, if the value of the data item is not null and is equal to
	 *         given value, false otherwise.
	 */
	public static boolean equals(DataItem<? extends Number> dataItem, short value) {
		Number dataItemValue = dataItem.getValue();
		if (dataItemValue == null) {
			return false;
		} else {
			return dataItemValue.shortValue() == value;
		}
	}

	/**
	 * Returns whether value of the data item is not null and is equal to given
	 * primitive value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param value
	 *            the value.
	 * @return true, if the value of the data item is not null and is equal to
	 *         given value, false otherwise.
	 */
	public static boolean equals(DataItem<? extends Number> dataItem, int value) {
		Number dataItemValue = dataItem.getValue();
		if (dataItemValue == null) {
			return false;
		} else {
			return dataItemValue.intValue() == value;
		}
	}

	/**
	 * Returns whether value of the data item is not null and is equal to given
	 * primitive value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param value
	 *            the value.
	 * @return true, if the value of the data item is not null and is equal to
	 *         given value, false otherwise.
	 */
	public static boolean equals(DataItem<? extends Number> dataItem, long value) {
		Number dataItemValue = dataItem.getValue();
		if (dataItemValue == null) {
			return false;
		} else {
			return dataItemValue.longValue() == value;
		}
	}

	/**
	 * Returns whether value of the data item is not null and is equal to given
	 * primitive value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param value
	 *            the value.
	 * @return true, if the value of the data item is not null and is equal to
	 *         given value, false otherwise.
	 */
	public static boolean equals(DataItem<? extends Number> dataItem, double value) {
		Number dataItemValue = dataItem.getValue();
		if (dataItemValue == null) {
			return false;
		} else {
			return dataItemValue.doubleValue() == value;
		}
	}

	/**
	 * Returns whether value of the data item is not null and is equal to given
	 * primitive value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param value
	 *            the value.
	 * @return true, if the value of the data item is not null and is equal to
	 *         given value, false otherwise.
	 */
	public static boolean equals(DataItem<? extends Number> dataItem, float value) {
		Number dataItemValue = dataItem.getValue();
		if (dataItemValue == null) {
			return false;
		} else {
			return dataItemValue.floatValue() == value;
		}
	}

	private DataItemUtils() {

	}
}
