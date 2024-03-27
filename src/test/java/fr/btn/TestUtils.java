package fr.btn;

import fr.btn.securityUtils.Cryptographer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class TestUtils {
    public static String generateEncodedStringWithUserData(List<String> data, int delay) {
        Instant now = LocalDateTime.now().plusMinutes(delay).atZone(ZoneId.systemDefault()).toInstant();

        StringBuilder stringBuilder = new StringBuilder();

        for(String part : data)
            stringBuilder.append(part).append("|");

        stringBuilder.append(now.toEpochMilli()).append('|');

        try {
            return Cryptographer.encode(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static LocalDateTime getLockAccessDateTime(int nbFails, int nbAttemptsMax, int lockMins) {
        int lockedMinutes = (nbFails - nbAttemptsMax + 1) * lockMins;

        return LocalDateTime.now().plusMinutes(lockedMinutes);
    }
}
