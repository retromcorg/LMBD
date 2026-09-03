package com.johnymuffin.jban.core.evidence;

import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;

import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImageEvidence implements IEvidence {
    private final UUID adminUUID;
    private final long issued;
    private volatile boolean active;
    private volatile String url;
    private volatile EvidenceVisibility visibility;
    private final ReadWriteLock lock;

    public ImageEvidence(UUID adminUUID, long issued, boolean active, String url) {
        this(adminUUID, issued, active, url, EvidenceVisibility.PUBLIC);
    }

    public ImageEvidence(UUID adminUUID, long issued, boolean active, String url, EvidenceVisibility visibility) {
        this.adminUUID = adminUUID;
        this.issued = issued;
        this.active = active;
        this.url = url;
        this.visibility = visibility == null ? EvidenceVisibility.PUBLIC : visibility;
        this.lock = new ReentrantReadWriteLock();
    }

    public ImageEvidence(JsonObject evidenceData) {
        this(
                UUID.fromString(JsonUtil.getString(evidenceData, "adminUUID")),
                JsonUtil.getLong(evidenceData, "issued", 0L),
                JsonUtil.getBoolean(evidenceData, "active", false),
                JsonUtil.getString(evidenceData, "url"),
                EvidenceVisibility.fromValue(JsonUtil.getString(evidenceData, "visibility"))
        );
    }

    public JsonObject getJSON() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", 1);
        jsonObject.addProperty("adminUUID", adminUUID.toString());
        jsonObject.addProperty("issued", issued);
        lock.readLock().lock();
        try {
            jsonObject.addProperty("active", active);
            jsonObject.addProperty("url", url);
            jsonObject.addProperty("visibility", visibility.name());
        } finally {
            lock.readLock().unlock();
        }
        return jsonObject;
    }

    public int getType() {
        return 1;
    }

    public String  getEvidenceUUID() {
        lock.readLock().lock();
        try {
            return UUID.nameUUIDFromBytes((adminUUID.toString() + issued + active + getType()).getBytes()).toString().substring(0, 6);
        } finally {
            lock.readLock().unlock();
        }
    }

    public UUID getAdminUUID() {
        return adminUUID;
    }

    public long getIssued() {
        return issued;
    }

    public boolean isActive() {
        lock.readLock().lock();
        try {
            return active;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setActive(boolean active) {
        lock.writeLock().lock();
        try {
            this.active = active;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getUrl() {
        lock.readLock().lock();
        try {
            return url;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setUrl(String url) {
        lock.writeLock().lock();
        try {
            this.url = url;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public EvidenceVisibility getVisibility() {
        lock.readLock().lock();
        try {
            return visibility;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setVisibility(EvidenceVisibility visibility) {
        lock.writeLock().lock();
        try {
            this.visibility = visibility == null ? EvidenceVisibility.PUBLIC : visibility;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
