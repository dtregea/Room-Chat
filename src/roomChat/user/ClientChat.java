/**
 * ClientChat.java
 * @author Daniel Tregea
 * Users can interact with a GUI to send messages to rooms.
 * ClientChat uses a Communicator object to listen for and send message
 */
package roomChat.user;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import roomChat.server.Message;
import java.io.IOException;
import java.util.HashMap;

public class ClientChat extends Application {
    private final static String SERVER_IP = "127.0.0.1";
    private final static int PORT = 30000;
    private Communicator communicator;
    private static final TextFlow chat = new TextFlow();
    private static final TextArea inputArea = new TextArea();
    private static final HashMap<String, String[]> colorSchemes = new HashMap<>();
    private String roomNameChange;
    private boolean changeRoomConfirming = false;
    private static Button send;
    private static Button changeRoom;
    private static Button[] controlButtons;
    private static ScrollPane chatPane;
    private static GridPane root;
    private static Label logInLabel = new Label("Room Chat");
    private static Stage stage;
    private static Scene chatScene;
    public static boolean connected = false;

    public ClientChat(){
        try {
            communicator = new Communicator(SERVER_IP, PORT);
            new Thread(() -> communicator.listen()).start();
        } catch(IOException e){
            updateLogInGUI(new Message("Could not connect to server"));
        }
    }

    @Override
    public void init(){
        // String contents in order:
        // Chat background, Text chat input background, control buttons, send button,send button color, button text color
        colorSchemes.put("BEE", new String[]{"FFCC47", "E6BE8A", "996515", "5E321F", "WHITE", "BLACK"});
        colorSchemes.put("DESSERT", new String[]{"FFCB8E", "FAF0BE", "FC5A8D", "CD3B6A", "BLACK", "BLACK"});
        colorSchemes.put("PURPLE", new String[]{"957DAD", "E0BBE4", "FEC8D8", "D291BC", "BLACK", "BLACK"});
        colorSchemes.put("BEACH", new String[]{"F1E0B0", "E7CFC8", "97F2F3", "89AEB2", "WHITE", "BLACK"});
    }

