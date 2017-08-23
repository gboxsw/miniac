package com.gboxsw.miniac.samples.miniac_simple_app;

import com.gboxsw.miniac.*;
import com.gboxsw.miniac.mqttgateway.MqttGateway;
import com.gboxsw.miniac.mqttutils.MqttFactory;
import com.gboxsw.miniac.mqttutils.MqttFactory.Persistence;

/**
 * Simple miniac-based application.
 */
public class App {

	public static void main(String[] args) {
		// create application
		Application app = Application.createSimpleApplication();

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
