package fr.btn.utils;

import fr.btn.entities.UserEntity;
import fr.btn.securityUtils.Cryptographer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;


public class Utils {

    private static final int EXP_IN_MILLIS = 3 * 60 * 1000;

    private Utils() {}
    public static String generateEncodedStringWithUserData(List<String> data) {
        /*Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);

        long expInMilliseconds = calendar.getTimeInMillis();*/

        StringBuilder stringBuilder = new StringBuilder();

        for(String part : data)
            stringBuilder.append(part + "|");
    try {
            return Cryptographer.encode(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static List<String> decodeAndExtractData(String encodedData) {
        //System.out.println("encoded-data=" + encodedData);

        try {
            String decodedData = Cryptographer.decode(encodedData);
            //System.out.println("decoded-data=" + decodedData);

            StringBuilder builder = new StringBuilder();
            List<String> parts = new ArrayList<>();

            int p = 0;
            while(p < decodedData.length()) {
                if(decodedData.charAt(p) == '|') {
                    String attribute = builder.toString();
                    parts.add(attribute);
                    builder.delete(0, attribute.length());
                }
                else
                    builder.append(decodedData.charAt(p));

                p++;
            }

            return parts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isAccountExpired(UserEntity account) {
        LocalDateTime start = account.getConfirmDateTime();

        if(start == null)
            return false;

        LocalDateTime end = LocalDateTime.now();

        Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();

        return endInstant.toEpochMilli() - startInstant.toEpochMilli() >= EXP_IN_MILLIS;
    }
}
