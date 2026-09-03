package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JBLang;
import com.johnymuffin.jban.beta.JohnyBans;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import com.projectposeidon.api.PoseidonUUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

import static com.johnymuffin.jban.core.JsonReader.readJsonFromUrl;
import static com.johnymuffin.jban.core.Util.encode;
import static com.johnymuffin.jban.core.Util.isPlayerAuthorized;
import static com.johnymuffin.jban.core.Util.validUUID;
import static com.johnymuffin.jban.core.Util.verifyJSONArguments;

public class JLookupCommand extends JCommand {
    private static final ChatColor HEADER = ChatColor.GOLD;
    private static final ChatColor LABEL = ChatColor.GRAY;
    private static final ChatColor VALUE = ChatColor.YELLOW;
    private static final ChatColor MUTED = ChatColor.DARK_GRAY;

    public JLookupCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
        if (!isPlayerAuthorized(cs, "johnybans.lookup")) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            sendUsage(cs);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if ("ip".equals(subCommand)) {
            if (!canViewIpData(cs)) {
                cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
                return true;
            }
            lookupIp(cs, args[1]);
            return true;
        }

        UUID uuid = resolveUuid(args[1]);
        if (uuid == null) {
            cs.sendMessage(ChatColor.RED + "Unable to resolve player: " + args[1]);
            return true;
        }

