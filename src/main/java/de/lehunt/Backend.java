package de.lehunt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class Backend {

    private static final String BROKER = "localhost";
    private Mqtt3AsyncClient mqttClient;
    private final HintLookupCallback hintLookupCallback;

    public Backend(HintLookupCallback hintLookupCallback) {
        this.hintLookupCallback = hintLookupCallback;
    }

    public void start() {

        String CLIENTID = "BackendClient";
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

        OnMessageCallback onMessageCallback = new OnMessageCallback(mqttClient, hintLookupCallback);

        String subscritionTopic = "+/+/up";
        mqttClient.subscribeWith()
                .topicFilter(subscritionTopic)
                .qos(MqttQos.EXACTLY_ONCE)
                .callback(onMessageCallback)
                .send();
        System.out.println("Backend subscribed successful topics on:  " + subscritionTopic + " for getting all messages of the mobile clients.");
    }

    public static class StaticHintLookup implements HintLookupCallback {

        @Override
        public JSONObject lookupHint(String huntId, String payload) {
            String ext = ".json";
            System.out.println(huntId);

            JSONObject json = new JSONObject(payload);
            //String type = json.getString("type");
            int advertisement = json.getInt("advertisement");
            int curStation = json.getInt("station");

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
                String json = new String(data, StandardCharsets.UTF_8);

                JSONObject obj = new JSONObject(json);
                JSONArray arr = obj.getJSONArray("advertisements");


                for(int i = 0; i < arr.length(); i++) {
                    JSONObject object = arr.getJSONObject(i);

                    // System.out.println(object);
                    for(String key:object.keySet()){
                        if (key.contentEquals(String.valueOf(beaconId))) {

                            return object.getJSONObject(key);
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
}
