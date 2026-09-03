package com.johnymuffin.jban.core;

import com.google.gson.JsonObject;

import java.util.UUID;

public class Warning extends EnforcementAction {
    private boolean acknowledged;

    public Warning(UUID playerUUID, UUID adminUUID, String reason, String serverName, String warningID, boolean resolved, long timeIssued) {
        this(playerUUID, adminUUID, reason, serverName, warningID, resolved, resolved, timeIssued);
    }

    public Warning(UUID playerUUID, UUID adminUUID, String reason, String serverName, String warningID, boolean acknowledged, boolean resolved, long timeIssued) {
        super(playerUUID, adminUUID, reason, null, serverName, warningID, resolved, timeIssued);
        this.acknowledged = acknowledged;
    }

    public Warning(JsonObject actionData) {
        super(actionData);
        this.acknowledged = JsonUtil.getBoolean(actionData, "acknowledged", isResolved());
    }

    public JsonObject getWarningJson() {
        JsonObject jsonObject = toJSON(true);
        jsonObject.addProperty("acknowledged", acknowledged);
        return jsonObject;
    }

    public JsonObject getPublicWarningJson() {
        JsonObject jsonObject = toPublicJSON();
        jsonObject.addProperty("acknowledged", acknowledged);
        return jsonObject;
    }

    @Override
    public EnforcementType getType() {
        return EnforcementType.WARNING;
    }

    @Override
    protected String getIdFieldName() {
        return "warningID";
    }

    @Override
    protected String getClearedFieldName() {
        return "resolved";
    }

    public String getWarningID() {
        return getActionID();
    }

    public boolean isResolved() {
        return isCleared();
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setResolved(boolean resolved) {
        setCleared(resolved);
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
}
