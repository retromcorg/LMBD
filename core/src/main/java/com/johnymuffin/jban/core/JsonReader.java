package com.johnymuffin.jban.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class JsonReader {
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 5000;
    private static final int READ_TIMEOUT_MILLISECONDS = 5000;

    public static JsonElement readJsonValueFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("Content-Type", "application/json");
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setRequestMethod("GET");
        }

        InputStream is = connection.getInputStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            return JsonParser.parseReader(rd);
        } finally {
            is.close();
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }
    
    public static JsonObject readJsonFromUrl(String url) throws IOException {
        return readJsonValueFromUrl(url).getAsJsonObject();
    }

}
