package com.johnymuffin.jban.core;

import com.google.gson.JsonObject;

import java.util.UUID;

public class Ban extends EnforcementAction {
    public Ban(UUID playerUUID, UUID adminUUID, String reason, Long expiry, String serverName, String banID, boolean pardoned, long timeIssued) {
        super(playerUUID, adminUUID, reason, expiry, serverName, banID, pardoned, timeIssued);
    }

    public Ban(JsonObject banData) {
        super(banData);
    }

    @Deprecated
    public JsonObject getBanJson() {
        return getBanJson(false);
    }

    public JsonObject getBanJson(boolean auditHistory) {
        return toJSON(auditHistory);
    }

    public JsonObject getPublicBanJson() {
        return toPublicJSON();
    }


    @Override
    public EnforcementType getType() {
        return EnforcementType.BAN;
    }

    @Override
    protected String getIdFieldName() {
        return "banID";
    }

    @Override
    protected String getClearedFieldName() {
        return "pardoned";
    }

    public boolean isBanActive() {
        return isActive();
    }

    public String getBanID() {
        return getActionID();
    }

    public boolean isPardoned() {
        return isCleared();
    }

    public void setPardoned(boolean pardoned) {
        setCleared(pardoned);
    }
}
