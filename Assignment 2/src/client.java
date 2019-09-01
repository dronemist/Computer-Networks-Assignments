import java.io.*; 
import java.net.*; 
class TCPClient { 

    public static void main(String argv[]) throws Exception 
    { 
        String sentence = "test"; 
        String modifiedSentence; 

        BufferedReader inFromUser = 
          new BufferedReader(new InputStreamReader(System.in)); 

        Socket clientSocket = new Socket("localhost", 8); 
        Socket clientSocket2 = new Socket("localhost", 8); 
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

