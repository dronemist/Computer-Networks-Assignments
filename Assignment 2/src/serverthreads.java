import java.io.*;
import java.net.*;
import java.util.Map;

class TCPServer {

  // Map<String, ClientHandler> users = new Map<String, ClientHandler>();

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
  String clientSentenceTemp; //To check if another \n entered by user
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

  boolean usernameWellFormed(String username){ // true if username well formed, else false
    return username.chars().allMatch(Character::isLetterOrDigit);
  }

  String register(boolean registerSend) { //registerSend true if sendUsername = null and false if sendUsername != null and receiveUsername = null
    String[] temp = this.clientSentence.split(" ");

    // Registration commands
    String commandToRegisterSend = "REGISTER TOSEND ";
    String commandToRegisterReceive = "REGISTER TORECV ";

    //Acknowledgement commands
    String registeredSend = "REGISTERED TOSEND ";
    String registeredRecv = "REGISTERED TORECV ";

    //error messages
    String error100Message = "ERROR 100 Malformed username \n \n";
    String error101SendUserMessage = "ERROR 101 No sendUser registered \n \n";
    String error101RecvUserMessage = "ERROR 101 No receiveUser registered \n \n";


    if(clientSentenceTemp.length() == 0){ //To check if another \n entered by user
      if(registerSend){ // if registerSend is true then set sendUsername else set receiveUsername
        if(this.clientSentence.startsWith(commandToRegisterSend)){
          String usernameEntered;
          if(temp.length == 3){
            usernameEntered = temp[2];
          }
          else{ // if more spaces, then Malformed
            return error100Message;
          }
          if(usernameWellFormed(usernameEntered)){ //register the user
            this.sendUsername = usernameEntered;
            return registeredSend + this.sendUsername + "\n \n";
          }
          else{
            return error100Message;
          }

        }

        else{
          System.out.println(this.clientSentence);
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
            return registeredRecv + this.receiveUsername + "\n \n";
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
    else{
      return error100Message;
    }


    // if(temp.length == 3)
    // {
    //   if(registerSend && this.clientSentence.startsWith("REGISTER TOSEND"))
    //   {
    //     this.sendUsername = temp[2];
    //     return "REGISTERED TOSEND " + this.sendUsername + "\n";
    //   } else if(!registerSend && this.clientSentence.startsWith("REGISTER TORECV"))
    //   {
    //     this.receiveUsername = temp[2];
    //     return "REGISTERED TORECV " + this.receiveUsername + "\n";
    //   } else {
    //     return "ERROR 100 Malformed username \n";
    //   }
    // } else {
    //     return "ERROR 100 Malformed username \n";
    // }

  }

  ClientHandler (Socket inputFromClientSocket, Socket outputToClientSocket, TCPServer server, BufferedReader inFromClient, DataOutputStream outToClient) {
    this.inputFromClientSocket = inputFromClientSocket;
    this.outputToClientSocket = outputToClientSocket;
    this.inFromClient = inFromClient;
    this.server = server;
    this.outToClient = outToClient;
    this.sendUsername = null;
    this.receiveUsername = null;
  }

  public void run() {
    while(true) {
      try {
        // NOTE: better to read character by character
        // NOTE: Can a user register its senderusername again?

        this.clientSentence = inFromClient.readLine();
        System.out.println(this.clientSentence);
        String outputToClient = "\n";
        this.clientSentenceTemp = ""; // This should be a blank string, to check if \n entered by user
        if(this.sendUsername == null)
        {
          outputToClient = register(true);

        } else if(this.receiveUsername == null)
        {
          // this.clientSentenceTemp = inFromClient.readLine(); // This should be a blank string, to check if \n entered by user
          outputToClient = register(false);
        }

        // if(clientSentence.equals("over"))
        // {
        //   this.inputFromClientSocket.close();
        //   this.outputToClientSocket.close();
        //   break;
        // }
        // capitalizedSentence = clientSentence.toUpperCase() + '\n';
        outToClient.writeBytes(outputToClient);

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
