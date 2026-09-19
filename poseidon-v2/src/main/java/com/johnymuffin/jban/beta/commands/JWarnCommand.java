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

import java.util.UUID;
import java.util.logging.Level;

import static com.johnymuffin.jban.core.Util.postToURL;
import static com.johnymuffin.jban.core.Util.isPlayerAuthorized;
import static com.johnymuffin.jban.core.Util.verifyJSONArguments;

public class JWarnCommand extends JCommand {
    public JWarnCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
        if (!isPlayerAuthorized(cs, "johnybans.warn")) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }
        if (args.length < 2) {
            cs.sendMessage(ChatColor.RED + "Invalid Usage, /jwarn (username) (reason)");
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
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(' ');
        }
        String reason = reasonBuilder.toString().trim();

        JsonObject postData = new JsonObject();
        postData.addProperty("username", playerName);
        postData.addProperty("uuid", uuid.toString());
        postData.addProperty("adminUsername", admin);
        postData.addProperty("key", plugin.getServerKey());
        postData.addProperty("adminUUID", (cs instanceof Player) ? ((Player) cs).getUniqueId().toString() : UUID.nameUUIDFromBytes(admin.getBytes()).toString());
        postData.addProperty("warningReason", reason);
        postData.addProperty("server", plugin.getServerName());

        final String url = plugin.getGetServerURL() + "/api/v1/warnUser";
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
                        cs.sendMessage(ChatColor.YELLOW + "Warning issued to " + playerName + ". Warning ID: " + JsonUtil.getString(jsonResponse, "warningID"));
                        plugin.refreshWarningStatus(finalUuid);
                    } catch (Exception e) {
                        cs.sendMessage("Malformed JSON received from LMBD server.");
                        plugin.getLog().log(Level.WARNING, "An error occurred processing warning", e);
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
}
