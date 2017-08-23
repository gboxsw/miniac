package com.gboxsw.miniac.samples.miniac_persistence_app;

import java.io.File;

import com.gboxsw.miniac.*;
import com.gboxsw.miniac.mqttgateway.MqttGateway;
import com.gboxsw.miniac.mqttutils.MqttFactory;
import com.gboxsw.miniac.mqttutils.MqttFactory.Persistence;

/**
 * Simple miniac-based application demonstrating usage of persistence storage.
 */
public class App {
	public static void main(String[] args) {
		// create application
		Application app = Application.createSimpleApplication();

		// create persistent storage and configure application to store state
		// every 60 seconds
		app.setPersistentStorage(new XmlPersistentStorage(new File("storage.xml")));
		app.setAutosavePeriod(60);

		// create mqtt factory and mqtt gateway associated to the application
		MqttFactory mqttFactory = new MqttFactory();
		mqttFactory.setServerUri("tcp://iot.eclipse.org:1883");
		mqttFactory.setPersistence(Persistence.MEMORY);
		mqttFactory.setAutomaticReconnect(true);
		app.addGateway("mqtt", new MqttGateway(mqttFactory));

		// create module and add it to the application
		SampleModule mod = new SampleModule();
		app.addModule(mod);

		// launch the application
		app.launch();
	}
}
