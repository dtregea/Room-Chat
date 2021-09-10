/**
 * Room.java
 * @author Daniel Tregea
 * Used by Server.java and ClientHandler.java to create chat-able room instances
 * Rooms and a list of users which when called upon can broadcast a message
 * from one user to all in the list
 */
package roomChat.server;

import java.io.IOException;
import java.util.ArrayList;

public class Room implements Comparable<Room>{
    private final String roomName;
    private final ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); // List of clients

    public Room(String roomName) {
        this.roomName = roomName;
    }

    /**
     * Get the name of a room
     * @return room name
     */
    public String getRoomName(){
        return roomName;
    }

    /**
     * Add a client to the list of clients
     * @param clientHandler The client's client handler
     */
    public void addClient(ClientHandler clientHandler){
        clientHandlers.add(clientHandler);
        clientHandler.setRoom(this);
        broadcast(clientHandler, new Message("has joined the chat!"));
    }

    /**
     * Remove a client from the list of clients
     * @param clientHandler The client's clienthandler
     * @param message A message to broadcast to chat upon a user leaving
     */
    public void removeClient(ClientHandler clientHandler, Message message){
        clientHandlers.remove(clientHandler);
        clientHandler.setRoom(null);
        if(getRoomSize() > 0)
            broadcast(clientHandler, message);
        else
            if (!roomName.equalsIgnoreCase("Main")){
                System.out.println(roomName + " has no clients, deleting.");
                Server.removeRoom(this);
            }
    }

    /**
     * Broadcast a message from a client to all clients in a room
     * @param clientHandler The ClientHandler of the client sending the message
     * @param message The message to be sent
     */
    public void broadcast(ClientHandler clientHandler, Message message){ // better as clientHandler method?
        Message messageToSend = new Message("");
        // Determine message type
        if(message.getType() == Message.TYPE.MESSAGE){
            messageToSend.setMessage(roomName + " - "  + clientHandler.getUserName() + ": " + message);
        } else if (message.getType() == Message.TYPE.CHANGE_ROOM) {
            messageToSend.setMessage(roomName + " - " + clientHandler.getUserName() + " moved to room \"" + message.getMessage() + "\"");
        } else if (message.getType() == Message.TYPE.SERVER_BROADCAST){
            messageToSend.setMessage("Server announcement: " + message.getMessage());
        }
        // Broadcast that message
        for(ClientHandler eachClientHandler : clientHandlers){
            try {
                eachClientHandler.send(messageToSend);
            } catch(IOException ignore){
                System.out.println("exception in broadcast");
            }
        }
    }

    /**
     * Get the amount of clients in a room
     * @return the amount of clients in a room
     */
    private int getRoomSize(){
        return clientHandlers.size();
    }

    /**
     * Determine equality of rooms
     * @param obj the object to be compared to
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Room){
            Room temp = (Room) obj;
            return this.roomName.equalsIgnoreCase(temp.roomName);
        } else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return roomName.hashCode();
    }

    /**
     * Compare rooms based on room size
     * @param o The room to compare
     * @return Comparison (-1,0,1) on the size of the two rooms
     */
    @Override
    public int compareTo(Room o) {
        int result = this.getRoomSize() - o.getRoomSize();
        if(result == 0)
            result = this.roomName.compareTo(o.roomName);
        return result;
    }

    /**
     * Generate a string on the room and size
     * @return room and size
     */
    @Override
    public String toString() {
        return roomName + " - " + getRoomSize();
    }
}
