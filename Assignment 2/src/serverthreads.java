import java.io.*;
import java.net.*;
import java.util.*;

class TCPServer {

  Hashtable<String, ClientHandler> users = new Hashtable<>();
  Hashtable<String, String> publicKeys = new Hashtable<>();
  public static void main(String argv[]) throws Exception
  {
    TCPServer server = new TCPServer();
    ServerSocket welcomeSocket = new ServerSocket(6791);
    Scanner input = new Scanner(System.in);
    int mode = -1;
    while(!(mode >= 1 && mode <= 3)){
      System.out.println("Enter which mode to start the server in");
      try{
        mode = input.nextInt(); 
        // mode = 1 is uncrypted, 2 is encrption without hash, 3 is encryption with hash
        if(!(mode >= 1 && mode <= 3)){
            System.out.println("Mode should be an integer between 1 and 3, try again");
        }
      }
      catch(Exception e){
        System.out.println("Mode should be an integer between 1 and 3, try again");
      }
    }
    input.close();
    try {
      while(true)
      {
        /// the send socket of the client is attached to this socket
        Socket sendClientSocket = welcomeSocket.accept();
        /// the recieve socket of the client is attached to this socket
        Socket recieveClientSocket = welcomeSocket.accept();



        BufferedReader sendInFromClient =
        new BufferedReader(new
        InputStreamReader(sendClientSocket.getInputStream()));

        DataOutputStream sendOutToClient =
        new DataOutputStream(sendClientSocket.getOutputStream());

        BufferedReader recieveInFromClient =
        new BufferedReader(new
        InputStreamReader(recieveClientSocket.getInputStream()));

        DataOutputStream recieveOutToClient =
        new DataOutputStream(recieveClientSocket.getOutputStream());

        //telling the mode to each client
        recieveOutToClient.writeBytes(Integer.toString(mode) + "\n");

        ClientHandler clientHandler = new ClientHandler(sendClientSocket, recieveClientSocket,
        server, sendInFromClient, sendOutToClient, recieveInFromClient, recieveOutToClient, mode);
        Thread thread = new Thread(clientHandler);
        thread.start();
      }
    } catch(Exception e) {
      System.out.println("Closing");
      welcomeSocket.close();
    }
  }
}


class ClientHandler implements Runnable {

  int mode;

  String clientSentence;
  /// To check if another \n entered by user
  String clientSentenceTemp;
  Socket sendClientSocket;
  Socket recieveClientSocket;
  TCPServer server;
  /// sending username of the client this client handler is assigned to
  String sendUsername;

  String publicKey;

  /// recieving username of the client this client handler is assigned to
  String receiveUsername;
  BufferedReader sendInFromClient;
  DataOutputStream sendOutToClient;
  BufferedReader recieveInFromClient;
  DataOutputStream recieveOutToClient;

  /// This function registers the clienthandler
  // NOTE: need to correct a few things
  ClientHandler (Socket inputFromClientSocket, Socket outputToClientSocket, TCPServer server,
  BufferedReader sendInFromClient, DataOutputStream sendOutToClient,
  BufferedReader recieveInFromClient, DataOutputStream recieveOutToClient, int mode) {
    this.mode = mode;
    this.sendClientSocket = inputFromClientSocket;
    this.recieveClientSocket = outputToClientSocket;
    this.sendInFromClient = sendInFromClient;
    this.sendOutToClient = sendOutToClient;
    this.recieveInFromClient = recieveInFromClient;
    this.recieveOutToClient = recieveOutToClient;
    this.server = server;
    this.sendUsername = null;
    this.receiveUsername = null;
    this.publicKey = null;
  }

  boolean usernameWellFormed(String username) {
    // true if username well formed, else false
    return username.chars().allMatch(Character::isLetterOrDigit);
  }
  /// registering the public key
  String registerPublicKey() {

    String[] temp = this.clientSentence.split(" ");

    // Registration commands
    String commandToRegisterKey = "REGISTER PUBLICKEY ";

    // Acknowledgement commands
    String registeredSend = "REGISTERED PUBLICKEY\n\n";

    // Error commands
    String error104MessageString = "ERROR 104 CANNOT REGISTER PUBLIC KEY\n\n";

    if(this.clientSentenceTemp.length() == 0
    && this.clientSentence.startsWith(commandToRegisterKey)
    && this.receiveUsername != null) {
        this.publicKey = temp[2];
        server.publicKeys.put(this.receiveUsername, temp[2]);
        return registeredSend;
    } else {
      return error104MessageString;
    }
  }

