import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

class TCPClient {
 
  MessageReceiver messageReceiver;
  MessageSender messageSender;
  public static void main(String argv[]) throws Exception
    {   
      String clientSendingProtocol;
      String clientReceivingProtocol;
      String sentence = "test";
      String modifiedSentence = "";
      // Generating public and private key
      KeyPair keyPair = CryptographyExample.generateKeyPair();
      BufferedReader inFromUser =
      new BufferedReader(new InputStreamReader(System.in));

      Socket sendSocket = new Socket("localhost", 6791);
      Socket recieveSocket = new Socket("localhost", 6791);
      DataOutputStream sendOutToServer =
        new DataOutputStream(sendSocket.getOutputStream());

      BufferedReader sendInFromServer =
      new BufferedReader(new
      InputStreamReader(sendSocket.getInputStream()));

      DataOutputStream recieveOutToServer =
        new DataOutputStream(recieveSocket.getOutputStream());

      BufferedReader recieveInFromServer =
        new BufferedReader(new
        InputStreamReader(recieveSocket.getInputStream()));

      String username = "";
      // for registration
      while(!modifiedSentence.startsWith("REGISTERED TOSEND")) {
        System.out.println("Enter username");
        username = inFromUser.readLine();
        clientSendingProtocol = "REGISTER TOSEND " + username + "\n\n";

        sendOutToServer.writeBytes(clientSendingProtocol);
        modifiedSentence = sendInFromServer.readLine();
        System.out.println(modifiedSentence);
        sendInFromServer.readLine(); 
        // For the addditional \n sent by the Server
      }

      clientSendingProtocol = "REGISTER TORECV " + username + "\n\n";

      while(!modifiedSentence.startsWith("REGISTERED TORECV")){
        sendOutToServer.writeBytes(clientSendingProtocol);
        modifiedSentence = sendInFromServer.readLine();
        System.out.println(modifiedSentence);
        sendInFromServer.readLine(); 
        // For the addditional \n sent by the Server
      }

      // converting public key to string
      PublicKey publicKey = keyPair.getPublic();
      byte[] encodedPublicKey = publicKey.getEncoded();
      String b64PublicKey = Base64.getEncoder().encodeToString(encodedPublicKey);

      clientSendingProtocol = "REGISTER PUBLICKEY " + b64PublicKey + "\n\n";

      while(!modifiedSentence.startsWith("REGISTERED PUBLICKEY")){
        sendOutToServer.writeBytes(clientSendingProtocol);
        modifiedSentence = sendInFromServer.readLine();
        System.out.println(modifiedSentence);
        sendInFromServer.readLine(); 
        // For the addditional \n sent by the Server
      }
      // registration done

      MessageReceiver messageReceiver = new MessageReceiver(recieveSocket, recieveInFromServer, recieveOutToServer, keyPair);
      Thread receiver_thread = new Thread(messageReceiver);
      System.out.println("Receiver started");
      receiver_thread.start();

      MessageSender messageSender = new MessageSender(sendSocket, sendOutToServer, inFromUser, sendInFromServer);
      Thread sender_thread = new Thread(messageSender);
      System.out.println("Sender started");
      sender_thread.start();
    }
}

class MessageReceiver implements Runnable {
  Socket recieveSocket;
  BufferedReader recieveInFromServer;
  DataOutputStream recieveOutToServer;
  String serverSentence;
  KeyPair keyPair;

  MessageReceiver (Socket recieveSocket, BufferedReader inFromServer, DataOutputStream outToServer,
  KeyPair keyPair) { 
    // Receives messages, sends acknowledgements
    this.recieveSocket = recieveSocket;
    this.recieveInFromServer = inFromServer;
    this.recieveOutToServer = outToServer;
    this.keyPair = keyPair;
  }

  byte[] getPrivateKeyInByte() {
    PrivateKey privateKey = this.keyPair.getPrivate();
    byte[] encodedPrivateKey = privateKey.getEncoded();
    return encodedPrivateKey;
  }

