import java.io.*;
import java.net.*;
class TCPClient {



    public static void main(String argv[]) throws Exception
    {
        String sentence = "test";
        String modifiedSentence;

        BufferedReader inFromUser =
          new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket("localhost", 6788);
        Socket clientSocket2 = new Socket("localhost", 6788);
        DataOutputStream outToServer =
          new DataOutputStream(clientSocket.getOutputStream());


        BufferedReader inFromServer =
          new BufferedReader(new
          InputStreamReader(clientSocket2.getInputStream()));

        while(!sentence.equals("over")) {
          sentence = inFromUser.readLine();
          outToServer.writeBytes(sentence + '\n');
          modifiedSentence = inFromServer.readLine();
          System.out.print(modifiedSentence);
        }

       clientSocket.close();
       clientSocket2.close();

    }
}

class MessageReceiver implements Runnable{
  Socket inputFromServerSocket;
  BufferedReader inFromServer;
  String serverSentence;

  MessageReceiver (Socket inputFromServerSocket, BufferedReader inFromServer) {
    this.inputFromServerSocket = inputFromServerSocket;
    this.inFromServer = inFromServer;
  }

  public void run() {
    while(true) {
      try {

        this.serverSentence = inFromServer.readLine();
        System.out.println(this.serverSentence);

      } catch(Exception e) {
        try {
          this.inputFromServerSocket.close();
        } catch(Exception ee) { }
          break;
      }

    }

  }

}

class MessageSender implements Runnable{
  Socket outputToServerSocket;
  BufferedReader outToServer;
  BufferedReader inFromUser;
  String clientSentence;

  MessageSender (Socket outputToServerSocket, BufferedReader outToServer, BufferedReader inFromUser) {
    this.outputToServerSocket = outputToServerSocket;
    this.outToServer = outToServer;
    this.inFromUser = inFromUser;
  }

  public void run() {
    while(true) {
      try {

        this.clientSentence = inFromUser.readLine();
        outToServer.writeBytes(this.clientSentence);

      } catch(Exception e) {
        try {
          this.outputToServerSocket.close();
        } catch(Exception ee) { }
          break;
      }

    }

  }

}
