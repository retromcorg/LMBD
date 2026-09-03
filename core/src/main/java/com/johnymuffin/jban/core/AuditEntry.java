package com.johnymuffin.jban.core;

import com.google.gson.JsonObject;

import java.util.UUID;

public class AuditEntry {
    private final UUID actorUUID;
    private final long issued;
    private final String action;
    private final String oldValue;
    private final String newValue;
    private boolean active;

    public AuditEntry(UUID actorUUID, long issued, String action, String oldValue, String newValue) {
        this(actorUUID, issued, action, oldValue, newValue, true);
    }

    public AuditEntry(UUID actorUUID, long issued, String action, String oldValue, String newValue, boolean active) {
        this.actorUUID = actorUUID;
        this.issued = issued;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.active = active;
    }

    public AuditEntry(JsonObject jsonObject) {
        this(
                UUID.fromString(JsonUtil.getString(jsonObject, "actorUUID")),
                JsonUtil.getLong(jsonObject, "issued", 0L),
                JsonUtil.getString(jsonObject, "action"),
                JsonUtil.getString(jsonObject, "oldValue"),
                JsonUtil.getString(jsonObject, "newValue"),
                JsonUtil.getBoolean(jsonObject, "active", true)
        );
    }

    public JsonObject toJSON() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("actorUUID", actorUUID.toString());
        jsonObject.addProperty("issued", issued);
        jsonObject.addProperty("action", action);
        jsonObject.addProperty("active", active);
        if (oldValue != null) {
            jsonObject.addProperty("oldValue", oldValue);
        }
        if (newValue != null) {
            jsonObject.addProperty("newValue", newValue);
        }
        return jsonObject;
    }

    public UUID getActorUUID() {
        return actorUUID;
    }

    public long getIssued() {
        return issued;
    }

    public String getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
