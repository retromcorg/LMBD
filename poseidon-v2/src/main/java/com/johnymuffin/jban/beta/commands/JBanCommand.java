package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JBLang;
import com.johnymuffin.jban.beta.JohnyBans;
import com.johnymuffin.jban.beta.event.BanEvent;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.UUID;
import java.util.logging.Level;

import static com.johnymuffin.jban.core.Util.*;

public class JBanCommand extends JCommand {
    private String admin;

    public JBanCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
        this.admin = ((cs instanceof Player) ? ((Player) cs).getName() : "Console");
        if (!isPlayerAuthorized(cs, "johnybans.ban")) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            cs.sendMessage(ChatColor.RED + "Invalid Usage, /jban (username) (reason) (duration)");
            return true;
        }


        String playerName = args[0];
        String reason = null;
        Calendar until = null;
        Player player = null;
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(playerName)) {
                player = p;
                playerName = p.getName();
            }
        }

        OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        UUID uuid = cachedPlayer == null ? null : cachedPlayer.getUniqueId();
        if (uuid == null) {
            String msg = JBLang.getInstance().getMessage("player_not_found_full");
            msg = msg.replace("%username%", playerName);
            cs.sendMessage(msg);
            return true;
        }

        //Ban could be a tempban
        if (args.length > 1) {
            int to = args.length - 1;
            if (isParableInteger(args[args.length - 1])) {
                until = Calendar.getInstance();
                final int min = Integer.parseInt(args[args.length - 1]);
                until.add(12, min);
            } else {
                try {
                    //Tempban
                    LocalDateTime dateTime = LocalDateTime.now();
                    LocalDateTime time = changeDateTimeByString(dateTime, args[args.length - 1], true);
                    long epoch = time.atZone(ZoneId.systemDefault()).toEpochSecond();
                    long current = System.currentTimeMillis() / 1000L;
                    //Turn into Easy Ban Format
                    until = Calendar.getInstance();
                    final int min = (int) ((epoch - current) / 60L);
                    until.add(12, min);
                    to = args.length - 1;
                } catch (Exception e) {
                    //Most likely a Perm ban at this point :(
                    until = null;
                    to = args.length;
                }
            }
            String tmp = "";
            for (int i = 1; i < to; ++i) {
                tmp = tmp + args[i] + " ";
            }
            if (tmp.length() > 0) {
                reason = tmp;
            }
        }
        //Make Sure Ban Reason Exists
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Ban Reason Not Specified";
        }

        UUID adminUUID;
        if (cs instanceof Player) {
            adminUUID = ((Player) cs).getUniqueId();
        } else {
            adminUUID = UUID.nameUUIDFromBytes(admin.getBytes());
        }


        final JsonObject postData = new JsonObject();
        final String url = plugin.getGetServerURL() + "/api/v1/banUser";
        postData.addProperty("username", playerName);
        postData.addProperty("uuid", uuid.toString());
        postData.addProperty("adminUsername", admin);
        postData.addProperty("key", plugin.getServerKey());
        postData.addProperty("adminUUID", adminUUID.toString());
        postData.addProperty("banReason", reason);
        if (until != null) {
            postData.addProperty("expiry", (until.getTimeInMillis() / 1000L));
        }
        postData.addProperty("server", plugin.getServerName());

        String finalPlayerName = playerName;
        String finalReason = reason;
        Calendar finalUntil = until;
        Player finalPlayer = player;

        UUID finalUuid = uuid;
        String finalReason1 = reason;
        Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                final String response = postToURL(postData.toString(), "application/json", url);
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    JsonObject jsonResponse;
                    try {
                        jsonResponse = JsonUtil.parseObject(response);
                        if (!verifyJSONArguments(jsonResponse, "error", "successful")) {
                            cs.sendMessage("Invalid json response");
                            plugin.getLog().log(Level.WARNING, "Invalid response on ban, " + jsonResponse.toString());
                            return;
                        }
                        if (!JsonUtil.getBoolean(jsonResponse, "successful", false)) {
                            if (JsonUtil.has(jsonResponse, "error_message")) {
                                cs.sendMessage(ChatColor.RED + "Server returned error: " + JsonUtil.getString(jsonResponse, "error_message"));
                            } else {
                                cs.sendMessage(ChatColor.RED + "A unknown server error occurred.");
                            }
                            return;
                        }

                        if (!JsonUtil.getBoolean(jsonResponse, "error", false)) {
                            String banID = JsonUtil.getString(jsonResponse, "banID");
                            cs.sendMessage(ChatColor.GREEN + "Successfully banned " + finalPlayerName + ". Ban ID: " + banID);
                            plugin.logger(Level.INFO, finalPlayerName + " has been banned by " + admin);
                            if (finalPlayer != null) {
                                finalPlayer.kickPlayer(ChatColor.RED + "You have been banned");
                            }


                            // Fire the Ban Event
                            Long expiry = null;
                            if (finalUntil != null) {
                                expiry = finalUntil.getTimeInMillis() / 1000L;
                            }

                            BanEvent banEvent = new BanEvent(finalUuid, adminUUID, finalReason1, expiry, banID);
                            Bukkit.getServer().getPluginManager().callEvent(banEvent);

                        } else {
                            cs.sendMessage("An error occurred processing that command");
                            plugin.getLog().log(Level.WARNING, "Error on ban, " + jsonResponse.toString());
                        }

                    } catch (Exception e) {
                        cs.sendMessage("Sorry, an error occurred. Malformed JSON received from LMBD server.");
                        plugin.getLog().log(Level.WARNING, "An error occurred processing ban", e);
                    }
                });
            } catch (Exception e) {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    cs.sendMessage("Unable to establish a connection to the LMBD webserver");
                    plugin.getLog().log(Level.WARNING, "Unable to establish connection", e);
                });
            }
        });


        return true;
    }


    private java.time.LocalDateTime changeDateTimeByString(java.time.LocalDateTime current, String part, boolean increaseOrDecrease) {
        java.time.LocalDateTime newDateTime = null;

        Integer amount = Integer.parseInt(part.replaceAll("\\D", ""));
        if (part.contains("S") || part.contains("s"))
            newDateTime = increaseOrDecrease ? current.plusSeconds(amount) : current.minusSeconds(amount);
        else if (part.contains("m"))
            newDateTime = increaseOrDecrease ? current.plusMinutes(amount) : current.minusMinutes(amount);
        else if (part.contains("h") || part.contains("H"))
            newDateTime = increaseOrDecrease ? current.plusHours(amount) : current.minusHours(amount);
        else if (part.contains("D") || part.contains("d"))
            newDateTime = increaseOrDecrease ? current.plusDays(amount) : current.minusDays(amount);
        else if (part.contains("W") || part.contains("w"))
            newDateTime = increaseOrDecrease ? current.plusDays(amount * 7) : current.minusDays(amount * 7);
        else if (part.contains("M"))
            newDateTime = increaseOrDecrease ? current.plusMonths(amount) : current.minusMonths(amount);
        else if (part.contains("Y") || part.contains("y"))
            newDateTime = increaseOrDecrease ? current.plusYears(amount) : current.minusYears(amount);
        else {

        }

        return newDateTime;
    }

    private boolean isParableInteger(final String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }
}
