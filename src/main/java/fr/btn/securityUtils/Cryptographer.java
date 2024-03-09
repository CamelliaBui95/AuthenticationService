package fr.btn.securityUtils;

import com.fasterxml.jackson.databind.ser.Serializers;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Cryptographer {
    private static final String ALGO = "AES";
    private static final String SECRET_KEY = "Put_Your_Secret_Key_Here";

    public static String encode(String data)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {

        // Instantiate a cipher with the indicated algorithm for encoding/decoding
        Cipher cipher = Cipher.getInstance(ALGO);

        // Create a secret key from the provided key in string format
        SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGO);

        // Initialize the cipher in encrypt mode
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encode data
        byte[] encodedBytes = cipher.doFinal(data.getBytes());

        // Transform the encoded data to a form that is safe to pass to a url
        byte[] urlSafeEncodedBytes = Base64.getUrlEncoder().encode(encodedBytes);

        return new String(urlSafeEncodedBytes);
    }

    public static String decode(String encodedData)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {

        Cipher cipher = Cipher.getInstance(ALGO);

        SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGO);

        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] base64DecodedBytes = Base64.getDecoder().decode(encodedData);

        byte[] decodedBytes = cipher.doFinal(base64DecodedBytes);

        return new String(decodedBytes);
    }
}
