package com.johnymuffin.jban.beta.exceptions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.johnymuffin.jban.core.JSONConfiguration;
import com.johnymuffin.jban.core.JsonUtil;

import java.io.File;
import java.util.UUID;

public class JBExceptions extends JSONConfiguration {
    private boolean isNew = false;

    public JBExceptions(File file) {
        super(file);
        isNew = file.exists();


    }


    public JsonArray getExceptionList() {
        return JsonUtil.getArray(this.jsonConfig, "exceptions");
    }


    private void saveExceptionList(JsonArray jsonArray) {
        this.jsonConfig.add("exceptions", jsonArray);
        this.saveFile();
    }


    public boolean isUserAnException(UUID uuid) {
        for (JsonElement entry : this.getExceptionList()) {
            UUID compare = UUID.fromString(entry.getAsString());
            if (compare.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeUserFromExceptions(UUID uuid) {
        JsonArray jsonArray = this.getExceptionList();
        boolean removed = jsonArray.remove(new JsonPrimitive(uuid.toString()));
        this.saveExceptionList(jsonArray);
        return removed;
    }

    public void addUserToExceptions(UUID uuid) {
        if (isUserAnException(uuid)) {
            return;
        }
        JsonArray jsonArray = getExceptionList();
        jsonArray.add(uuid.toString());
        this.saveExceptionList(jsonArray);
    }


}
