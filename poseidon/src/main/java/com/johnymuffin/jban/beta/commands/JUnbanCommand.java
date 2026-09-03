package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JBLang;
import com.johnymuffin.jban.beta.JohnyBans;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import com.projectposeidon.api.PoseidonUUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

import static com.johnymuffin.jban.core.Util.*;

public class JUnbanCommand extends JCommand {
    private String admin;

    public JUnbanCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
        if (!isPlayerAuthorized(cs, "johnybans.unban")) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }
        this.admin = ((cs instanceof Player) ? ((Player) cs).getName() : "Console");
        if (args.length != 1) {
            cs.sendMessage(ChatColor.RED + "Invalid Usage, /junban (username)");
            return true;
        }

        String playerName = args[0];
        Player player = null;
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(playerName)) {
                player = p;
                playerName = p.getName();
            }
        }

        UUID uuid = PoseidonUUID.getPlayerUUIDFromCache(playerName, true);
        if (uuid == null) uuid = PoseidonUUID.getPlayerUUIDFromCache(playerName, false);
        if (uuid == null) {
            String msg = JBLang.getInstance().getMessage("player_not_found_full");
            msg = msg.replace("%username%", playerName);
            cs.sendMessage(msg);
            return true;
        }

        final String url = plugin.getGetServerURL() + "/api/v1/unBanUUID";
        JsonObject postData = new JsonObject();
        postData.addProperty("username", playerName);
        postData.addProperty("uuid", uuid.toString());
        postData.addProperty("adminUsername", admin);
        postData.addProperty("key", plugin.getServerKey());
        if (cs instanceof Player) {
            postData.addProperty("adminUUID", ((Player) cs).getUniqueId().toString());
        } else {
            postData.addProperty("adminUUID", UUID.nameUUIDFromBytes(admin.getBytes()).toString());
        }
        postData.addProperty("server", plugin.getServerName());
        String finalPlayerName = playerName;

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
                        if (!JsonUtil.getBoolean(jsonResponse, "error", false)) {
                            if (!JsonUtil.getBoolean(jsonResponse, "successful", false)) {
                                if (JsonUtil.has(jsonResponse, "error_message")) {
                                    cs.sendMessage(ChatColor.RED + "Server returned error: " + JsonUtil.getString(jsonResponse, "error_message"));
                                } else {
                                    cs.sendMessage(ChatColor.RED + "A unknown server error occurred.");
                                }
                                return;
                            }
                            cs.sendMessage(finalPlayerName + " Has been unbanned successfully");
                        } else {
                            cs.sendMessage("An error occurred processing that command");
                            plugin.getLog().log(Level.WARNING, "Error on unban, " + jsonResponse.toString());
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
}
