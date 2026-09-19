package com.johnymuffin.jban.beta;

import com.johnymuffin.jban.beta.commands.JBCodeCommand;
import com.johnymuffin.jban.beta.commands.JBanCommand;
import com.johnymuffin.jban.beta.commands.JLookupCommand;
import com.johnymuffin.jban.beta.commands.JMuteCommand;
import com.johnymuffin.jban.beta.commands.JUnbanCommand;
import com.johnymuffin.jban.beta.commands.JUnmuteCommand;
import com.johnymuffin.jban.beta.commands.JWarnAckCommand;
import com.johnymuffin.jban.beta.commands.JWarnCommand;
import com.johnymuffin.jban.beta.exceptions.JBCommand;
import com.johnymuffin.jban.beta.exceptions.JBExceptions;
import com.johnymuffin.jban.core.Mute;
import com.johnymuffin.jban.core.Warning;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.johnymuffin.jban.core.JsonReader.readJsonFromUrl;

public class JohnyBans extends JavaPlugin {
    private static final String DEFAULT_SERVER_NAME = "TestServer";
    private static final String DEFAULT_SERVER_KEY = "123pass";
    //Basic Plugin Info
    private static JohnyBans plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;
    //Plugin Specific
    private String getServerURL;
    private String serverName;
    private JBConfig jbConfig;
    private String serverKey;

    public JBExceptions getJbExceptions() {
        return jbExceptions;
    }

