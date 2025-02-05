import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class MD5 {
    public static byte[] getMd5(byte[] input) 
    { 
        try { 
  
            // Static getInstance method is called with hashing MD5 
            MessageDigest md = MessageDigest.getInstance("SHA-256"); 
  
            // digest() method is called to calculate message digest 
            // of an input digest() return array of byte 
            byte[] messageDigest = md.digest(input); 
  
            return messageDigest; 
        }  
  
        // For specifying wrong message digest algorithms 
        catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        } 
    } 
}