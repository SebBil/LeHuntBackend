package de.lehunt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Backend {

    private static final String BROKER = "localhost";
    private final String CLIENTID = "BackendClient";
    private final String SubscritionTopic = "+/+/up";
    private Mqtt3AsyncClient mqttClient;
    private final HintLookupCallback hintLookupCallback;

    public Backend(HintLookupCallback hintLookupCallback) {
        this.hintLookupCallback = hintLookupCallback;
    }

    public void start() {

        mqttClient = Mqtt3Client.builder()
                .identifier(CLIENTID)
                .serverHost(BROKER)
                .buildAsync();
        try {
            Mqtt3ConnAck mqtt3ConnAck = mqttClient.connect().get();
            if (mqtt3ConnAck.getReturnCode().isError()) {
                System.out.println("Backend could not connect to MQTT Broker with address " + BROKER);
            } else {
                System.out.println("Backend conncted successfully to MQTT Broker with address " + BROKER);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // SetupFirstHints();

        OnMessageCallback onMessageCallback = new OnMessageCallback(mqttClient, hintLookupCallback);

        mqttClient.subscribeWith()
                    .topicFilter(SubscritionTopic) //TODO adjust the topic filter
                    .qos(MqttQos.EXACTLY_ONCE)
                    .callback(onMessageCallback)
                    .send();
        System.out.println("Client subscribed successful to topic " + SubscritionTopic);
    }

    // TODO: 12.01.2020 Setup retained message to all hunt topics with
    private void SetupFirstHints(){
        /* First hint for hunt1 */
        mqttClient.publishWith()
                .topic("hunt1/+/up")
                .retain(true)
                .qos(MqttQos.EXACTLY_ONCE)
                .payload("First hint in the hunt1. Begin your digital hunt".getBytes())
                .send();
        System.out.println("First Hint is set through retain message on hunt1/+/up");

        /* First hint for hunt2 */
        mqttClient.publishWith()
                .topic("hunt2/+/up")
                .retain(true)
                .qos(MqttQos.EXACTLY_ONCE)
                .payload("First hint in the hunt2. Begin your digital hunt".getBytes())
                .send();


    }


    private static class OnMessageCallback implements Consumer<Mqtt3Publish> {

        private final Mqtt3AsyncClient client;
        private final HintLookupCallback hintLookupCallback;

        private OnMessageCallback(Mqtt3AsyncClient c, HintLookupCallback hlc) {
            this.client = c;
            this.hintLookupCallback = hlc;
        }

        @Override
        public void accept(Mqtt3Publish mqtt3Publish) {

            String topic = mqtt3Publish.getTopic().toString();
            String splittedTopic[] = topic.split("/");

            String clientId = splittedTopic[1];
            String huntId = splittedTopic[0];
            JSONObject json = null;
            System.out.println("Message arrived on topic: '" + topic + "' with payload: '" + mqtt3Publish.getPayloadAsBytes() + "'");

            // lookup new Hint
            String hint = hintLookupCallback.lookupHint(huntId, new String(mqtt3Publish.getPayloadAsBytes()));

            // create response Topic
            String responseTopic = huntId + "/" + clientId + "/down";

            // send out new Hint
            System.out.println("Sending response on topic '" + responseTopic + "'" + " with payload '" + hint + "'.");
            client.publishWith()
                    .topic(responseTopic)
                    .payload(hint.getBytes())
                    .send();

        }
    }

    public interface HintLookupCallback {

        String lookupHint(String huntid, String payload);

    }

    public static class StaticHintLookupCallback implements HintLookupCallback {

        @Override
        public String lookupHint(String huntid, String payload) {
            JSONObject json = new JSONObject(payload);
            String HuntAndBeaconID = json.getString("advertisment");

            String tmp[] = HuntAndBeaconID.split("-");
            String HuntID = tmp[0];
            String BeaconID = tmp[1];

            if (HuntID.equalsIgnoreCase("hunt1")) {
                switch (BeaconID) {
                    case "1":
                        return "{\"message\":\"This is your second Hint, so you can find your first station." +
                                "jfiöoreajfioewaönfewajfiewaoöfewajfiewa" +
                                "fjeöajfioewajifvreujgiföoreaijfoewjaifojrewaf" +
                                "fieauireajfioewöajfioewaöjivfera waef" +
                                "fioöewa fjfiöoeajfoiewa jfifaj afjioewa jfewa" +
                                " feijoaöjfi owjf waifjiewoaf iw fwea jfwejaireh" +
                                " fj oiewaöjfiwaf jwoafj ioajfi oewaöjfo2jfoiewajf ier" +
                                " fjieowajfiwajfiwojf oigiqjg ioewaf4i3g 80 fj fwifjoiew\"";
                    case "2":
                        return "This is your third Hint, so you can find the next station." +
                                "fijeowa fjw ieaofjiwoaf wf wifj ewiaojfoiewa  fjwaf wea" +
                                "fje iwaojf iwafjoa jfiwa jfiewjaf io ewajfioew afi ajf" +
                                "f jewoiaöjfi jier gfoiewaj fiewaj fiewjifo jewaio jfoiew" +
                                "j ifewoajf oiwajfi ajfiwoajfiowahgie jgioajfwiajfiewa jfoiew" +
                                "fj iwafwjfioewjafiowa jfiewoa jfiewoaöjfiewajf oiewhvidja" +
                                " fjioewajfaöjioewj fw wi afwiajf iowa jiwaf jiewfoiewa jfoi" +
                                "fjei wafjew oiafj iwaofjoiewjf oiewajf owajf ewoi jfoiewa";
                    default:
                        return "Hint was not found on backend.";
                }
            } else if (HuntID.equalsIgnoreCase("hunt2")) {
                switch (BeaconID) {
                    case "1":
                        return "Insert ur first hint here";
                    case "2":
                        return "Another hint";
                    // TODO Insert more hints here...
                    default:
                        return "Hint was not found on backend.";
                }
            } else {
                return "Error: Hunt does not exist";
            }
        }

    }

    public static void main(String[] args) {
        Backend backend = new Backend(new StaticHintLookupCallback());
        backend.start();
    }
}
