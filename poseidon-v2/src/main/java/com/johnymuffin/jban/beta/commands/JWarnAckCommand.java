package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JohnyBans;
import com.johnymuffin.jban.core.Warning;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

import static com.johnymuffin.jban.core.Util.postToURL;
import static com.johnymuffin.jban.core.Util.verifyJSONArguments;

public class JWarnAckCommand extends JCommand {
    public JWarnAckCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
        if (!(cs instanceof Player)) {
            cs.sendMessage(ChatColor.RED + "Only players can acknowledge warnings.");
            return true;
        }
        Player player = (Player) cs;

        Warning[] cachedWarnings = plugin.getCachedWarnings(player.getUniqueId());
        if (cachedWarnings.length == 0) {
            cs.sendMessage(ChatColor.GREEN + "You have no warnings to acknowledge.");
            return true;
        }

        JsonObject postData = new JsonObject();
        postData.addProperty("username", player.getName());
        postData.addProperty("uuid", player.getUniqueId().toString());
        postData.addProperty("key", plugin.getServerKey());

        final String url = plugin.getGetServerURL() + "/api/v1/ackWarnings";
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

                        plugin.clearCachedWarnings(player.getUniqueId());
                        int acknowledged = JsonUtil.getInt(jsonResponse, "acknowledgedWarnings", 0);
                        cs.sendMessage(ChatColor.GREEN + "Acknowledged " + acknowledged + " warning(s).");
                    } catch (Exception e) {
                        cs.sendMessage("Malformed JSON received from LMBD server.");
                        plugin.getLog().log(Level.WARNING, "An error occurred acknowledging warnings", e);
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
