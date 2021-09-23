/**
 * Server.java
 * @author Daniel Tregea
 * Allows ClientChat.java users to connect and handled with ClientHandler.java
 * Run commands on stdin
 * "User" and "Client" used interchangeably
 */
package roomChat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.security.MessageDigest;

public class Server {

    private static final int SERVER_PORT = 30000;
    private static ServerSocket serverSocket;
    private static final HashSet<Room> rooms = new HashSet<>();
    private static final HashMap<String, ClientHandler> clients = new HashMap<>(); // List of active clients with handlers
    static Connection connect = null; // Connection to database

    public static void main(String[] args) throws IOException{

        connectToDatabase();
        // Set everyone to offline upon startup to ensure log in functionality after reboot
        markAllUsersOffline();
        Scanner scanner = new Scanner(System.in);
        serverSocket = new ServerSocket(SERVER_PORT);

        // Listen for clients
        new Thread(() -> {
            try {
                listen(serverSocket);
            } catch (IOException e) {
                System.out.println("Error in listening. Server offline"); //happens when connect from browser, modify listen method to continue
                System.out.println(e.getMessage());
                disconnectFromDatabase();
                System.exit(-1);
            }
        }).start();

        // Read in commands
        System.out.println("Startup successful. Listening for commands on stdin");
        while(true){
            readCommands(scanner.nextLine());
        }
    }

    /** Listen for new clients and send them to room "Main"
     * @param serverSocket This servers socket
     * @throws IOException Indicates an unknown request has been made (GET/POST etc.)
     */
    private static void listen(ServerSocket serverSocket) throws IOException {
        System.out.println("Server is listening");
        while(true){
            Socket socket = serverSocket.accept();
            System.out.println("client connected");
            new Thread(new ClientHandler(socket, "Main")).start();
        }
    }

    /** Read commands in the console
     * @param command command to be executed
     */
    private static void readCommands(String command){
        String[] commandLine = command.strip().split(" ");

        if(commandLine[0].equalsIgnoreCase("/END")){
            System.out.println("Shutting down");
            serverBroadcast(new Message("Server is being shut down", Message.TYPE.SERVER_BROADCAST));
            markAllUsersOffline();
            disconnectFromDatabase();
            System.exit(-1);
        }else if(commandLine[0].equalsIgnoreCase("/ANNOUNCE")) {
            serverBroadcast(new Message(command.substring(3), Message.TYPE.SERVER_BROADCAST));
        }else if(commandLine[0].equalsIgnoreCase("/KICK")){
            try {
                clients.get(commandLine[1]).kick("You have been kicked from the server");

            } catch(NullPointerException n){
                System.out.println("User not found");
            }
        } else{
            System.out.println("Command not recognized");
        }
    }

    /** Remove a room from the list of rooms.
     * @param room The room to be removed
     */
    public static void removeRoom(Room room){
        rooms.remove(room);
    }

    /** Get a room instance
     *  Creates room if not found
     * @param name the name of the room to get
     * @return room instance of the name entered
     */
    public static Room getRoom(String name){
        for(Room room: rooms){
            if(room.getRoomName().equalsIgnoreCase(name))
                return room;
        }
        return createRoom(name);
    }

    /** Create a room instance
     * @param name the name of the room to create
     * @return the newly created room instance
     */
    private static Room createRoom(String name){
        Room newRoom = new Room(name);
        rooms.add(newRoom);
        return newRoom;
    }

    /** Determine whether a name is taken by a user in RoomChatDatabase
     * @param name the name of the user
     * @return True - Name is taken. False - Name is not taken
     */
    public static boolean isNameTaken(String name){
        boolean result = false;
        try {
            PreparedStatement preparedStatement = connect.prepareStatement("SELECT username FROM user_info WHERE username=?");
            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();
            if(rs.next()) {
                String query = rs.getString("username");
                if (query.equals(name))
                    result = true;
            }
        } catch(SQLException e){
            System.out.println("error in name taken");
            System.out.println(e.getMessage());
        }
        return result;
    }

    /** Mark a user as online in RoomChatDatabase
     * @param clientHandler the clientHandler of the client to set online
     */
    private static void setClientOnline(ClientHandler clientHandler){
        try {
            PreparedStatement preparedStatement = connect.prepareStatement("UPDATE user_info SET connected=true WHERE username=?");
            preparedStatement.setString(1, clientHandler.getUserName());
            preparedStatement.executeUpdate();
            System.out.println(clientHandler.getUserName()  + " set online");
            clients.put(clientHandler.getUserName(), clientHandler);
            clientHandler.setConnected(true);
        } catch (SQLException e){
            System.out.println("Error in setting a client online");
        }

    }

    /** Mark a user as offline in RoomChatDatabase
     * @param clientHandler the clientHandler of the client to set offline
     */
    public static void setClientOffline(ClientHandler clientHandler){
        try {
            PreparedStatement preparedStatement = connect.prepareStatement("UPDATE user_info SET connected=false WHERE username=?");
            preparedStatement.setString(1, clientHandler.getUserName());
            preparedStatement.executeUpdate();
            System.out.println(clientHandler.getUserName()  + " set offline");
            clients.remove(clientHandler.getUserName());
        } catch (SQLException e){
            System.out.println("Error in setting client offline");
        }

    }

