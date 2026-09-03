package com.johnymuffin.jban.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.evidence.IEvidence;
import com.johnymuffin.jban.core.evidence.ImageEvidence;
import com.johnymuffin.jban.core.evidence.TextEvidence;
import com.johnymuffin.jban.core.evidence.EvidenceVisibility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class EnforcementAction {
    private final UUID playerUUID;
    private final UUID adminUUID;
    private String reason;
    private Long expiry;
    private final String serverName;
    private final String actionID;
    private final long timeIssued;
    private boolean cleared;
    private final ArrayList<IEvidence> evidences = new ArrayList<>();
    private final ArrayList<AuditEntry> auditEntries = new ArrayList<>();

    protected EnforcementAction(UUID playerUUID, UUID adminUUID, String reason, Long expiry, String serverName, String actionID, boolean cleared, long timeIssued) {
        this.playerUUID = playerUUID;
        this.adminUUID = adminUUID;
        this.reason = reason;
        this.expiry = expiry;
        this.serverName = serverName;
        this.actionID = actionID;
        this.cleared = cleared;
        this.timeIssued = timeIssued;
    }

    protected EnforcementAction(JsonObject actionData) {
        this.playerUUID = UUID.fromString(JsonUtil.getString(actionData, "playerUUID"));
        this.adminUUID = UUID.fromString(JsonUtil.getString(actionData, "adminUUID"));
        this.reason = JsonUtil.getString(actionData, "reason");
        this.serverName = JsonUtil.getString(actionData, "serverName");
        this.actionID = JsonUtil.getString(actionData, getIdFieldName());
        this.cleared = JsonUtil.getBoolean(actionData, getClearedFieldName(), false);
        if (JsonUtil.has(actionData, "expiry")) {
            this.expiry = actionData.get("expiry").getAsLong();
        } else {
            this.expiry = null;
        }
        if (JsonUtil.has(actionData, "timeIssued") && actionData.get("timeIssued").getAsLong() < (System.currentTimeMillis() / 1000L)) {
            this.timeIssued = actionData.get("timeIssued").getAsLong();
        } else {
            this.timeIssued = 1609459200L;
        }

        for (JsonElement evidenceRaw : JsonUtil.getArray(actionData, "evidence")) {
            if (!evidenceRaw.isJsonObject()) {
                continue;
            }
            JsonObject evidenceData = evidenceRaw.getAsJsonObject();
            int type = JsonUtil.getInt(evidenceData, "type", 0);
            if (type == 1) {
                evidences.add(new ImageEvidence(evidenceData));
            } else if (type == 2) {
                evidences.add(new TextEvidence(evidenceData));
            }
        }

        for (JsonElement auditRaw : JsonUtil.getArray(actionData, "audit")) {
            if (auditRaw.isJsonObject()) {
                auditEntries.add(new AuditEntry(auditRaw.getAsJsonObject()));
            }
        }
    }

    public JsonObject toJSON(boolean includeInactiveAudit) {
        JsonObject action = baseJSON();

        JsonArray evidence = new JsonArray();
        for (IEvidence evidenceEntry : evidences) {
            evidence.add(evidenceEntry.getJSON());
        }
        action.add("evidence", evidence);

        JsonArray audit = new JsonArray();
        for (AuditEntry auditEntry : auditEntries) {
            if (includeInactiveAudit || auditEntry.isActive()) {
                audit.add(auditEntry.toJSON());
            }
        }
        action.add("audit", audit);
        return action;
    }

    public JsonObject toPublicJSON() {
        JsonObject action = baseJSON();

        JsonArray evidence = new JsonArray();
        for (IEvidence evidenceEntry : evidences) {
            EvidenceVisibility visibility = evidenceEntry.getVisibility();
            if (evidenceEntry.isActive() && (visibility == null || visibility == EvidenceVisibility.PUBLIC)) {
                evidence.add(evidenceEntry.getJSON());
            }
        }
        action.add("evidence", evidence);
        return action;
    }

    private JsonObject baseJSON() {
        JsonObject action = new JsonObject();
        action.addProperty("type", getType().name());
        action.addProperty("playerUUID", playerUUID.toString());
        action.addProperty("reason", reason);
        action.addProperty("serverName", serverName);
        action.addProperty("adminUUID", adminUUID.toString());
        action.addProperty(getIdFieldName(), actionID);
        action.addProperty(getClearedFieldName(), cleared);
        action.addProperty("timeIssued", timeIssued);
        if (expiry != null) {
            action.addProperty("expiry", expiry);
        }
        return action;
    }

    public boolean isActive() {
        if (cleared) {
            return false;
        }
        if (expiry == null) {
            return true;
        }
        return expiry > (System.currentTimeMillis() / 1000L);
    }

    public void addAuditEntry(UUID actorUUID, String action, String oldValue, String newValue) {
        auditEntries.add(new AuditEntry(actorUUID, System.currentTimeMillis() / 1000L, action, oldValue, newValue));
    }

    public abstract EnforcementType getType();

    protected abstract String getIdFieldName();

    protected abstract String getClearedFieldName();

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public UUID getAdminUUID() {
        return adminUUID;
    }

    public String getReason() {
        return reason;
    }

    public Long getExpiry() {
        return expiry;
    }

    public String getServerName() {
        return serverName;
    }

    public String getActionID() {
        return actionID;
    }

    public long getTimeIssued() {
        return timeIssued;
    }

    public boolean isCleared() {
        return cleared;
    }

    public void setCleared(boolean cleared) {
        this.cleared = cleared;
    }

    public ArrayList<IEvidence> getEvidences() {
        return evidences;
    }

    public List<AuditEntry> getAuditEntries() {
        return auditEntries;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setExpiry(Long expiry) {
        this.expiry = expiry;
    }

}
