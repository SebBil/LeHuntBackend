package de.lehunt;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import org.json.JSONObject;

import java.util.function.Consumer;

class OnMessageCallback implements Consumer<Mqtt3Publish> {

    private final Mqtt3AsyncClient client;
    private final HintLookupCallback hintLookupCallback;

    public OnMessageCallback(Mqtt3AsyncClient c, HintLookupCallback hlc) {
        this.client = c;
        this.hintLookupCallback = hlc;
    }

    @Override
    public void accept(Mqtt3Publish mqtt3Publish) {
        String topic = mqtt3Publish.getTopic().toString();

        System.out.println("Message arrived on topic: '" + topic + "' with payload: '" + new String(mqtt3Publish.getPayloadAsBytes()) + "'");

        // create response Topic
        String huntId = topic.split("/")[0];
        String clientId = topic.split("/")[1];
        String responseTopic = huntId + "/" + clientId + "/down";

        // lookup new Hint
        JSONObject response = hintLookupCallback.lookupHint(huntId, new String(mqtt3Publish.getPayloadAsBytes()));


        // send out new Hint
        System.out.println("Sending response on topic '" + responseTopic + "'" + " with payload '" + response + "'.");
        client.publishWith()
                .topic(responseTopic)
                .payload(response.toString().getBytes())
                .send();

    }
}


