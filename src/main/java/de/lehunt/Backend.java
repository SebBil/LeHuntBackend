package de.lehunt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
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
        System.out.println("Backend subscribed successful topics on:  " + SubscritionTopic + " for getting all messages of the mobile clients.");
    }

    // TODO: 12.01.2020 Setup retained message to all hunt topics with
    private void SetupFirstHints() {
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

    public interface HintLookupCallback {

        JSONObject lookupHint(String huntId, String payload);

    }

    public static class StaticHintLookupCallback implements HintLookupCallback {

        @Override
        public JSONObject lookupHint(String huntId, String payload) {
            String ext = ".json";
            System.out.println(huntId);

            JSONObject json = new JSONObject(payload);
            String type = json.getString("type");
            int advertisement = json.getInt("advertisement");
            int curStation = json.getInt("station");

            // TODO: 13.01.2020 Check if the next station is the correct one
            if(advertisement != curStation){
                return new JSONObject("{type:Info,message:You reached the false station. Keep searching on}");
            }

            return FindReadHunt(new String(new StringBuilder(huntId).append(ext)), advertisement);

        }


        public JSONObject FindReadHunt(String fileName, int beaconId){

            File file = getFileFromResources(fileName);
            if(file == null){
                return new JSONObject("{type:Error, message:Hunt doesn't exist}");
            }

            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String json = new String(data, "UTF-8");

                JSONObject obj = new JSONObject(json);
                JSONArray arr = obj.getJSONArray("advertisements");


                for(int i = 0; i < arr.length(); i++) {
                    JSONObject object = arr.getJSONObject(i);

                    // System.out.println(object);
                    for(String key:object.keySet()){
                        if (key.contentEquals(String.valueOf(beaconId))) {

                            JSONObject tmp = object.getJSONObject(key);
                            return tmp;
                        }
                    }
                }
                return new JSONObject("{type:Error,message:Hint is not in the database}");

            } catch (FileNotFoundException e) {
                // e.printStackTrace();
                return new JSONObject("{type:Error,message:The Hunt was not found}");
            } catch (IOException e) {
                // e.printStackTrace();
                return new JSONObject("{type:Error,message:Input or Output failure}");
            }
        }


        // get file from classpath, resources folder
        private File getFileFromResources(String fileName) {

            ClassLoader classLoader = getClass().getClassLoader();

            URL resource = classLoader.getResource(fileName);
            if (resource == null) {
                return null;
                // throw new IllegalArgumentException("file is not found!");
            } else {
                return new File(resource.getFile());
            }

        }
    }


    public static void main(String[] args) {
        Backend backend = new Backend(new StaticHintLookupCallback());
        backend.start();
    }
}
