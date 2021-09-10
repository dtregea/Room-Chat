/**
 * ClientHandler.java
 * @author Daniel Tregea
 * ClientHandler objects connect to the client's socket via their communicator class
 * and handles/listens message requests and operations.
 */
package roomChat.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private String userName;
    private final Socket socket;
    private Room room;
    private boolean connected = false;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public ClientHandler(Socket socket, String room) throws IOException {
        this.socket = socket;
        this.room = Server.getRoom(room);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());

    }

    @Override
    public void run() {
        Message message;
        // Log in sequence
        while (!connected){
            try {
                message = (Message) receive();
                String[] credentials = message.toString().split(" ");

                if (message.getType() == Message.TYPE.LOGIN)
                    message = Server.logInUser(this, credentials[0], credentials[1]);
                 else if(message.getType() == Message.TYPE.REGISTER)
                    message = Server.registerUser(this, credentials[0], credentials[1]);

                if (message.getType() == Message.TYPE.LOGIN_SUCCESS)
                    room.addClient(this);

                send(message);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("ClientHandler disconnect during log in");
                break;
            }
        }
        // Chatting sequence
        while(connected){
            try{
                message = (Message) receive(); // receive from UI
                //TODO message filtering, ban inappropriate language
                System.out.println("RECEIVED MESSAGE IN ROOM " + room.getRoomName() + ": " + message.toString() + " - TYPE: "+ message.getType());
                if (message.getType() == Message.TYPE.MESSAGE){
                    room.broadcast(this, message); // broadcast to rest in room
                } else if (message.getType() == Message.TYPE.CHANGE_ROOM){
                    Room newRoom = Server.getRoom(message.toString());
                    if(newRoom.equals(room)){
                        send(new Message("You are already in " + room.getRoomName()));
                    } else{
                        send(new Message("Going to room: " + newRoom.getRoomName()));
                        room.removeClient(this, message);
                        newRoom.addClient(this);
                    }
                }else if (message.getType() == Message.TYPE.ROOM_STATUS){
                    send(new Message(Server.getRoomOccupancy()));
                }
            } catch(IOException | ClassNotFoundException e){
                room.removeClient(this, new Message("has disconnected"));
                Server.setClientOffline(this); // set offline
                connected = false;
            }
        }
    }

    /**
     * Get a clients username
     * This will be the same as the clients username in RoomChatDatabase
     * @return clients username
     */
    public String getUserName() {
        return userName;
    }

    /**
     * set a clients username
     * @param userName the name to be set to
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Send a message to the client
     * @param message message object to be sent
     * @throws IOException indicates connection error to clients socket
     */
    public void send(Message message) throws IOException {
        out.writeObject(new Message(message + "\n", message.getType()));
        out.flush();
    }

    /**
     * Listen for Message objects from the client
     * @return The object received over the stream
     * @throws IOException indicates connection error to clients socket
     */
    public Object receive() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    /**
     * Set a clients room
     * @param room the room to set the client to
     */
    public void setRoom(Room room) {
        this.room = room;
    }


    /**
     * Set a client as connected to the chat
     * @param connected status of whether client is connected to chat
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Kick a user from the server
     * @param reason Reason why the client was kicked
     */
    public void kick(String reason){
        try {
            send(new Message(reason));
            socket.close();
        } catch(IOException e){
            System.out.println("Error in kicking");
        }
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ClientHandler){
            ClientHandler temp = (ClientHandler) obj;
            return this.socket == temp.socket;
        } else {
            return false;
        }
    }
}
