package com.gboxsw.miniac.samples.miniac_simple_app;

import java.util.concurrent.TimeUnit;

import com.gboxsw.miniac.*;
import com.gboxsw.miniac.converters.IntToTextConverter;
import com.gboxsw.miniac.dataitems.LocalDataItem;
import com.gboxsw.miniac.dataitems.MsgDataItem;

public class SampleModule extends Module {

	/**
	 * Data item of type {@link Integer} bound to messages.
	 */
	private final MsgDataItem<Integer> number;

	/**
	 * Data item of type {@link String}. It can be used as a local variable with
	 * change notifications.
	 */
	private final LocalDataItem<String> localString;

	/**
	 * Cancellable handle for a scheduled action.
	 */
	private Cancellable clockSchedule;

	/**
	 * Constructs the module.
	 */
	public SampleModule() {
		number = new MsgDataItem<>("mqtt/miniac/number", "mqtt/miniac/number", new IntToTextConverter(), Integer.class);
		localString = new LocalDataItem<>(false, String.class);
	}

	@Override
	protected void onInitialize() {
		Application app = getApplication();

		app.addDataItem(Application.DATA_GATEWAY, "number", number);
		app.addDataItem(Application.DATA_GATEWAY, "string", localString);

		// notification when value of data item number changes
		app.subscribe(number.getId(), (message) -> {
			System.out.println("Number changed: " + number.getValue());
		});

		// notification when value of data item localString changes
		app.subscribe(localString.getId(), (message) -> {
			System.out.println("String data item changed: " + localString.getValue());
		});

		// handle received mqtt message and change value of data item
		app.subscribe("mqtt/miniac/string", (message) -> {
			System.out.println("Mqtt message received: " + message.getTopic());
			// set value of local variable to content of received message
			localString.requestChange(message.getContent());
		});

		// handle received mqtt message
		app.subscribe("mqtt/miniac/command", (message) -> {
			String command = message.getContent();
			System.out.println("Received command: " + command);

			// exit application
			if ("exit".equalsIgnoreCase(command)) {
				app.publish(new Message(Application.SYSTEM_GATEWAY + "/exit"));
				return;
			}

			// save state of application (note that persistent storage is not
			// configured in this sample)
			if ("save".equalsIgnoreCase(command)) {
				app.publish(new Message(Application.SYSTEM_GATEWAY + "/save"));
				return;
			}

			if ("stop-clock".equalsIgnoreCase(command)) {
				System.out.println("Clock is cancelled.");
				clockSchedule.cancel();
				return;
			}

			if ("number-test".equalsIgnoreCase(command)) {
				System.out.println("Test of changing the number.");
				number.requestChange(123);
				return;
			}
		});

		// handle system messages
		app.subscribe(Application.SYSTEM_GATEWAY + "/#", (message) -> {
			System.out.println("System message: " + message.getTopic());
		});

		// handle message about connection state of mqtt gateway
		app.subscribe("mqtt/$connected", (message) -> {
			System.out.println("MQTT connection state: " + message.getContent());
		});

		// demo of periodically published and locally handled messages
		// create periodically published message to local (echo) gateway
		clockSchedule = app.publishAtFixedRate(new Message(Application.LOCAL_GATEWAY + "/clock"), 0, 1,
				TimeUnit.SECONDS);

		app.subscribe(Application.LOCAL_GATEWAY + "/clock", (message) -> {
			System.out.println("Clock message received at " + System.currentTimeMillis());
		});
	}

}
