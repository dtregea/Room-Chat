# Room chat
A javafx application which allows users to send messages to unique, joinable rooms

## Prerequisites

- Java 15
- JavaFX 11.0.2
- Apache Derby 10.15
- Windows


## How to set up (IntelliJ)
1. Navigate to File/Project Structure/Project Settings/
2. In /Libraries, add your javafx \bin folder with the '+'
3. In /Modules, under the "Export | Scope" window, click '+', and select "JARS or Directories".
4. Select the following from the derby lib\ folder: derby.jar, derbyclient.jar, derbytools.jar

## How to run
1. Run Server.java
2. Run ClientChat.java

## Connect to a server on a local network
1. Obtain computer running Server.java IPv4 address on the local network. This can be done on the command prompt by typing in "ipconfig" (Windows). For consistency, set up a static IP address on your computer.
2. On connecting computers, change the SERVER_IP variable in ClientChat.java to the servers ip address.
- Can use local host
- A computer running Server.java can also run ClientChat.java