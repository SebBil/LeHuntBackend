package de.lehunt;


import org.json.JSONObject;

interface HintLookupCallback {

    JSONObject lookupHint(String huntId, String payload);

}
