package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JBLang;
import com.johnymuffin.jban.beta.JohnyBans;
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

import static com.johnymuffin.jban.core.Util.postToURL;
import static com.johnymuffin.jban.core.Util.isPlayerAuthorized;
import static com.johnymuffin.jban.core.Util.verifyJSONArguments;

public class JMuteCommand extends JCommand {
    public JMuteCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
        if (!isPlayerAuthorized(cs, "johnybans.mute")) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }
        if (args.length < 2) {
            cs.sendMessage(ChatColor.RED + "Invalid Usage, /jmute (username) (reason) [duration]");
            return true;
        }

        String admin = ((cs instanceof Player) ? ((Player) cs).getName() : "Console");
        String playerName = args[0];
        OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        UUID uuid = cachedPlayer == null ? null : cachedPlayer.getUniqueId();
        if (uuid == null) {
            cs.sendMessage(ChatColor.RED + "Unable to find that player.");
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        Long expiry = null;
        int reasonLimit = args.length;
        Calendar until = parseExpiryFromDuration(args[args.length - 1]);
        if (until != null) {
            expiry = until.getTimeInMillis() / 1000L;
            reasonLimit = args.length - 1;
        }
        for (int i = 1; i < reasonLimit; i++) {
            reasonBuilder.append(args[i]).append(' ');
        }
        String reason = reasonBuilder.toString().trim();
        if (reason.isEmpty()) {
            cs.sendMessage(ChatColor.RED + "Please specify a mute reason.");
            return true;
        }

        UUID adminUUID = (cs instanceof Player) ? ((Player) cs).getUniqueId() : UUID.nameUUIDFromBytes(admin.getBytes());
        JsonObject postData = new JsonObject();
        postData.addProperty("username", playerName);
        postData.addProperty("uuid", uuid.toString());
        postData.addProperty("adminUsername", admin);
        postData.addProperty("key", plugin.getServerKey());
        postData.addProperty("adminUUID", adminUUID.toString());
        postData.addProperty("muteReason", reason);
        postData.addProperty("server", plugin.getServerName());
        if (expiry != null) {
            postData.addProperty("expiry", expiry);
        }

        final String url = plugin.getGetServerURL() + "/api/v1/muteUser";
        UUID finalUuid = uuid;
        Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                final String response = postToURL(postData.toString(), "application/json", url);
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    try {
                        JsonObject jsonResponse = JsonUtil.parseObject(response);
                        if (!verifyJSONArguments(jsonResponse, "error", "successful")) {
                            cs.sendMessage("Invalid json response");
                            return;
                        }
                        if (!JsonUtil.getBoolean(jsonResponse, "successful", false)) {
                            cs.sendMessage(ChatColor.RED + JsonUtil.getString(jsonResponse, "error_message", "A server error occurred."));
                            return;
                        }
                        cs.sendMessage(ChatColor.GREEN + "Successfully muted " + playerName + ". Mute ID: " + JsonUtil.getString(jsonResponse, "muteID"));
                        plugin.refreshMuteStatus(finalUuid);
                    } catch (Exception e) {
                        cs.sendMessage("Malformed JSON received from LMBD server.");
                        plugin.getLog().log(Level.WARNING, "An error occurred processing mute", e);
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

    private Calendar parseExpiryFromDuration(String duration) {
        if (duration == null) {
            return null;
        }
        if (isParableInteger(duration)) {
            Calendar until = Calendar.getInstance();
            final int min = Integer.parseInt(duration);
            until.add(12, min);
            return until;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.now();
            LocalDateTime time = changeDateTimeByString(dateTime, duration, true);
            long epoch = time.atZone(ZoneId.systemDefault()).toEpochSecond();
            long current = System.currentTimeMillis() / 1000L;
            Calendar until = Calendar.getInstance();
            final int min = (int) ((epoch - current) / 60L);
            until.add(12, min);
            return until;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime changeDateTimeByString(LocalDateTime current, String part, boolean increaseOrDecrease) {
        LocalDateTime newDateTime = null;

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