        boolean allScope = args.length >= 3 && "all".equalsIgnoreCase(args[2]);
        if (("ips".equals(subCommand) || ("alts".equals(subCommand) && allScope)) && !canViewIpData(cs)) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }

        if ("player".equals(subCommand)) {
            lookupPlayer(cs, uuid, false, "player");
        } else if ("alts".equals(subCommand)) {
            lookupPlayer(cs, uuid, allScope, "alts");
        } else if ("ips".equals(subCommand)) {
            lookupPlayer(cs, uuid, allScope, "ips");
        } else if ("history".equals(subCommand)) {
            lookupHistory(cs, uuid, allScope);
        } else {
            sendUsage(cs);
        }
        return true;
    }

    private void lookupPlayer(CommandSender cs, UUID uuid, boolean allScope, String mode) {
        final String url = plugin.getGetServerURL()
                + "/api/v1/getPlayerAssociations?uuid=" + encode(uuid.toString())
                + "&server=" + encode(plugin.getServerName())
                + "&scope=" + encode(allScope ? "all" : "server")
                + "&key=" + encode(plugin.getServerKey());

        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                final JsonObject jsonObject = readJsonFromUrl(url);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> handlePlayerResponse(cs, jsonObject, mode));
            } catch (Exception e) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    cs.sendMessage(ChatColor.RED + "Unable to contact the LMBD webserver.");
                    plugin.getLog().log(Level.WARNING, "Unable to perform player lookup", e);
                });
            }
        });
    }

    private void lookupHistory(CommandSender cs, UUID uuid, boolean allScope) {
        StringBuilder url = new StringBuilder(plugin.getGetServerURL())
                .append("/api/v1/getEnforcementHistory?uuid=").append(encode(uuid.toString()));
        if (allScope) {
            url.append("&includePast=true");
        } else {
            url.append("&server=").append(encode(plugin.getServerName()));
        }

        final String historyUrl = url.toString();
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                final JsonObject jsonObject = readJsonFromUrl(historyUrl);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> handleHistoryResponse(cs, jsonObject));
            } catch (Exception e) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    cs.sendMessage(ChatColor.RED + "Unable to contact the LMBD webserver.");
                    plugin.getLog().log(Level.WARNING, "Unable to perform enforcement history lookup", e);
                });
            }
        });
    }

    private void lookupIp(CommandSender cs, String ip) {
        final String url = plugin.getGetServerURL()
                + "/api/v1/getIPAssociations?ip=" + encode(ip)
                + "&key=" + encode(plugin.getServerKey());

        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                final JsonObject jsonObject = readJsonFromUrl(url);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> handleIpResponse(cs, jsonObject));
            } catch (Exception e) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    cs.sendMessage(ChatColor.RED + "Unable to contact the LMBD webserver.");
                    plugin.getLog().log(Level.WARNING, "Unable to perform IP lookup", e);
                });
            }
        });
    }

    private void handlePlayerResponse(CommandSender cs, JsonObject jsonObject, String mode) {
        if (!isValidResponse(cs, jsonObject)) {
            return;
        }

        String username = JsonUtil.getString(jsonObject, "username");
        String uuid = JsonUtil.getString(jsonObject, "uuid");
        String scope = JsonUtil.getString(jsonObject, "scope");
        JsonArray alternatives = JsonUtil.getArray(jsonObject, "alternativeAccounts");
        JsonArray ipHistory = JsonUtil.getArray(jsonObject, "ipHistory");

        if ("player".equals(mode)) {
            sendHeader(cs, "LMBD Player Lookup", username);
            sendField(cs, "UUID", uuid);
            sendField(cs, "Active bans", JsonUtil.getInt(jsonObject, "activeBanCount", 0));
            sendField(cs, "Active mutes", JsonUtil.getInt(jsonObject, "activeMuteCount", 0));
            sendField(cs, "Active warnings", JsonUtil.getInt(jsonObject, "activeWarningCount", 0));
            sendField(cs, "Alt accounts (" + scope + ")", alternatives.size());
            sendField(cs, "Known IPs (" + scope + ")", ipHistory.size());
            return;
        }

        if ("alts".equals(mode)) {
            sendHeader(cs, "Alt Accounts", username + " (" + scope + ")");
            sendAltRows(cs, alternatives);
            return;
        }

        sendHeader(cs, "IP History", username + " (" + scope + ")");
        sendIpRows(cs, ipHistory);
    }

    private void handleIpResponse(CommandSender cs, JsonObject jsonObject) {
        if (!isValidResponse(cs, jsonObject)) {
            return;
        }

        JsonArray accounts = JsonUtil.getArray(jsonObject, "accounts");
        JsonArray activeBanIDs = JsonUtil.getArray(jsonObject, "activeBanIDs");
        sendHeader(cs, "IP Lookup", JsonUtil.getString(jsonObject, "ip"));
        sendField(cs, "Associated accounts", accounts.size());
        sendField(cs, "Active ban IDs", joinArray(activeBanIDs));
        sendIpAccountRows(cs, accounts);
    }

    private void handleHistoryResponse(CommandSender cs, JsonObject jsonObject) {
        if (!isValidResponse(cs, jsonObject)) {
            return;
        }

        JsonArray enforcements = JsonUtil.getArray(jsonObject, "enforcements");
        boolean includePast = JsonUtil.getBoolean(jsonObject, "includePast", false);
        String server = JsonUtil.getString(jsonObject, "server");
        String scope = server == null ? "all servers" : server;
        sendHeader(cs, "Enforcement History", scope + (includePast ? " including past" : " active/open"));
        if (enforcements.isEmpty()) {
            sendNone(cs);
            return;
        }

        for (int i = 0; i < enforcements.size(); i++) {
            JsonObject enforcement = enforcements.get(i).getAsJsonObject();
            sendHistoryRow(cs, enforcement);
        }
    }

    private void sendAltRows(CommandSender cs, JsonArray accounts) {
        if (accounts.isEmpty()) {
            sendNone(cs);
            return;
        }

        for (int i = 0; i < accounts.size(); i++) {
            JsonObject account = accounts.get(i).getAsJsonObject();
            cs.sendMessage(LABEL + "- " + VALUE + JsonUtil.getString(account, "name")
                    + MUTED + " | " + LABEL + "bans " + VALUE + JsonUtil.getInt(account, "activeBanCount", 0)
                    + MUTED + " | " + LABEL + "mutes " + VALUE + JsonUtil.getInt(account, "activeMuteCount", 0)
                    + MUTED + " | " + LABEL + "warnings " + VALUE + JsonUtil.getInt(account, "activeWarningCount", 0));
        }
    }

    private void sendIpAccountRows(CommandSender cs, JsonArray accounts) {
        if (accounts.isEmpty()) {
            sendNone(cs);
            return;
        }

        for (int i = 0; i < accounts.size(); i++) {
            JsonObject account = accounts.get(i).getAsJsonObject();
            cs.sendMessage(LABEL + "- " + VALUE + JsonUtil.getString(account, "name")
                    + MUTED + " | " + LABEL + "servers " + VALUE + joinArray(JsonUtil.getArray(account, "servers"))
                    + MUTED + " | " + LABEL + "active bans " + VALUE + JsonUtil.getInt(account, "activeBanCount", 0));
        }
    }

    private void sendIpRows(CommandSender cs, JsonArray ipHistory) {
        if (ipHistory.isEmpty()) {
            sendNone(cs);
            return;
        }

        for (int i = 0; i < ipHistory.size(); i++) {
            JsonObject row = ipHistory.get(i).getAsJsonObject();
            cs.sendMessage(LABEL + "- " + VALUE + JsonUtil.getString(row, "ip")
                    + MUTED + " | " + LABEL + "servers " + VALUE + joinArray(JsonUtil.getArray(row, "servers")));
        }
    }

    private void sendHistoryRow(CommandSender cs, JsonObject enforcement) {
        String type = JsonUtil.getString(enforcement, "type");
        String id = getEnforcementId(enforcement, type);
        String serverName = JsonUtil.getString(enforcement, "serverName");
        String reason = JsonUtil.getString(enforcement, "reason");
        String issued = formatEpochSeconds(JsonUtil.getString(enforcement, "timeIssued"));
        String expiry = "";
        if (JsonUtil.has(enforcement, "expiry")) {
            expiry = MUTED + " | " + LABEL + "expiry " + VALUE + formatEpochSeconds(JsonUtil.getString(enforcement, "expiry"));
        }
        cs.sendMessage(LABEL + "- " + VALUE + type
                + MUTED + " | " + LABEL + "id " + VALUE + id
                + MUTED + " | " + LABEL + "server " + VALUE + serverName
                + MUTED + " | " + LABEL + "issued " + VALUE + issued
                + expiry
                + MUTED + " | " + LABEL + "reason " + VALUE + reason);
    }

    private void sendHeader(CommandSender cs, String title, String value) {
        cs.sendMessage(HEADER + "=== " + title + ": " + VALUE + value + HEADER + " ===");
    }

    private void sendField(CommandSender cs, String label, Object value) {
        cs.sendMessage(LABEL + label + ": " + VALUE + value);
    }

    private void sendNone(CommandSender cs) {
        cs.sendMessage(MUTED + "None found.");
    }

    private boolean isValidResponse(CommandSender cs, JsonObject jsonObject) {
        if (jsonObject == null || !verifyJSONArguments(jsonObject, "error")) {
            cs.sendMessage(ChatColor.RED + "Invalid response from LMBD server.");
            return false;
        }
        if (JsonUtil.getBoolean(jsonObject, "error", false)) {
            cs.sendMessage(ChatColor.RED + "LMBD server error: " + JsonUtil.getString(jsonObject, "error_message", "Unknown error"));
            return false;
        }
        return true;
    }

    private String joinArray(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(array.get(i).getAsString());
        }
        return builder.toString();
    }

    private String getEnforcementId(JsonObject enforcement, String type) {
        if ("BAN".equalsIgnoreCase(type)) {
            return JsonUtil.getString(enforcement, "banID");
        }
        if ("MUTE".equalsIgnoreCase(type)) {
            return JsonUtil.getString(enforcement, "muteID");
        }
        if ("WARNING".equalsIgnoreCase(type)) {
            return JsonUtil.getString(enforcement, "warningID");
        }
        return "unknown";
    }

    private String formatEpochSeconds(String rawTimestamp) {
        if (rawTimestamp == null) {
            return "unknown";
        }
        try {
            long timestamp = Long.parseLong(rawTimestamp);
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(timestamp * 1000L));
        } catch (NumberFormatException e) {
            return rawTimestamp;
        }
    }

    private UUID resolveUuid(String input) {
        if (validUUID(input)) {
            return UUID.fromString(input);
        }
        UUID uuid = PoseidonUUID.getPlayerUUIDFromCache(input, true);
        if (uuid == null) {
            uuid = PoseidonUUID.getPlayerUUIDFromCache(input, false);
        }
        return uuid;
    }

    private boolean canViewIpData(CommandSender cs) {
        return !(cs instanceof org.bukkit.entity.Player) || isPlayerAuthorized(cs, "johnybans.lookup.ip");
    }

    private void sendUsage(CommandSender cs) {
        cs.sendMessage(ChatColor.RED + "Usage:");
        cs.sendMessage(ChatColor.RED + "/jlookup player <username|uuid>");
        cs.sendMessage(ChatColor.RED + "/jlookup alts <username|uuid> [all]");
        cs.sendMessage(ChatColor.RED + "/jlookup ips <username|uuid> [all]");
        cs.sendMessage(ChatColor.RED + "/jlookup ip <ip>");
        cs.sendMessage(ChatColor.RED + "/jlookup history <username|uuid> [all]");
    }
}