  /// registerSend true if sendUsername = null and false if sendUsername != null and receiveUsername = null
  String register(boolean registerSend) {
    String[] temp = this.clientSentence.split(" ");

    // Registration commands
    String commandToRegisterSend = "REGISTER TOSEND ";
    String commandToRegisterReceive = "REGISTER TORECV ";

    // Acknowledgement commands
    String registeredSend = "REGISTERED TOSEND ";
    String registeredRecv = "REGISTERED TORECV ";

    // error messages
    String error100Message = "ERROR 100 Malformed username\n\n";
    String error101SendUserMessage = "ERROR 101 No User registered\n\n";
    String error101RecvUserMessage = "ERROR 101 No User registered\n\n";

    // To check if another \n entered by user
    if(this.clientSentenceTemp.length() == 0){
      if(registerSend){
        // if registerSend is true then set sendUsername else set receiveUsername
        if(this.clientSentence.startsWith(commandToRegisterSend)) {
          String usernameEntered;
          if(temp.length == 3) {
            usernameEntered = temp[2];
          }
          else {
            // if more spaces, then Malformed
            return error100Message;
          }
          if(usernameWellFormed(usernameEntered)) {
            // register the user
            this.sendUsername = usernameEntered;
            return registeredSend + this.sendUsername + "\n\n";
          }
          else {
            return error100Message;
          }
        }
        else {
          // System.out.println(this.clientSentence);
          return error101SendUserMessage;
        }
      }
      else {
        // if registerSend == false, then registerReceive
        if(this.clientSentence.startsWith(commandToRegisterReceive)){
          String usernameEntered;
          if(temp.length == 3){
            usernameEntered = temp[2];
          }
          else {
            return error100Message;
          }
          if(usernameWellFormed(usernameEntered)){ //register the user
            this.receiveUsername = usernameEntered;

            server.users.put(usernameEntered, this); // User registered on the server

            return registeredRecv + this.receiveUsername + "\n\n";
          }
          else{
            return error100Message;
          }
        }
        else{
          return error101RecvUserMessage;
        }
      }
    }
    else {
      return error100Message;
    }
  }
  // MODE: argument of hashSignature

  void unregisterUser(){
    server.users.remove(this.receiveUsername);
    server.publicKeys.remove(this.receiveUsername);
    this.receiveUsername = null;
    this.sendUsername = null;
    this.publicKey = null;
  }

  String error103Message(){ // Whenever error 103, Close the connection
    String error103Message = "ERROR 103 Header incomplete\n\n";
    unregisterUser();

    return error103Message;
  }

  String messageForwardToClient(String recipient, String message, String hashSignature) {
    String error102Message = "ERROR 102 UNABLE TO SEND\n\n";
    String error106Message = "ERROR 106 No public key found\n\n";

    String messageForwardProtocol = "";
    // MODE: UNCOMMENT
    // messageForwardProtocol = "FORWARD " + this.sendUsername + "\n" + "Content-length: " + Integer.toString(message.length())
    // + "\n\n" + message;

    // DOUBT: I am not sure if public key needs to be sent in the same message
    // because we are checking for message integrity


    if(mode == 3){
      messageForwardProtocol = "FORWARD " + this.sendUsername + "\n" + "Content-length: " + Integer.toString(message.length())
      + "\n" + "SIGNATURE: " + hashSignature + "\n\n" + message;
    }
    else{
      messageForwardProtocol = "FORWARD " + this.sendUsername + "\n" + "Content-length: " + Integer.toString(message.length())
      + "\n\n" + message;
    }

    ClientHandler recipientClientHandler = this.server.users.get(recipient);
    // System.out.println(recipient);

    if(recipientClientHandler == null) {

      return error102Message;

    }
    else {
      try {
        recipientClientHandler.recieveOutToClient.writeBytes(messageForwardProtocol);
        // MODE:
        String inputFromReceiver[] = recipientClientHandler.recieveInFromClient.readLine().split(" ");
        // DOUBT: should we check directly send or wait for reciever to request public key
        String publicKey;

        if(inputFromReceiver.length == 2 &&
        inputFromReceiver[0].startsWith("FETCHKEY") &&
        recipientClientHandler.recieveInFromClient.readLine().equals(""))
        {
          publicKey = server.publicKeys.get(inputFromReceiver[1]);
          String publicKeyProtocol = "PUBLICKEY " + publicKey + "\n\n";
          if(publicKey != null) {
            recipientClientHandler.recieveOutToClient.writeBytes(publicKeyProtocol);
          } else {
            recipientClientHandler.recieveOutToClient.writeBytes(error106Message);
            return error102Message;
          }
        }
        // NOTE: Problem can occur in thread synchronization
        // DOUBT: should we send a different error if message not consistent?
        String acknowledgement = recipientClientHandler.recieveInFromClient.readLine();
        // System.out.println(acknowledgement);
        if(acknowledgement.equals("RECEIVED " + this.sendUsername) && recipientClientHandler.recieveInFromClient.readLine().equals("")){
          // Check if proper header format
          return acknowledgement;
        }
        else {
          return error102Message;
        }
      }
      catch(Exception e){
        return error102Message;
      }
    }
    // this.outputToClientSocket.writeBytes(messageForwardProtocol);
  }


