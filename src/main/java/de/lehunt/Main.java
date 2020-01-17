package de.lehunt;

public class Main {

    public static void main(String[] args) {
        Backend backend = new Backend(new Backend.StaticHintLookup());
        backend.start();
    }
}
