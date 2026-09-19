package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JBLang;
import com.johnymuffin.jban.beta.JohnyBans;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

import static com.johnymuffin.jban.core.JsonReader.readJsonFromUrl;
import static com.johnymuffin.jban.core.Util.*;

public class JBCodeCommand extends JCommand {
    private String admin;

    public JBCodeCommand(JohnyBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String s, String[] strings) {
        this.admin = ((cs instanceof Player) ? ((Player) cs).getName() : "Console");
        if (!isPlayerAuthorized(cs, "johnybans.jbcode")) {
            cs.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }
        if (!(cs instanceof Player)) {
            cs.sendMessage(JBLang.getInstance().getMessage("unavailable_to_console"));
            return true;
        }
        Player player = (Player) cs;
        String authURL = plugin.getGetServerURL() + "/api/v1/getUserAuthCode?adminUUID=" + encode(player.getUniqueId().toString()) + "&key=" + encode(plugin.getServerKey());
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                final JsonObject jsonObject = readJsonFromUrl(authURL);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    if (!verifyJSONArguments(jsonObject, "error")) {
                        plugin.getLog().log(Level.WARNING, "Invalid JSON Response " + jsonObject.toString());
                        cs.sendMessage(ChatColor.RED + "Invalid response from LMBD server.");
                        return;
                    }
                    if (JsonUtil.getBoolean(jsonObject, "error", false)) {
                        cs.sendMessage(ChatColor.RED + "Error returned from LMBD server");
                        plugin.logger(Level.INFO, "Error returned on authentication code: " + jsonObject.toString());
                        return;
                    }
                    cs.sendMessage(ChatColor.GRAY + "Authentication Code: " + ChatColor.DARK_RED + JsonUtil.getString(jsonObject, "code"));
                });
            } catch (Exception e) {
                plugin.getLog().log(Level.WARNING, "Invalid response, or connection error when checking for IP ban", e);
            }
        }, 10L);
        return true;
    }
}