  String processMessageSendFromClient(){

    // String error103Message = "ERROR 103 Header incomplete\n\n";
    String error106Message = "ERROR 106 No public Key found\n\n";
    String unregisteredMessage = "Unregistered " + this.receiveUsername + "\n\n";
    String error102Message = "ERROR 102 UNABLE TO SEND\n\n";

    try {
      this.clientSentence = sendInFromClient.readLine();

      String[] temp = this.clientSentence.split(" ");

      if(temp[0].equals("UNREGISTER")){
        if(sendInFromClient.readLine().equals("")){
          //unregistering
          unregisterUser();

          // recipientClientHandler.recieveOutToClient.writeBytes(unregisteredMessage);

          return unregisteredMessage;
        }
        else{
          return error103Message();
        }
      }

      if(temp.length == 2 && temp[0].equals("FETCHKEY")) {
        String recipient = temp[1];
        String publicKey = server.publicKeys.get(recipient);
        if(publicKey != null) {
          if(sendInFromClient.readLine().length() == 0){
            return "PUBLICKEY " + publicKey + "\n\n";
          }
          else {
            return error103Message();
          }
        } else {
          sendInFromClient.readLine();
          // For the extra \n
          if(mode == 1) {
            return error102Message;
          }
          return error106Message;
        }
      }
      if(temp.length == 2 && temp[0].equals("SEND")) {

        // To check whether first line is okay
        String recipient = null;
        String message = null;
        String hashSignature = null;
        recipient = temp[1];

        this.clientSentence = sendInFromClient.readLine();
        temp = this.clientSentence.split(" ");
        String hashSignatureArray[] = null;
        if(mode == 3){
           hashSignatureArray = sendInFromClient.readLine().split(" ");
        }

        // Checking if content-length header is okay and next line is blank
        if(temp.length == 2 && temp[0].equals("Content-length:")   && sendInFromClient.readLine().length() == 0
        && ( mode != 3 || hashSignatureArray.length == 2 && hashSignatureArray[0].equals("SIGNATURE:") )) // mode = 3 implies checkHash

        // MODE: See this if for mode

        {

          int contentLength = Integer.parseInt(temp[1]);
          message = "";
          for(int i = 0; i < contentLength; ++i){
            message = message + (char)sendInFromClient.read();
          }
          if(mode == 3){
            hashSignature = hashSignatureArray[1];
          }

          // Printing message
          System.out.println("Message sent: " + message);

          // Now forwarding the message
          String forwardResponse = messageForwardToClient(recipient, message, hashSignature);

          if(forwardResponse.startsWith("RECEIVED")) {
              return "SENT " + recipient + "\n\n";
          }
          else {
            return forwardResponse; // Error sent by forwardResponse
          }

        }
        else {
          return error103Message();
        }
      }
      else {
        // NOTE: We should also send some message to client so that connection is closed
        // System.out.println("here1");
        return error103Message();
      }
    }
    catch(Exception e){
      // What error to throw here
      return error103Message();
    }
  }

  public void run() {
    String error103Message = "ERROR 103 Header incomplete\n\n";
    while(true) {
      try {
        // NOTE: Can a user register its senderusername again?
        String outputToClient = null;

        if(this.sendUsername == null) {
          // System.out.println(this.clientSentenceTemp.length());
          this.clientSentence = sendInFromClient.readLine();
          System.out.println(this.clientSentence);
          this.clientSentenceTemp = sendInFromClient.readLine(); // This should be a blank string, to check if \n coming in request
          outputToClient = register(true);

        } else if(this.receiveUsername == null) {
          // this.clientSentenceTemp = inFromClient.readLine(); // This should be a blank string, to check if \n entered by user
          this.clientSentence = sendInFromClient.readLine();
          System.out.println(this.clientSentence);
          this.clientSentenceTemp = sendInFromClient.readLine(); // This should be a blank string, to check if \n coming in request
          outputToClient = register(false);
        } else if(this.publicKey == null) {
          this.clientSentence = sendInFromClient.readLine();
          System.out.println(this.clientSentence);
          this.clientSentenceTemp = sendInFromClient.readLine();
          outputToClient = registerPublicKey();
        } else {
          // User registered, send and receive messages now
          outputToClient = processMessageSendFromClient();
          System.out.println(outputToClient);
        }
        if(outputToClient != null) {
          sendOutToClient.writeBytes(outputToClient);
        }
        if(outputToClient.equals(error103Message)){ //Close the connection in this case from server side
          this.recieveInFromClient.close();
          this.recieveOutToClient.close();
          this.sendClientSocket.close();
          this.recieveClientSocket.close();
        }
      } catch(Exception e) {
        try {
          this.recieveInFromClient.close();
          this.recieveOutToClient.close();
          this.sendClientSocket.close();
          this.recieveClientSocket.close();
        } catch(Exception ee) { }
          break;
      }
    }
  }
}
