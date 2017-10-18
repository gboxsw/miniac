package com.gboxsw.miniac.samples.miniac_persistence_app;

import com.gboxsw.miniac.*;
import com.gboxsw.miniac.dataitems.*;

/**
 * Sample module as a logical group of data items, variables and subscriptions.
 */
public class SampleModule extends Module {

	/**
	 * Data item of type {@link Integer} bound to messages.
	 */
	private final MsgDataItem<Integer> number;

	/**
	 * Data item that accumulates values of the data item {@link #number}.
	 */
	private final AccumulatingDataItem total;

	/**
	 * Data item that persistently stores its last value.
	 */
	private final LocalDataItem<String> persitentString;

	public SampleModule() {
		// number is created as read-only data item - no write topic is
		// specified
		number = new MsgDataItem<>("mqtt/miniac/number", Converters.reverse(Converters.INT_TO_TEXTBYTES),
				Integer.class);

		// the data item "total" accumulates values of the data item "number"
		total = new AccumulatingDataItem(number);

		// create data item that persistently stores its last value
		persitentString = new LocalDataItem<>(true, String.class);
	}

	@Override
	protected void onInitialize() {
		Application app = getApplication();

		app.addDataItem(Application.DATA_GATEWAY, "number", number);
		app.addDataItem(Application.DATA_GATEWAY, "total", total);
		app.addDataItem(Application.DATA_GATEWAY, "string", persitentString);

		// notification when value of data item "number" changes
		app.subscribe(number.getId(), (message) -> {
			System.out.println("Number changed: " + number.getValue());
		});

		// notification when value of data item "total" changes
		app.subscribe(total.getId(), (message) -> {
			System.out.println("Total changed: " + total.getValue());
		});

		// notification when mqtt message with value for "persistentString" is
		// received
		app.subscribe("mqtt/miniac/string", (message) -> {
			System.out.println("New value for \"persistentString\" received: " + message.getContent());
			persitentString.requestChange(message.getContent());
		});

		// notification when application starts
		app.subscribe(Application.SYSTEM_GATEWAY + "/start", (message) -> {
			System.out.println("Initial value of \"persistentString\": " + persitentString.getValue());
			System.out.println("Initial value of \"total\": " + total.getValue());
		});

		// handle received mqtt message with commands
		app.subscribe("mqtt/miniac/command", (message) -> {
			String command = message.getContent();
			System.out.println("Received command: " + command);

			// exit application
			if ("exit".equalsIgnoreCase(command)) {
				app.publish(new Message(Application.SYSTEM_GATEWAY + "/exit"));
				return;
			}
		});
	}

}
