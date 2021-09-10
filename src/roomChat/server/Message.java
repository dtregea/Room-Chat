/**
 * Message.java
 * @author Daniel Tregea
 * Message objects are used to communicate between the client and Server
 */
package roomChat.server;

import java.io.Serializable;

public class Message implements Serializable {
    private final TYPE type;
    private String message;

    public Message(String message){
        this.message = message;
        this.type = TYPE.MESSAGE;
    }
    public Message(TYPE type){
        message = null;
        this.type = type;
    }
    public Message(String message, TYPE type){
        this.message = message;
        this.type = type;
    }

    public enum TYPE{
        MESSAGE, CHANGE_ROOM, ROOM_STATUS, SERVER_BROADCAST, LOGIN_SUCCESS, LOGIN_DENIED, LOGIN, REGISTER
    }

    /**
     * Get the type of the message
     * @return Message enum type
     */
    public TYPE getType() {
        return type;
    }

    /**
     * Get the contents of the message
     * @return contents of the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the contents of the message
     * @param message String contents to set message to
     */
    public void setMessage(String message){
        this.message = message;
    }

    /**
     * Get the contents of the message
     * @return contents of the message
     */
    @Override
    public String toString() {
        return message;
    }
}
