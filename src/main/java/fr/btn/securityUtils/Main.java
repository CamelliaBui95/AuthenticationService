package fr.btn.securityUtils;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        /*String testStr = "hello|world|at|";

        StringBuilder builder = new StringBuilder();

        List<String> words = new ArrayList<>();

        int p = 0;
        while(p < testStr.length()) {
            if(testStr.charAt(p) == '|') {
                String word = builder.toString();
                words.add(word);
                builder.delete(0, word.length());
            }
            else
                builder.append(testStr.charAt(p));

            p++;
        }

        for(String word : words)
            System.out.println(word);*/

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        String nowStr = now.toString();

        //System.out.println(nowStr);


        //System.out.println(nowDate);

        calendar.add(Calendar.MINUTE, 10);

        Date expiration = calendar.getTime();

        //System.out.println(expiration);

        /*System.out.println(calendar.getTimeInMillis());
        System.out.println(calendar.getTime().getTime());
        System.out.println(expiration.getTime() - now.getTime() >= 0);*/

        String str1 = "J4p8r5mpbeOdkfcCK8k71rMsl93PQdqpUoV1UiEP20w%3D";
        String str2 = "J4p8r5mpbeOdkfcCK8k71rMsl93PQdqpUoV1UiEP20w%3D";


        //java.lang.IllegalArgumentException: Illegal base64 character 25
        byte[] decodedBytes = Base64.getUrlDecoder().decode("J4p8r5mpbeOdkfcCK8k71rMsl93PQdqpUoV1UiEP20w%3D".getBytes(StandardCharsets.UTF_8));
        String decodedStr = new String(decodedBytes);

        System.out.println(decodedStr);
    }
}