    private JBExceptions jbExceptions;
    private final Map<UUID, Mute[]> activeMutes = new ConcurrentHashMap<>();
    private final Map<UUID, Warning[]> activeWarnings = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] Is Loading, Version: " + pdf.getVersion());

        JBLang.getInstance(plugin);
        jbConfig = new JBConfig(new File(plugin.getDataFolder(), "config.yml"));
        jbConfig.reload();
        getServerURL = jbConfig.getConfigString("serverURL.value");
        serverName = jbConfig.getConfigString("serverName.value");
        serverKey = jbConfig.getConfigString("serverKey.value");
        if (isPlaceholderServerConfig()) {
            logger(Level.SEVERE, "LMBD is using the default serverName or serverKey. Update serverName.value and serverKey.value in config.yml before enabling this plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        jbExceptions = new JBExceptions(new File(plugin.getDataFolder(), "exceptions.json"));


        JBListener listener = new JBListener(plugin);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        this.getCommand("jban").setExecutor(new JBanCommand(plugin));
        this.getCommand("junban").setExecutor(new JUnbanCommand(plugin));
        this.getCommand("jmute").setExecutor(new JMuteCommand(plugin));
        this.getCommand("junmute").setExecutor(new JUnmuteCommand(plugin));
        this.getCommand("jwarn").setExecutor(new JWarnCommand(plugin));
        this.getCommand("warnack").setExecutor(new JWarnAckCommand(plugin));
        this.getCommand("jbcode").setExecutor(new JBCodeCommand(plugin));
        this.getCommand("jlookup").setExecutor(new JLookupCommand(plugin));

        this.getCommand("mbexceptions").setExecutor(new JBCommand(plugin));
    }

    @Override
    public void onDisable() {
    }

    public String getGetServerURL() {
        return getServerURL;
    }

    private boolean isPlaceholderServerConfig() {
        return DEFAULT_SERVER_NAME.equals(serverName) || DEFAULT_SERVER_KEY.equals(serverKey);
    }

    public String getServerName() {
        return serverName;
    }

    public void logger(Level level, String message) {
        Bukkit.getLogger().log(level, "[" + pluginName + "] " + message);
    }

    public void logDebug(Level level, String message) {
        if (jbConfig.getConfigBoolean("settings.debug.enabled.value")) {
            logger(level, "[" + pluginName + "] [DEBUG] " + message);
        }
    }

    public Logger getLog() {
        return log;
    }

    public JBConfig getJbConfig() {
        return jbConfig;
    }

    public String getServerKey() {
        return serverKey;
    }

    public String getEvidenceToken(UUID adminUUID) {
        if (adminUUID == null) {
            return null;
        }

        String tokenURL = getGetServerURL()
                + "/api/v1/getEvidenceToken?adminUUID="
                + encode(adminUUID.toString())
                + "&key="
                + encode(getServerKey());

        try {
            JsonObject jsonObject = readJsonFromUrl(tokenURL);
            if (jsonObject == null) {
                return null;
            }

            if (JsonUtil.getBoolean(jsonObject, "error", false)) {
                logger(Level.WARNING, "Error returned while requesting evidence token: " + jsonObject.toString());
                return null;
            }

            return JsonUtil.getString(jsonObject, "token");
        } catch (Exception e) {
            getLog().log(Level.WARNING, "Error getting evidence token for UUID " + adminUUID, e);
            return null;
        }
    }

    public void refreshMuteStatus(UUID uuid) {
        final String muteURL = getGetServerURL() + "/api/v1/isUUIDMuted?uuid=" + encode(uuid.toString()) + "&info=true";
        Bukkit.getScheduler().scheduleAsyncDelayedTask(this, () -> {
            try {
                JsonObject jsonObject = readJsonFromUrl(muteURL);
                if (jsonObject == null || JsonUtil.getBoolean(jsonObject, "error", false)) {
                    return;
                }

                if (!JsonUtil.has(jsonObject, "activeMutes")) {
                    activeMutes.remove(uuid);
                    return;
                }

                JsonArray activeMutesJson = JsonUtil.getArray(jsonObject, "activeMutes");
                Mute[] parsed = new Mute[activeMutesJson.size()];
                for (int i = 0; i < activeMutesJson.size(); i++) {
                    parsed[i] = new Mute(activeMutesJson.get(i).getAsJsonObject());
                }
                activeMutes.put(uuid, parsed);
            } catch (Exception e) {
                getLog().log(Level.WARNING, "Unable to refresh mute status for " + uuid, e);
            }
        });
    }

    public void refreshWarningStatus(UUID uuid) {
        final String warningURL = getGetServerURL() + "/api/v1/getWarningHistory?uuid=" + encode(uuid.toString());
        Bukkit.getScheduler().scheduleAsyncDelayedTask(this, () -> {
            try {
                JsonObject jsonObject = readJsonFromUrl(warningURL);
                if (jsonObject == null || JsonUtil.getBoolean(jsonObject, "error", false)) {
                    return;
                }

                if (!JsonUtil.has(jsonObject, "warnings")) {
                    activeWarnings.remove(uuid);
                    return;
                }

                Warning[] parsed = parseActiveWarningsForServer(JsonUtil.getArray(jsonObject, "warnings"));
                if (parsed.length == 0) {
                    activeWarnings.remove(uuid);
                    return;
                }

                activeWarnings.put(uuid, parsed);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> sendPendingWarnings(uuid));
            } catch (Exception e) {
                getLog().log(Level.WARNING, "Unable to refresh warning status for " + uuid, e);
            }
        });
    }

    private Warning[] parseActiveWarningsForServer(JsonArray warningsJson) {
        Warning[] parsed = new Warning[warningsJson.size()];
        int index = 0;
        for (int i = 0; i < warningsJson.size(); i++) {
            Warning warning = new Warning(warningsJson.get(i).getAsJsonObject());
            if (warning.isResolved() || warning.isAcknowledged() || !warning.getServerName().equalsIgnoreCase(getServerName())) {
                continue;
            }
            parsed[index++] = warning;
        }
        if (index == parsed.length) {
            return parsed;
        }
        Warning[] trimmed = new Warning[index];
        System.arraycopy(parsed, 0, trimmed, 0, index);
        return trimmed;
    }

    public Mute[] getCachedMutes(UUID uuid) {
        Mute[] mutes = activeMutes.get(uuid);
        if (mutes == null) {
            return new Mute[0];
        }
        return mutes;
    }

    public void clearCachedMutes(UUID uuid) {
        activeMutes.remove(uuid);
    }

    public Warning[] getCachedWarnings(UUID uuid) {
        Warning[] warnings = activeWarnings.get(uuid);
        if (warnings == null) {
            return new Warning[0];
        }
        return warnings;
    }

    public void clearCachedWarnings(UUID uuid) {
        activeWarnings.remove(uuid);
    }

    public void sendPendingWarnings(UUID uuid) {
        if (uuid == null) {
            return;
        }

        Warning[] warnings = getCachedWarnings(uuid);
        if (warnings.length == 0) {
            return;
        }

        Player player = null;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(uuid)) {
                player = onlinePlayer;
                break;
            }
        }
        if (player == null || !player.isOnline()) {
            return;
        }

        String baseUrl = jbConfig.getConfigString("settings.user-ban-url.value");
        player.sendMessage(ChatColor.YELLOW + "You have " + warnings.length + " warning(s) that require acknowledgement:");
        for (Warning warning : warnings) {
            player.sendMessage(ChatColor.GOLD + "- " + ChatColor.YELLOW + warning.getReason());
            player.sendMessage(ChatColor.GRAY + "  " + baseUrl + "/w/" + warning.getWarningID() + "E");
        }
        player.sendMessage(ChatColor.RED + "Type /warnack to acknowledge these warnings.");
    }

    private String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            getLog().log(Level.WARNING, "Invalid encoding", e);
            return "";
        }
    }

}
