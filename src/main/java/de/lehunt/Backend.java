package de.lehunt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Backend {

    private static final String TOPIC_FILTER = "test/topic";
    private static final  String HOST = "broker.hivemq.com";
    private final HintLookupCallback hintLookupCallback;

    Backend(HintLookupCallback hintLookupCallback) {
        this.hintLookupCallback = hintLookupCallback;
    }

    public static void main(String[] args) {
        Backend backend = new Backend(new StaticHintLookupCallback());
        backend.start();
    }

    public void start() {
        Mqtt3AsyncClient client = Mqtt3Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(HOST)
                .buildAsync();
        try {
            Mqtt3ConnAck mqtt3ConnAck = client.connect().get();
            if (mqtt3ConnAck.getReturnCode().isError()) {
                System.out.println("Backend could not connect to MQTT Broker with address " + HOST);
            } else {
                System.out.println("Backend conncted successfully to MQTT Broker with address " + HOST);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        OnMessageCallback onMessageCallback = new OnMessageCallback(client, hintLookupCallback);

        client.subscribeWith()
                .topicFilter(TOPIC_FILTER) //TODO adjust the topic filter
                .qos(MqttQos.EXACTLY_ONCE)
                .callback(onMessageCallback)
                .send();
    }

    private static class OnMessageCallback implements Consumer<Mqtt3Publish> {
        private final Mqtt3AsyncClient client;
        private final HintLookupCallback hintLookupCallback;

        private OnMessageCallback(Mqtt3AsyncClient client, HintLookupCallback hintLookupCallback) {
            this.client = client;
            this.hintLookupCallback = hintLookupCallback;
        }

        @Override
        public void accept(Mqtt3Publish mqtt3Publish) {
            String topic = mqtt3Publish.getTopic().toString();
            //extract the id of the Hint

            String[] splittedTopic = topic.split("/");


            String clientId = splittedTopic[1];
            String huntId = splittedTopic[0];
            String json = new String(mqtt3Publish.getPayloadAsBytes());
            //TODO extract from topic
            System.out.println("Message arrived on topic: '" + topic + "' with payload: '" + new String(mqtt3Publish.getPayloadAsBytes()) + "'");
            //lookup the Hint
            String hint = hintLookupCallback.lookupHint(json);
            //send out the hint
            String responseTopic = huntId + "/" + clientId;

            System.out.println("Sending response on topic '" + responseTopic + "'" + " with payload '" + hint + "'.");
            client.publishWith()
                    .topic(responseTopic)
                    .payload(hint.getBytes())
                    .send();
        }
    }
}
