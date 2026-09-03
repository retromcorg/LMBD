package com.johnymuffin.jban.core;

import com.google.gson.JsonObject;

import java.util.UUID;

public class Mute extends EnforcementAction {
    public Mute(UUID playerUUID, UUID adminUUID, String reason, Long expiry, String serverName, String muteID, boolean pardoned, long timeIssued) {
        super(playerUUID, adminUUID, reason, expiry, serverName, muteID, pardoned, timeIssued);
    }

    public Mute(JsonObject actionData) {
        super(actionData);
    }

    public JsonObject getMuteJson() {
        return toJSON(true);
    }

    public JsonObject getPublicMuteJson() {
        return toPublicJSON();
    }

    @Override
    public EnforcementType getType() {
        return EnforcementType.MUTE;
    }

    @Override
    protected String getIdFieldName() {
        return "muteID";
    }

    @Override
    protected String getClearedFieldName() {
        return "pardoned";
    }

    public boolean isMuteActive() {
        return isActive();
    }

    public String getMuteID() {
        return getActionID();
    }

    public boolean isPardoned() {
        return isCleared();
    }

    public void setPardoned(boolean pardoned) {
        setCleared(pardoned);
    }
}
