package fr.btn.securityUtils;


import com.aayushatharva.brotli4j.common.annotations.Local;
import fr.btn.utils.Utils;

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

        LocalDateTime now = LocalDateTime.now();

    }
}

        /*try {
            String username = userData.get(0);
            long expInMilliSecs = Long.parseLong(userData.get(2));

            Calendar calendar = Calendar.getInstance();
            long nowInMilliSecs = calendar.getTimeInMillis();

            if(expInMilliSecs - nowInMilliSecs <= 0)
                return false;

            return userRepository.count("username=?1", username) == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }*/
