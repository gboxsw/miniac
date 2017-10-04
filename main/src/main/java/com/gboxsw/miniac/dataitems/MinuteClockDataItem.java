package com.gboxsw.miniac.dataitems;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import com.gboxsw.miniac.*;

/**
 * Data item whose value is changed every minute.
 */
public class MinuteClockDataItem extends DataItem<Integer> {

	/**
	 * Cancellable generator of ticks.
	 */
	private Cancellable tickGenerator;

	/**
	 * Constructs the minute clock.
	 */
	public MinuteClockDataItem() {
		super(Integer.class, true);
	}

	/**
	 * Returns hour of day that corresponds to the current value of the data
	 * item.
	 * 
	 * @return the hour of day.
	 */
	public int getHour() {
		Integer minuteOfDay = getValue();
		if (minuteOfDay == null) {
			return -1;
		} else {
			return minuteOfDay / 60;
		}
	}

	/**
	 * Returns minute of hour that corresponds to the current value of the data
	 * item.
	 * 
	 * @return the minute of hour.
	 */
	public int getMinute() {
		Integer minuteOfDay = getValue();
		if (minuteOfDay == null) {
			return -1;
		} else {
			return minuteOfDay % 60;
		}
	}

	@Override
	protected void onActivate(Bundle savedState) {
		Application app = getApplication();
		tickGenerator = app.invokeAtFixedRate(new Runnable() {
			@Override
			public void run() {
				update();
			}
		}, 30, 30, TimeUnit.SECONDS);
	
		update();
	}

	@Override
	protected Integer onSynchronizeValue() {
		Calendar calendar = Calendar.getInstance();
		return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
	}

	@Override
	protected void onValueChangeRequested(Integer newValue) {
		// nothing to do
	}

	@Override
	protected void onSaveState(Bundle outState) {
		// nothing to do
	}

	@Override
	protected void onDeactivate() {
		tickGenerator.cancel();
	}
}
