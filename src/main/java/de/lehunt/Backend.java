package de.lehunt;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;


public class Backend {

    private static final  String BROKER = "tcp://192.168.2.184:1883";
    private static final int QOS_EXACTLY_ONCE = 1;
    private final String  clientId = "BackendClient";
    private MqttClient mqttClient;
    private final HintLookupCallback hintLookupCallback;


    public Backend(HintLookupCallback hintLookupCallback){
        this.hintLookupCallback = hintLookupCallback;
    }

    public void start() {

        // Create Mqtt Client and Connect to Broker


		try {
            mqttClient = new MqttClient(BROKER, clientId);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+ BROKER);
            mqttClient.connect();
            System.out.println("Connected");

            OnMessageCallback callback = new OnMessageCallback(mqttClient, hintLookupCallback);
		    // mqttClient.setCallback(callback);

		    mqttClient.subscribe("#", QOS_EXACTLY_ONCE, callback);
            System.out.println("Subscribed to all Topics");

		} catch (MqttException e) {
            System.out.println("Backend could not connect to MQTT Broker with address " + BROKER);
			// e.printStackTrace();
		};
    }

    private static class OnMessageCallback implements IMqttMessageListener {

        private final MqttClient client;
        private final HintLookupCallback hintLookupCallback;

        private OnMessageCallback(MqttClient c, HintLookupCallback hlc){
            this.client = c;
            this.hintLookupCallback = hlc;
        }


        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            String[] splittedTopic = topic.split("/");


            String clientId = splittedTopic[1];
            String huntId = splittedTopic[0];
            String json = null;
            try {
                json = new String(message.getPayload());
                System.out.println("Message arrived on topic: '" + topic + "' with payload: '" + new String(message.getPayload()) + "'");
                //lookup the Hint
                String hint = hintLookupCallback.lookupHint(json);
                //send out the hint
                String responseTopic = huntId + "/" + clientId;

                System.out.println("Sending response on topic '" + responseTopic + "'" + " with payload '" + hint + "'.");
                client.publish(responseTopic, new MqttMessage(hint.getBytes()));
            } catch (MqttException e) {
                e.printStackTrace();
            }
            //TODO extract from topic

        }

    }

    /*private MqttCallback OnMessageCallback = new MqttCallback() {

        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            System.out.println(new String(message.getPayload()));
            // LookupNewHint(new String(message.getPayload()), topic);

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };*/

    private void LookupNewHint(String payload, String topic){
        JSONObject obj = new JSONObject(payload);

        String splittedTopic[] = topic.split("/");

        String huntid= splittedTopic[0];
        String clientid = splittedTopic[1];

        String pubTopic = huntid + "/" + clientid + "/down";
        String returnMessage = "ihr neuer hinweis ist: bla bla bla";

        // TODO: 10.01.2020 split json object
        // TODO: 10.01.2020 lookup new hint

        System.out.println(obj.getString("advertisment"));

        try {
            new MqttClient(BROKER, "PubClient").publish(pubTopic, new MqttMessage(returnMessage.getBytes()));
        } catch (MqttException e) {
            System.out.println("Error: Publish message failed");
            e.printStackTrace();
        }
    }

    public interface HintLookupCallback {

        String lookupHint(String identifier);

    }

    public static class StaticHintLookupCallback implements HintLookupCallback {

        @Override
        public String lookupHint(String json) {


            String hintId = extractHuntIDFromJson(json);
            switch (hintId) {
                case "1":
                    return "Insert ur first hint here";
                case "2":
                    return "Another hint";
                // TODO Insert more hints here...
                default:
                    return "Hint was not found on backend.";
            }
        }

        private String extractHuntIDFromJson(String json) {

            return "1"; // TODO extract from json


        }


    }

    public static void main(String[] args) {
        Backend backend = new Backend(new StaticHintLookupCallback());
        backend.start();
    }
}