  public void run() {
    while(true) {
      try {
        String error103Message = "ERROR 103 Header incomplete\n\n";
        this.serverSentence = this.recieveInFromServer.readLine();
        String[] temp = this.serverSentence.split(" ");

        if(temp.length == 2 && temp[0].equals("FORWARD")) { 
          // To check whther first line is okay
          String sender = null;
          String message = null;

          sender = temp[1];

          this.serverSentence = this.recieveInFromServer.readLine();
          // System.out.println(this.serverSentence);
          temp = this.serverSentence.split(" ");
          if(temp.length == 2 
          && temp[0].equals("Content-length:") 
          && this.recieveInFromServer.readLine().length() == 0) { 

            // Checking if content-length header is okay and next line is blank
            int contentLength = Integer.parseInt(temp[1]);
            message = "";
            for(int i=0; i<contentLength; ++i) {
              message = message + (char)recieveInFromServer.read();
            }
            // Decrypting the message
            byte[] messageInBytes = java.util.Base64.getDecoder().decode(message);
            byte[] decryptedMessage = CryptographyExample.decrypt(this.getPrivateKeyInByte(), messageInBytes);
            message = new String(decryptedMessage);

            // Printing output
            System.out.println("#" + sender + ": " + message);

            // Now sending acknowledgement
            String acknowledgement = "RECEIVED " + sender + "\n\n";
            this.recieveOutToServer.writeBytes(acknowledgement);
            }
            else {
              this.recieveOutToServer.writeBytes(error103Message);
            }
        }
        else {
          this.recieveOutToServer.writeBytes(error103Message);
        }

      } catch(Exception e) {
        try {
          this.recieveSocket.close();
          break;
        } catch(Exception ee) { 
          break;
        }
          
      }
    }
  }
}

class MessageSender implements Runnable {
  Socket sendSocket;
  DataOutputStream outToServer;
  BufferedReader inFromUser;
  BufferedReader inFromServer;
  String clientSentence;
  String messageSendProtocol;


  MessageSender (Socket sendSocket, DataOutputStream outToServer, BufferedReader inFromUser
  , BufferedReader inFromServer) {  //Sends messages, receives acknowledgements
    this.sendSocket = sendSocket;
    this.outToServer = outToServer;
    this.inFromServer = inFromServer;
    this.inFromUser = inFromUser;
  }

  public void run() {
    while(true) {
      try {
        this.clientSentence = inFromUser.readLine();
        // System.out.println(clientSentence);
        String[] temp = this.clientSentence.split(" ");
        String message = "";
        String recipient;
        if(temp.length > 1 && temp[0].startsWith("@")) {
          recipient = temp[0].substring(1, temp[0].length() - 1);
          message = this.clientSentence.substring(1 + recipient.length() + 2);

          // asking for public key
          this.messageSendProtocol = "FETCHKEY " + recipient + "\n\n";
          outToServer.writeBytes(this.messageSendProtocol);
          String tempString = inFromServer.readLine();
          String tempArray[] = tempString.split(" ");  
          // If we get the public key
          if(tempArray[0].startsWith("PUBLICKEY") && inFromServer.readLine().equalsIgnoreCase("")) {

            // getting public key
            String publicKey = tempArray[1];
            byte[] publicKeyInBytes = Base64.getDecoder().decode(publicKey);

            // encrypting the message
            byte[] messageInBytes = message.getBytes();
            byte[] encryptedMessage = CryptographyExample.encrypt(publicKeyInBytes, messageInBytes);
            message = Base64.getEncoder().encodeToString(encryptedMessage);

            // sending to server
            this.messageSendProtocol = "SEND " + recipient + "\n" + "Content-length: " + Integer.toString(message.length()) + "\n\n" + message;
            outToServer.writeBytes(this.messageSendProtocol);

            // Server acknowledgement
            System.out.println(inFromServer.readLine());
            System.out.print(inFromServer.readLine());
          } else {
            System.out.println(tempString);
          }
        }
      } catch(Exception e) {
        // System.out.println("hi");
        try {
          this.sendSocket.close();
          break;
        } catch(Exception ee) { 
          break;
        }
      }
    }
  }

}
