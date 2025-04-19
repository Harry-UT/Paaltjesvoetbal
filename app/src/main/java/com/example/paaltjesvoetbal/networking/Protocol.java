package com.example.paaltjesvoetbal.networking;

/**
 * Protocol class with constants for creating protocol messages.
 */
public final class Protocol {
    public static final String HELLO = "HELLO";
    public static final String LOGIN = "LOGIN";
    public static final String INVALIDUSERNAME = "INVALIDUSERNAME";
    public static final String MESSAGE = "MESSAGE";
    public static final String LOGOUT = "LOGOUT";
    public static final String SERVER = "SERVER";
    public static final String SEPARATOR = "@@@";
    public static final String DISCONNECT = "DISCONNECT";

    /**
     * Constructor to prevent instantiation.
     */
    private Protocol() {
    }
}