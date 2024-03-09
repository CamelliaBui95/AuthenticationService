package fr.btn.securityUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {

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

        String encoded = "fYDy939B8vWaJqrRw1NLm8zHGmGHBPTGB9gP5il9rtBN-HMQczvDk9-IKRBafe_e";

        byte[] decoded = Base64.getUrlDecoder().decode(encoded);

        System.out.println(new String(decoded));
    }
}
