package de.lehunt;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

public class Backend {

    private static final  String BROKER = "tcp://bilda.ddnss.de:1883";
    private static final int QOS_EXACTLY_ONCE = 1;
    private final String  clientId = "BackendClient";
    private final String SubscritionTopic = "+/+/up";
    private MqttClient mqttClient;
    private final HintLookupCallback hintLookupCallback;

    public Backend(HintLookupCallback hintLookupCallback){
        this.hintLookupCallback = hintLookupCallback;
    }

    public void start() {
		try {
            mqttClient = new MqttClient(BROKER, clientId);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+ BROKER);
            mqttClient.connect();
            System.out.println("Connected");

            OnMessageCallback callback = new OnMessageCallback(mqttClient, hintLookupCallback);

		    mqttClient.subscribe(SubscritionTopic, QOS_EXACTLY_ONCE, callback);
            System.out.println("Subscribed to Topics: " + SubscritionTopic);

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
            JSONObject json = null;
            try {
                System.out.println("Message arrived on topic: '" + topic + "' with payload: '" + new String(message.getPayload()) + "'");

                // lookup new Hint
                String hint = hintLookupCallback.lookupHint(new String(message.getPayload()));

                // create response Topic
                String responseTopic = huntId + "/" + clientId + "/down";

                System.out.println("Sending response on topic '" + responseTopic + "'" + " with payload '" + hint + "'.");

                // send out new Hint
                client.publish(responseTopic, new MqttMessage(hint.getBytes()));
            } catch (MqttException e) {
                e.printStackTrace();
            }

        }

    }

    public interface HintLookupCallback {

        String lookupHint(String payload);

    }

    public static class StaticHintLookupCallback implements HintLookupCallback {

        @Override
        public String lookupHint(String payload) {
            JSONObject json = new JSONObject(payload);

            String hintId = json.getString("adverstisment");
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


    }

    public static void main(String[] args) {
        Backend backend = new Backend(new StaticHintLookupCallback());
        backend.start();
    }
}
