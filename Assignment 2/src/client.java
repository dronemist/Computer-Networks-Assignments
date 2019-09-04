import java.io.*;
import java.net.*;
class TCPClient {


    public static void main(String argv[]) throws Exception
    {   String clientSendingProtocol;
        String clientReceivingProtocol;
        String sentence = "test";
        String modifiedSentence = "";

        BufferedReader inFromUser =
          new BufferedReader(new InputStreamReader(System.in));

        Socket outputToServerSocket = new Socket("localhost", 6788);
        Socket inputFromServerSocket = new Socket("localhost", 6788);
        DataOutputStream outToServer =
          new DataOutputStream(outputToServerSocket.getOutputStream());


        BufferedReader inFromServer =
          new BufferedReader(new
          InputStreamReader(inputFromServerSocket.getInputStream()));

        String username = "";
        //for registration
        while(!modifiedSentence.startsWith("REGISTERED TOSEND")) {
          System.out.println("Enter username");
          username = inFromUser.readLine();
          clientSendingProtocol = "REGISTER TOSEND " + username + "\n\n";

          outToServer.writeBytes(clientSendingProtocol);
          modifiedSentence = inFromServer.readLine();
          System.out.println(modifiedSentence);
          inFromServer.readLine(); //For the addditional \n sent by the Server
        }

        clientSendingProtocol = "REGISTER TORECV " + username + "\n\n";

        while(!modifiedSentence.startsWith("REGISTERED TORECV")){
          outToServer.writeBytes(clientSendingProtocol);
          modifiedSentence = inFromServer.readLine();
          System.out.println(modifiedSentence);
          inFromServer.readLine(); //For the addditional \n sent by the Server
        }

        //registration done


        MessageReceiver messageReceiver = new MessageReceiver(inputFromServerSocket, inFromServer);
        Thread receiver_thread = new Thread(messageReceiver);
        receiver_thread.start();

        MessageSender messageSender = new MessageSender(outputToServerSocket, outToServer, inFromUser);
        Thread sender_thread = new Thread(messageReceiver);
        sender_thread.start();

       // clientSocket.close();
       // clientSocket2.close();

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
  DataOutputStream outToServer;
  BufferedReader inFromUser;
  String clientSentence;
  String messageSendProtocol;


  MessageSender (Socket outputToServerSocket, DataOutputStream outToServer, BufferedReader inFromUser) {
    this.outputToServerSocket = outputToServerSocket;
    this.outToServer = outToServer;
    this.inFromUser = inFromUser;
  }

  public void run() {
    while(true) {
      try {
        this.clientSentence = inFromUser.readLine();
        String[] temp = this.clientSentence.split(" ");
        String message = "";
        String recipient;
        if(temp.length > 1 && temp[0].startsWith("@")){

          recipient = temp[0].substring(1);
          for(int i=1; i<temp.length; ++i){
            message = message + temp[i];
          }

          this.messageSendProtocol = "SEND " + recipient + "\n" + "Content-length: " + Integer.toString(message.length()) + "\n\n" + message;
          outToServer.writeBytes(this.messageSendProtocol);
        }






      } catch(Exception e) {
        try {
          this.outputToServerSocket.close();
        } catch(Exception ee) { }
          break;
      }

    }

  }

}
