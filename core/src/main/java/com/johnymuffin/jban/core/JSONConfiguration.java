package com.johnymuffin.jban.core;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JSONConfiguration {
    protected static final Gson GSON = new GsonBuilder().serializeNulls().create();

    protected File configFile;
    protected JsonObject jsonConfig;

    public JSONConfiguration(File file) {
        this.configFile = file;
        //Create directory
        if (!this.configFile.exists()) {
            this.configFile.getParentFile().mkdirs();
            jsonConfig = new JsonObject();
            saveFile();
        } else {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                jsonConfig = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            } catch (JsonParseException e) {
                System.out.println("Failed to load config file.");
                throw new RuntimeException(e + ": " + e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e + ": " + e.getMessage());
            }
        }
        saveFile();
    }


    public JsonElement get(String key) {
        return jsonConfig.get(key);
    }

    //Getters Start
    public JsonElement getConfigOption(String key) {
        return this.jsonConfig.get(key);
    }

    public String getConfigString(String key) {
        return JsonUtil.getString(jsonConfig, key);
    }

    public Integer getConfigInteger(String key) {
        return JsonUtil.has(jsonConfig, key) ? jsonConfig.get(key).getAsInt() : null;
    }

    public Long getConfigLong(String key) {
        return JsonUtil.has(jsonConfig, key) ? jsonConfig.get(key).getAsLong() : null;
    }

    public Double getConfigDouble(String key) {
        return JsonUtil.has(jsonConfig, key) ? jsonConfig.get(key).getAsDouble() : null;
    }

    public Boolean getConfigBoolean(String key) {
        return JsonUtil.has(jsonConfig, key) ? jsonConfig.get(key).getAsBoolean() : null;
    }

    //Getters End

    public boolean containsKey(String key) {
        return jsonConfig.has(key);
    }

    protected void generateConfigOption(String key, Object value) {
        if (!jsonConfig.has(key)) {
            jsonConfig.add(key, GSON.toJsonTree(value));
        }
    }

    protected void saveFile() {
        try (OutputStreamWriter file = new OutputStreamWriter(Files.newOutputStream(configFile.toPath()), StandardCharsets.UTF_8)) {
            GSON.toJson(jsonConfig, file);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
