package com.example.paaltjesvoetbal.networking;

/**
 * Protocol class with constants for creating protocol messages.
 */
public final class Protocol {
    // Client:
    public static final String HELLO = "HELLO";
    public static final String QUIT = "QUIT";

    // Server:
    public static final String WELCOME = "WELCOME";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String INVALIDUSERNAME = "INVALIDUSERNAME";
    public static final String SERVER = "SERVER";
    public static final String ERROR = "ERROR";

    // Both:
    public static final String UPDATE = "UPDATE";
    public static final String SEPARATOR = "~";

    /**
     * Constructor to prevent instantiation.
     */
    private Protocol() {}
}