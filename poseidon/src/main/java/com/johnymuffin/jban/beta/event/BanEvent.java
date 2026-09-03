package com.johnymuffin.jban.beta.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BanEvent extends Event {
    private final UUID target;
    private final UUID admin;
    private final String banReason;
    private final Long banDuration; // in milliseconds, null for permanent
    private final String banId;


    public BanEvent(UUID target, UUID admin, String banReason, @Nullable Long banDuration, String banId) {
        super("LMBDBanEvent");
        this.target = target;
        this.admin = admin;
        this.banReason = banReason;
        this.banDuration = banDuration;
        this.banId = banId;
    }

    public UUID getTarget() {
        return target;
    }

    public UUID getAdmin() {
        return admin;
    }

    public String getBanReason() {
        return banReason;
    }

    @Nullable
    public Long getBanDuration() {
        return banDuration;
    }

    public String getBanId() {
        return banId;
    }
}
