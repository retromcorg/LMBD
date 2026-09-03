package com.johnymuffin.jban.core.evidence;

import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;

import java.util.UUID;

public class TextEvidence implements IEvidence {
    private final UUID adminUUID;
    private final long issued;
    private boolean active;
    private final String text;
    private EvidenceVisibility visibility;

    public TextEvidence(UUID adminUUID, long issued, boolean active, String text) {
        this(adminUUID, issued, active, text, EvidenceVisibility.PUBLIC);
    }

    public TextEvidence(UUID adminUUID, long issued, boolean active, String text, EvidenceVisibility visibility) {
        this.adminUUID = adminUUID;
        this.issued = issued;
        this.active = active;
        this.text = text;
        this.visibility = visibility == null ? EvidenceVisibility.PUBLIC : visibility;
    }

    public TextEvidence(JsonObject evidenceData) {
        this(
                UUID.fromString(JsonUtil.getString(evidenceData, "adminUUID")),
                JsonUtil.getLong(evidenceData, "issued", 0L),
                JsonUtil.getBoolean(evidenceData, "active", false),
                JsonUtil.getString(evidenceData, "text"),
                determineVisibility(evidenceData)
        );
    }

    public JsonObject getJSON() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", 2);
        jsonObject.addProperty("adminUUID", adminUUID.toString());
        jsonObject.addProperty("issued", issued);
        jsonObject.addProperty("active", active);
        jsonObject.addProperty("text", text);
        jsonObject.addProperty("visibility", visibility.name());
        return jsonObject;
    }

    public int getType() {
        return 2;
    }

    public UUID getAdminUUID() {
        return adminUUID;
    }

    public long getIssued() {
        return issued;
    }

    public boolean isActive() {
        synchronized (this) {
            return active;
        }
    }

    public void setActive(boolean active) {
        synchronized (this) {
            this.active = active;
        }
    }

    public String getText() {
        return text;
    }

    public EvidenceVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(EvidenceVisibility visibility) {
        this.visibility = visibility == null ? EvidenceVisibility.PUBLIC : visibility;
    }

    public String getEvidenceUUID() {
        return UUID.nameUUIDFromBytes((adminUUID.toString() + issued + active + getType()).getBytes()).toString().substring(0, 6);
    }

    private static EvidenceVisibility determineVisibility(JsonObject evidenceData) {
        if (JsonUtil.has(evidenceData, "visibility")) {
            return EvidenceVisibility.fromValue(JsonUtil.getString(evidenceData, "visibility"));
        }
        UUID systemUUID = UUID.nameUUIDFromBytes("SYSTEM!".getBytes());
        if (systemUUID.equals(UUID.fromString(JsonUtil.getString(evidenceData, "adminUUID")))) {
            return EvidenceVisibility.STAFF_ONLY;
        }
        return EvidenceVisibility.PUBLIC;
    }
}
