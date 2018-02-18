package com.testiot.agent.ibm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.api.APIClient;
import com.ibm.iotf.client.gateway.GatewayCallback;
import com.ibm.iotf.client.gateway.GatewayClient;
import com.ibm.iotf.client.gateway.Notification;
import com.testiot.agent.Data;
import com.testiot.agent.IoTAgent;
import com.testiot.agent.Utils;

import java.text.DecimalFormat;
import java.util.*;

/*
    IBM Watson IoT Agent based on the Watson IoT java sdk samples.
    http://www.eclipse.org/legal/epl-v10.html
 */

public class WatsonIoTAgent extends IoTAgent {

	private static GatewayClient gwClient = null;
	private static APIClient apiClient = null;


	protected void once() {
		 /**
		  * Load device properties
		  */

		try {
			//Instantiate & connect the Gateway by passing the properties file
			gwClient = new GatewayClient(Utils.props);
			gwClient.connect();

            //Pass the above implemented CommandCallback as an argument to this device client
            WatsonIoTAgentListener callback = new WatsonIoTAgentListener();
            gwClient.setGatewayCallback(callback);

            gwClient.subscribeToDeviceCommands(Utils.props.getProperty("Gateway-Type"), Utils.props.getProperty("Gateway-ID"));
			
			/**
			 * We need APIClient to register the devicetype in Watson IoT Platform 
			 */
			Properties options = new Properties();
			options.put("Organization-ID", Utils.props.getProperty("Organization-ID"));
			options.put("id", "app" + (Math.random() * 10000));		
			options.put("Authentication-Method","apikey");
			options.put("API-Key", Utils.props.getProperty("API-Key"));		
			options.put("Authentication-Token", Utils.props.getProperty("API-Token"));
			
			apiClient = new APIClient(options);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

        if (!gwClient.isConnected()) {
            logger.error("connection was not established!");
            return;
        }
	}

    @Override
    protected void exec() {
        try{
            Data thing = new Data();
            long desired = thing.state.desired.counter;
            thing.state.reported.counter = desired;
            thing.state.desired.counter = desired + 1;

            String jsonState = thing.toJson();
            logger.info(System.currentTimeMillis() + ": >>> " + jsonState);

            boolean code = gwClient.publishGatewayEvent("status", jsonState, 2);
            if(!code){
                logger.error("Failed to publish the event...... "+ code);
                System.exit(-1);
            }
            Thread.sleep(10000);
        } catch (InterruptedException | JsonProcessingException e) {
            logger.error("", e);
        }
    }

    //Implement the CommandCallback class to provide the way in which you want the command to be handled
    class WatsonIoTAgentListener implements GatewayCallback {
        private boolean commandReceived = false;

        /**
         * This method is invoked by the library whenever there is command matching the subscription criteria
         */
        @Override
        public void processCommand(com.ibm.iotf.client.gateway.Command cmd) {
            commandReceived = true;
            logger.info("Received command = "+cmd.getData() + ", time = "+cmd.getTimestamp());
//                    +", cmd = "+cmd.getCommand()  +", deviceId = "+cmd.getDeviceId() + ", deviceType = "+cmd.getDeviceType());

        }

        @Override
        public void processNotification(Notification notification) {
            // TODO Auto-generated method stub

        }

        private void clear() {
            commandReceived = false;
        }
    }
}