    @Override
    public void start(Stage stage){
        // Scene 1 login
        ClientChat.stage = stage;
        //logInLabel = new Label("Room Chat");
        logInLabel.setAlignment(Pos.CENTER);
        logInLabel.setFont(Font.font(20));
        TextArea usernameArea = new TextArea();
        usernameArea.setPromptText("Username");
        PasswordField passwordArea = new PasswordField();
        passwordArea.setPromptText("Password");
        usernameArea.setMaxHeight(passwordArea.getHeight());
        VBox infoFields = new VBox(usernameArea, passwordArea);
        infoFields.setSpacing(5);
        Button login = new Button("Log-in");
        Button register = new Button("Create Account");
        VBox logButtons = new VBox(login, register);

        logButtons.setSpacing(10);
        logButtons.setAlignment(Pos.BOTTOM_LEFT);
        VBox logRoot = new VBox(logInLabel, infoFields, logButtons);
        logRoot.setSpacing(16);
        logRoot.setPadding(new Insets(20));
        Scene logInScene = new Scene(logRoot);
        stage.setScene(logInScene);

        // Scene 2 chat
        stage.setTitle("Multi chat");
        send = new Button("send");
        changeRoom = new Button("Change Room");
        changeRoom.setPadding(new Insets(10));
        Button roomStatus = new Button("Room Status");
        roomStatus.setPadding(new Insets(10));
        Button changeScheme = new Button("Change Scheme");
        changeScheme.setPadding(new Insets(10));
        controlButtons = new Button[]{changeScheme, changeRoom, roomStatus};
        VBox controls = new VBox(changeRoom, roomStatus, changeScheme);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(20);

        chatPane = new ScrollPane(chat);
        chatPane.setFitToWidth(true);
        chatPane.setMinSize(500, 400);
        inputArea.setMaxHeight(40);
        inputArea.setWrapText(true);
        inputArea.setPromptText("Enter Message");
        root = new GridPane();
        root.setPadding(new Insets(15));
        root.add(chatPane, 0, 0);
        root.add(inputArea, 0, 1);
        root.add(send, 0 , 2);
        root.add(controls, 1, 0);
        chatScene = new Scene(root);
        stage.setResizable(false);
        stage.show();

        // When user presses log in
        login.setOnMouseClicked(e->{
            if(connected) {
                String userName = usernameArea.getText().strip(), password = passwordArea.getText();
                if (!userName.equalsIgnoreCase("") && !password.equals(""))
                    communicator.send(new Message(userName + " " + password, Message.TYPE.LOGIN));
                else
                    updateLogInGUI(new Message("Fields may not be empty"));
            } else{
                updateLogInGUI(new Message("Server is not online. Please Restart"));
            }
        });

        // Send register request
        register.setOnMouseClicked(e->{
            if(connected) {
                String userName = usernameArea.getText().strip(), password = passwordArea.getText();
                if (!userName.equalsIgnoreCase("") && !password.equals(""))
                    communicator.send(new Message(userName + " " + password, Message.TYPE.REGISTER));
                else
                    updateLogInGUI(new Message("Fields may not be empty"));
            } else{
                updateLogInGUI(new Message("Server is not online. Please Restart"));
            }
        });

        // Send a message. Also serves as the cancel button when changing rooms
        send.setOnAction(e->{
            if(!changeRoomConfirming){
                if(!inputArea.getText().equals("") && !inputArea.getText().equals("\n")) {
                    Message message = new Message(inputArea.getText().strip());
                    communicator.send(message);
                }
            } else{ // "Runs when user clicks "cancel" when changing rooms"
                changeRoomConfirming = false;
                changeRoom.setText("change room");
                send.setText("send");
                updateGUI(new Message("Room change cancelled\n"));
            }

            clearText(inputArea);
        });

        // Press send when user presses enter on keyboard
        inputArea.setOnKeyPressed(e->{
            if(e.getCode() == KeyCode.ENTER){
                if(!changeRoomConfirming)
                    send.fire();
            }
        });

        // Change room. Serves as the confirm button when changing rooms
        changeRoom.setOnMouseClicked(e->{
            if(!changeRoomConfirming){
                if(!inputArea.getText().equals("")){
                    roomNameChange = inputArea.getText().strip();
                    String[] roomNameChangeLine = roomNameChange.split(" ");
                    if(roomNameChangeLine.length > 1)
                        updateGUI(new Message("Room name must contain no spaces\n"));
                    else{
                        updateGUI(new Message("Go to room \"" + roomNameChange + "\", confirm or cancel?\n"));
                        changeRoomConfirming = true;
                        changeRoom.setText("confirm");
                        send.setText("cancel");
                    }
                } else {
                    updateGUI(new Message("Usage: Type in room to enter, then press confirm\n"));
                }
            } else{ // When changing rooms, confirm
                changeRoomConfirming = false;
                changeRoom.setText("change room");
                send.setText("send");
                communicator.send(new Message(roomNameChange, Message.TYPE.CHANGE_ROOM));
            }
            clearText(inputArea);
        });

        // Sends a room status request
        roomStatus.setOnMouseClicked(e-> communicator.send(new Message(Message.TYPE.ROOM_STATUS)));

        // Change color schemes
        changeScheme.setOnMouseClicked(e->{
            String newScheme = inputArea.getText().strip().toUpperCase();
            clearText(inputArea);
            if(newScheme.equals("")){
                StringBuilder stringBuilder = new StringBuilder("--------\n");
                stringBuilder.append("Please type in which color scheme to change to:\n");
                for (String scheme: colorSchemes.keySet()){
                    stringBuilder.append(scheme).append(" | ");
                }
                updateGUI(new Message(stringBuilder.append("\n-------\n").toString()));
            } else {
                if (colorSchemes.get(newScheme) == null)
                    updateGUI(new Message("\"" + newScheme + "\" is not an available color scheme.\nPress change scheme to see options.\n"));
                setColorScheme(newScheme);
            }
        });

    }

    /**
     * Add a new chat to the room chat
     * @param newText the message to add to chat
     */
    public static void updateGUI(Message newText){
        Platform.runLater(()->chat.getChildren().add(new Text(newText.toString())));
    }

    /**
     * Update text in the LogInGUI
     * @param newText the message to set the text in the LogInGUI
     */
    public static void updateLogInGUI(Message newText){
        Platform.runLater(()->logInLabel.setText(newText.getMessage()));
    }

    /**
     * Clear a TextAreas text
     * @param text The TextArea to clear
     */
    public static void clearText(TextArea text){
        text.setText("");
    }

    /**
     * Set the color scheme of the GUI from the choices given
     * Available color schemes: Bee, beach, purple, dessert
     * @param newScheme the name of the new scheme to switch to
     */
    public static void setColorScheme(String newScheme){
        String[] scheme = colorSchemes.get(newScheme);
        int controlButtonTextColor;
        if(scheme[5].equals("WHITE"))
            controlButtonTextColor = 255;
        else
            controlButtonTextColor = 0;
        for (Button controlButton : controlButtons) {
            controlButton.setStyle("-fx-background-color: #" + scheme[2]);
            controlButton.setTextFill(Color.rgb(controlButtonTextColor, controlButtonTextColor, controlButtonTextColor));
        }
        root.setStyle("-fx-background-color: #" + scheme[0]);
        chatPane.setStyle("-fx-background: #" + scheme[1]);
        inputArea.setStyle("-fx-control-inner-background:#" + scheme[1]);
        send.setStyle("-fx-background-color: #" + scheme[3]);
        if (scheme[4].equals("WHITE"))
            send.setTextFill(Color.rgb(255, 255, 255));
        else
            send.setTextFill(Color.rgb(0, 0, 0));
    }

    /**
     * Switch the scene to the chat room scene
     */
    public static void changeToChatScene(){
        stage.close();
        stage.setScene(chatScene);
        stage.show();
    }
}