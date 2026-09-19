package com.johnymuffin.jban.beta.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class BanEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final UUID target;
    private final UUID admin;
    private final String banReason;
    private final Long banDuration; // in milliseconds, null for permanent
    private final String banId;


    public BanEvent(UUID target, UUID admin, String banReason, @Nullable Long banDuration, String banId) {
        super();
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

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
