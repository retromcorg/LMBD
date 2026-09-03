package com.johnymuffin.jban.core.evidence;

import com.google.gson.JsonObject;

import java.util.UUID;

public interface IEvidence {

    public JsonObject getJSON();

    public int getType();

    public String getEvidenceUUID();

    public boolean isActive();

    public void setActive(boolean active);

    public UUID getAdminUUID();

    public EvidenceVisibility getVisibility();

    public void setVisibility(EvidenceVisibility visibility);
}
