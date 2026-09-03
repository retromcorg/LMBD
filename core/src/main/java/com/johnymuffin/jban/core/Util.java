package com.johnymuffin.jban.core;

import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Pattern;

public class Util {
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 5000;
    private static final int READ_TIMEOUT_MILLISECONDS = 5000;


    public static boolean validUUID(String uuid) {
        return Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$").matcher(uuid).matches();
    }

    public static String getSHA256(String message) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        sha256.reset();
        sha256.update(message.getBytes());
        byte[] digest = sha256.digest();

        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1,
                digest));
    }

    public static boolean isURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Get domain from URL
    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    public static UUID toUUIDWithDashes(String uuid) {
        return UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }

    public static String postToURL(String data, String contentType, String postURL) throws Exception {
        //application/json
        URL url = new URL(postURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
        connection.getOutputStream().flush();
        connection.getOutputStream().close();

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();
        return response.toString();
    }

    public static boolean verifyJSONArguments(JsonObject jsonObject, String... arguments) {
        for (String s : arguments) {
            if (!jsonObject.has(s)) return false;
        }
        return true;
    }

    public static boolean isPlayerAuthorized(final CommandSender commandSender, final String permission) {
        if (commandSender.isOp()) {
            return true;
        }
        return commandSender.hasPermission(permission);

    }

    public static Long getUnixTimestamp(int day, int month, int year) {
        if(month >= 1) {
            month = month - 1;
        }
        Long unixTimestamp = 0L;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        unixTimestamp = calendar.getTimeInMillis() / 1000L;
        return unixTimestamp;
    }

    public static Ban[] filterBansToServer(Ban[] bans, String server) {
        ArrayList<Ban> filteredBans = new ArrayList<>();
        for (Ban ban : bans) {
            if (ban.getServerName().equals(server)) {
                filteredBans.add(ban);
            }
        }
        return filteredBans.toArray(new Ban[filteredBans.size()]);
    }

    public static boolean isHostname(String ipOrHostname) {
        //Return true if the string isn't an IP address
        return !Pattern.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$", ipOrHostname);
    }

    //Delete the text between two strings in a string with reoccurrences
    public static String deleteBetween(String str, String begin, String end) {
        int beginIndex = str.indexOf(begin);
        int endIndex = str.indexOf(end);
        while (beginIndex != -1 && endIndex != -1) {
            str = str.substring(0, beginIndex) + str.substring(endIndex + end.length());
            beginIndex = str.indexOf(begin);
            endIndex = str.indexOf(end);
        }
        return str;
    }

    public static String getIPFromHostname(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }


}
