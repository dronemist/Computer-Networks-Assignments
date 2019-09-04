import java.io.*;
import java.net.*;
import java.util.HashMap;

class TCPServer {

  HashMap<String, ClientHandler> users = new HashMap<>();

  public static void main(String argv[]) throws Exception
  {
    TCPServer server = new TCPServer();
    ServerSocket welcomeSocket = new ServerSocket(6788);
    welcomeSocket.setSoTimeout(20000);
    try {
      while(true)
      {
        Socket inputFromClientSocket = welcomeSocket.accept();
        System.out.println(inputFromClientSocket.getRemoteSocketAddress());
        Socket outputToClientSocket = welcomeSocket.accept();
        BufferedReader inFromClient =
          new BufferedReader(new
          InputStreamReader(inputFromClientSocket.getInputStream()));

        DataOutputStream outToClient =
          new DataOutputStream(outputToClientSocket.getOutputStream());

        ClientHandler clientHandler = new ClientHandler(inputFromClientSocket, outputToClientSocket, server, inFromClient, outToClient);
        Thread thread = new Thread(clientHandler);
        thread.start();
      }
    } catch(Exception e) {
      welcomeSocket.close();
    }
  }
}


class ClientHandler implements Runnable {

  String clientSentence;
  /// To check if another \n entered by user
  String clientSentenceTemp; 
  Socket inputFromClientSocket;
  Socket outputToClientSocket;
  TCPServer server;
  /// sending username of the client this client handler is assigned to
  String sendUsername;

  /// recieving username of the client this client handler is assigned to
  String receiveUsername;
  BufferedReader inFromClient;
  DataOutputStream outToClient;

  /// This function registers the clienthandler
  // NOTE: need to correct a few things
  ClientHandler (Socket inputFromClientSocket, Socket outputToClientSocket, TCPServer server, BufferedReader inFromClient, DataOutputStream outToClient) {
    this.inputFromClientSocket = inputFromClientSocket;
    this.outputToClientSocket = outputToClientSocket;
    this.inFromClient = inFromClient;
    this.server = server;
    this.outToClient = outToClient;
    this.sendUsername = null;
    this.receiveUsername = null;
  }

  boolean usernameWellFormed(String username) { 
    // true if username well formed, else false
    return username.chars().allMatch(Character::isLetterOrDigit);
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
        if(this.clientSentence.startsWith(commandToRegisterSend)){
          String usernameEntered;
          if(temp.length == 3){
            usernameEntered = temp[2];
          }
          else { 
            // if more spaces, then Malformed
            return error100Message;
          }
          if(usernameWellFormed(usernameEntered)){ 
            // register the user
            this.sendUsername = usernameEntered;
            return registeredSend + this.sendUsername + "\n\n";
          }
          else{
            return error100Message;
          }

        }
        else{
          // System.out.println(this.clientSentence);
          return error101SendUserMessage;
        }
      }
      else{ // if registerSend == false, then registerReceive
        if(this.clientSentence.startsWith(commandToRegisterReceive)){
          String usernameEntered;
          if(temp.length == 3){
            usernameEntered = temp[2];
          }
          else{ 
            return error100Message;
          }
          if(usernameWellFormed(usernameEntered)){ //register the user
            this.receiveUsername = usernameEntered;
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


  String processMessageSendFromClient(){
    String error102Message = "ERROR 102 UNABLE TO SEND\n\n";
    String error103Message = "ERROR 103 Header incomplete\n\n";


    try {
      this.clientSentence = inFromClient.readLine();
      String[] temp = this.clientSentence.split(" ");

      if(temp.length == 2 && temp[0].equals("SEND")){ 
        // To check whther first line is okay
        String recipient = null;
        String message = null;

        recipient = temp[1];

        this.clientSentence = inFromClient.readLine();
        temp = this.clientSentence.split(" ");
        // Checking if content-length header is okay and next line is blank
        if(temp.length == 2 && temp[0].equals("Content-length:") && inFromClient.readLine().length() == 0){ 
          int contentLength = Integer.parseInt(temp[1]);
          message = "";

          for(int i=0; i<contentLength; ++i){

            message = message + inFromClient.read();

          }
          return "SENT " + recipient + "\n\n";

        }
        else{
          return error103Message;
        }

      }
      else{ 
        // NOTE: We should also send some message to client so that connection is closed
        return error103Message;
      }
    }
    catch(Exception e){ 
      // What error to throw here
      return error103Message;
    }


  }

  public void run() {
    while(true) {
      try {
        // NOTE: Can a user register its senderusername again?


        String outputToClient = null;

        if(this.sendUsername == null)
        {
          // System.out.println(this.clientSentenceTemp.length());
          this.clientSentence = inFromClient.readLine();
          System.out.println(this.clientSentence);
          this.clientSentenceTemp = inFromClient.readLine(); // This should be a blank string, to check if \n coming in request
          outputToClient = register(true);

        } else if(this.receiveUsername == null)
        {
          // this.clientSentenceTemp = inFromClient.readLine(); // This should be a blank string, to check if \n entered by user
          this.clientSentence = inFromClient.readLine();
          System.out.println(this.clientSentence);
          this.clientSentenceTemp = inFromClient.readLine(); // This should be a blank string, to check if \n coming in request
          outputToClient = register(false);
        }
        else{ 
          // User registered, send and receive messages now



        }



        if(outputToClient != null){
          outToClient.writeBytes(outputToClient);
        }

      } catch(Exception e) {
        try {
          this.inputFromClientSocket.close();
          this.outputToClientSocket.close();
        } catch(Exception ee) { }
          break;
      }
    }
  }
}
