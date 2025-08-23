package com.example.paaltjesvoetbal;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import com.example.paaltjesvoetbal.networking.SocketConnection;
import com.example.paaltjesvoetbal.networking.Protocol;

/**
 * Class representing a connection with the client extending the SocketConnection class.
 */
public class ClientConnection extends SocketConnection {
    private GameView gameClient;

    /**
     * Make a new TCP connection to the given host and port.
     * The receiving thread is not started yet. Call start on the returned SocketConnection to start
     * receiving messages.
     *
     * @param host the address of the server to connect to
     * @param port the port of the server to connect to
     * @throws IOException if the connection cannot be made or there was some other I/O problem
     */
    protected ClientConnection(InetAddress host, int port) throws IOException {
        super(host, port);
    }

    public boolean sendMessage(String message, String timestamp) {
        return super.sendMessage(message + Protocol.SEPARATOR + timestamp); //TODO:change
    }

    public boolean login(String username) {
        return sendUsername(username); //TODO:change
    }

    public boolean sendUsername(String username) {
        return super.sendMessage(Protocol.HELLO + Protocol.SEPARATOR + username);
    }

    /**
     * Gives a GameClient to the client connection.
     *
     * @param gameClient the game client to assign
     */
    protected void setChatClient(GameView gameClient) {
        this.gameClient = gameClient;
    }


    /**
     * Starts the connection.
     */
    @Override
    protected void start() {
        super.start();
    }

    /**
     * Handles a message received from the connection.
     * @param message the message received from the connection
     */
    @Override
    protected void handleMessage(String message) {
        Log.d("ClientConnection", "Received message: " + message);
        System.out.println(message);
        String[] splitMsg = message.split(Protocol.SEPARATOR);
        //        System.out.println(Arrays.toString(splitMsg));
        switch (splitMsg[0]) {
            case Protocol.INVALIDUSERNAME:
                // notify gameClient that the username is invalid
                break;
            case Protocol.WELCOME:
                // notify gameClient that the login was successful
                break;
            case Protocol.UPDATE:
                gameClient.receiveUpdate(splitMsg[1], splitMsg[2], splitMsg[3]);
                break;
            case Protocol.SERVER:
                System.out.println("Message from server: " + splitMsg[1]);
                break;
            case Protocol.PING:
                Log.d("Ping", "Ping received from server.");
                gameClient.onPingResponse();
                break;
        }
    }

    /**
     * Handles a disconnect from the connection, i.e., when the connection is closed.
     */
    @Override
    public void handleDisconnect() {
        gameClient.handleDisconnect();
    }

    /**
     * Handles the closing of the connection.
     */
    @Override
    public void close() {
        super.close();
    }
}