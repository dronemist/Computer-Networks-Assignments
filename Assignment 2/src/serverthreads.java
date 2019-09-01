import java.io.*; 
import java.net.*;
import java.util.Map; 

class TCPServer { 

  // Map<String, ClientHandler> users = new Map<String, ClientHandler>();

  public static void main(String argv[]) throws Exception 
  { 
    TCPServer server = new TCPServer();
    ServerSocket welcomeSocket = new ServerSocket(8); 
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
  String register(boolean registerSend) {
    String[] temp = this.clientSentence.split(" ");
    if(temp.length == 3)
    {
      if(registerSend && this.clientSentence.startsWith("REGISTER TOSEND")) 
      {
        this.sendUsername = temp[2];
        return "REGISTERED TOSEND " + this.sendUsername + "\n";
      } else if(!registerSend && this.clientSentence.startsWith("REGISTER TORECV"))
      {
        this.receiveUsername = temp[2];
        return "REGISTERED TORECV " + this.receiveUsername + "\n";
      } else {
        return "ERROR 100 Malformed username \n";
      }
    } else {
        return "ERROR 100 Malformed username \n";
    } 
    
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
        this.clientSentence = inFromClient.readLine(); 
        System.out.println(this.clientSentence);
        String outputToClient = "\n";
        if(this.sendUsername == null)
        {
          outputToClient = register(true);
        } else if(this.receiveUsername == null)
        {
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

