package com.gboxsw.miniac.samples.miniac_persistence_app;

import com.gboxsw.miniac.*;
import com.gboxsw.miniac.dataitems.*;
import com.gboxsw.miniac.converters.IntToTextConverter;

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
		number = new MsgDataItem<>("mqtt/miniac/number", new IntToTextConverter(), Integer.class);

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

		app.subscribe(number.getId(), (message) -> {
			System.out.println("Number changed: " + number.getValue());
		});

		app.subscribe(total.getId(), (message) -> {
			System.out.println("Total changed: " + total.getValue());
		});

		app.subscribe("mqtt/miniac/string", (message) -> {
			System.out.println("New value for string received: " + message.getContent());
			persitentString.requestChange(message.getContent());
		});

		app.subscribe(Application.SYSTEM_GATEWAY + "/start", (message) -> {
			System.out.println("Initial value of persistent string: " + persitentString.getValue());
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