    /** Determine whether a user is online
     * @param username the username of the client
     * @return True - User is online. False - User is offline
     */
    private static boolean isUserOnline(String username){
        boolean result = false;
        try{
            PreparedStatement preparedStatement = connect.prepareStatement("SELECT connected FROM user_info WHERE username=?");
            preparedStatement.setString(1,username);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next())
                result = rs.getBoolean("connected");
        } catch(SQLException e){
            System.out.println("Error in isUserOnline");
            System.out.println(e.getMessage());
        }
        return result;
    }

    /** Get a status of the occupancy of all active rooms
     * @return a String of all rooms and the number of clients inside
     */
    public static String getRoomOccupancy(){
        StringBuilder stringBuilder = new StringBuilder("ROOMS\n--------------\n");
        for (Room room: rooms){
            stringBuilder.append(room).append("\n");
        }
        stringBuilder.append("--------------");
        return stringBuilder.toString();
    }

    /** Broadcast a message to all clients in every room.
     * @param message text to broadcast
     */
    private static void serverBroadcast(Message message){
        for(Room room: rooms){
            room.broadcast(null, message);
        }
    }

    /** Determine whether a password matches the one in RoomChatDatabase
     * @param user username of the client
     * @param password password of the client
     * @return True - Correct password. False - Incorrect password
     */
    private static boolean VerifyPassword(String user, String password){
        String hashedPassword = hashPassword(password);
        boolean result = false;
        try {
            PreparedStatement preparedStatement = connect.prepareStatement("SELECT password FROM user_info WHERE username=?");
            preparedStatement.setString(1, user);
            ResultSet rs = preparedStatement.executeQuery();
            if(rs.next())
                result =  rs.getString("password").equals(hashedPassword);
        } catch (SQLException e){
            System.out.println("error in verify password");
        }
        return result;
    }

    /** Register a client to the RoomChatDatabase
     * @param client The client clientHandler object
     * @param username The client's username
     * @param password The client's password
     * @return Message object on the success of the registration
     */
    public static Message registerUser(ClientHandler client, String username, String password) {
        if(isNameTaken(username))
            return new Message("Username already exists", Message.TYPE.LOGIN_DENIED);
        if(password.length() < 8)
            return new Message("Password must be at least 8 characters", Message.TYPE.LOGIN_DENIED);
        try {
            PreparedStatement createUser = connect.prepareStatement("INSERT INTO user_info VALUES(?,?,false)");
            createUser.setString(1, username);
            createUser.setString(2, hashPassword(password));
            createUser.executeUpdate();
            System.out.println(username + " has been put in the database");
        } catch (SQLException e){
            System.out.println("error in create user");
        }
        return logInUser(client, username, password);

    }

    /** Log in a client
     * @param client The client clientHandler object
     * @param username The client's username
     * @param password The client's password
     * @return Message on the status of the log in
     */
    public static Message logInUser(ClientHandler client, String username, String password){
        if(!VerifyPassword(username, password)) {
            return new Message("Incorrect user name or password", Message.TYPE.LOGIN_DENIED);
        }
        if(isUserOnline(username)){
            return new Message("User is already logged in", Message.TYPE.LOGIN_DENIED);
        }
        client.setUserName(username);
        setClientOnline(client);
        return new Message(Message.TYPE.LOGIN_SUCCESS);
    }

    /** Connect to the RoomChatDatabase
     */
    private static void connectToDatabase() {
        try {
            connect = DriverManager.getConnection("jdbc:derby:RoomChatDatabase; create = true");
            System.out.println("Connected to database.");
            Statement state = connect.createStatement();
            System.out.println("Statement created.");
            DatabaseMetaData dbm = connect.getMetaData();
            System.out.println("MetaData created.");
            ResultSet result = dbm.getTables(null, null, "USER_INFO", null);
            System.out.println("ResultSet created.");
            if (result.next()) {
                System.out.println("user_info exists");
            } else {
                state.execute("create table user_info(username varchar(100) not NULL, password varchar(100), connected boolean, PRIMARY KEY(username))");
                System.out.println("user_info created");
            }
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    /** Disconnect from the RoomChatDatabase
     */
    private static void disconnectFromDatabase(){
        try{
            connect.close();
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch(SQLException ignored){
        }
    }

    /** Hash a password with SHA-256 Algorithm
     * Code is from the following site:
     * https://howtodoinjava.com/java/java-security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
     * @param password The password to be hashed
     * @return The hashed password
     */
    private static String hashPassword(String password){
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error in generating password");
            e.printStackTrace();
        }
        return generatedPassword;
    }

    /** Mark all users offline
     *  This is used on startup to ensure users can log in properly after server reboot.
     */
    private static void markAllUsersOffline() {
        try{
            Statement statement = connect.createStatement();
            statement.execute("UPDATE user_info SET connected=false");
            System.out.println("All users marked offline in user_info");
        } catch(SQLException e){
            System.out.println("Error in setting all users to offline");
        }
    }
}
