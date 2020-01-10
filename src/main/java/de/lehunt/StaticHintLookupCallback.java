package de.lehunt;

public class StaticHintLookupCallback implements HintLookupCallback {


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
