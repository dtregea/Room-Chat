/**
 * Communicator.java
 * @author Daniel Tregea
 * A class which serves as the message handler for the clients GUI application (ClientChat.java)
 */
package roomChat.user;

import javafx.application.Platform;
import roomChat.server.Message;
import java.io.*;
import java.net.Socket;

public class Communicator {
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public Communicator(String address, int port) throws IOException {
        Socket socket = new Socket(address, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        ClientChat.connected = true;
    }

    /**
     * Listen for Message objects from the server
     */
    public void listen(){
        Message message;
        while(true){
            try{
                message = (Message) receive();
                System.out.println("message received: " + message.getType());
                if(message.getType() == Message.TYPE.LOGIN_SUCCESS) {
                    Platform.runLater(ClientChat::changeToChatScene);
                } else if (message.getType() == Message.TYPE.LOGIN_DENIED){
                    ClientChat.updateLogInGUI(message);
                    //TODO Handle kick messages uniquely
                } else{
                    ClientChat.updateGUI(message);
                }
            } catch(IOException e){
                System.out.println("Disconnected from server");
                ClientChat.updateGUI(new Message("Connection to server has been severed\n")); // TODO do to log in gui too
                ClientChat.updateLogInGUI(new Message("Connection to server has been severed"));
                break;
            } catch(ClassNotFoundException f){
                ClientChat.updateGUI(new Message("Object not found exception\n"));
                break;
            }

        }
    }

    /**
     * Send a message to the server
     * @param message message to be sent
     */
    public void send(Message message){
        try{
            out.writeObject(message);
            out.flush();
        } catch(IOException e){
            ClientChat.updateGUI(new Message("Connection to server has been severed\n"));

        }

    }

    /**
     * Receive messages from the server
     * @return Message object received from server
     * @throws IOException Indicates connection error to server
     */
    private Object receive() throws IOException, ClassNotFoundException {
        return in.readObject();
    }
}
