package com.johnymuffin.jban.beta.exceptions;

import com.johnymuffin.jban.beta.JBLang;
import com.johnymuffin.jban.beta.JohnyBans;
import com.projectposeidon.api.PoseidonUUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

import static com.johnymuffin.jban.core.Util.isPlayerAuthorized;

public class JBCommand implements CommandExecutor {
    private JohnyBans plugin;

    public JBCommand(JohnyBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!isPlayerAuthorized(commandSender, "johnybans.exceptions")) {
            commandSender.sendMessage(JBLang.getInstance().getMessage("no_permission"));
            return true;
        }

        if (strings.length == 0 || !(strings[0].equalsIgnoreCase("list") || strings[0].equalsIgnoreCase("add") || strings[0].equalsIgnoreCase("remove"))) {
            commandSender.sendMessage("/mbexceptions (list/add/remove)");
            return true;
        }
        String subcommand = strings[0];

        if (strings[0].equalsIgnoreCase("list")) {
            String exceptions = "Exceptions: ";
            for (Object object : plugin.getJbExceptions().getExceptionList()) {
                UUID uuid = UUID.fromString(String.valueOf(object));
                if (PoseidonUUID.getPlayerUsernameFromUUID(uuid) == null) {
                    exceptions = exceptions + "unknown user, ";
                } else {
                    exceptions = exceptions + PoseidonUUID.getPlayerUsernameFromUUID(uuid);
                }
            }
            commandSender.sendMessage(exceptions);
            return true;
        } else if (strings[0].equalsIgnoreCase("add")) {
            if (strings.length != 2) {
                commandSender.sendMessage("/mbexceptions (list/add/remove)");
                return true;
            }
            String username = strings[1];
            UUID uuid = PoseidonUUID.getPlayerMojangUUID(username);
            if (uuid == null) uuid = PoseidonUUID.getPlayerOfflineUUID(username);
            if (uuid == null) {
                commandSender.sendMessage(ChatColor.RED + "That player couldn't be found.");
                return true;
            }
            plugin.getJbExceptions().addUserToExceptions(uuid);
            commandSender.sendMessage(ChatColor.RED + "Player has been added to the exception list.");
            return true;
        } else if (strings[0].equalsIgnoreCase("remove")) {
            if (strings.length != 2) {
                commandSender.sendMessage("/mbexceptions (list/add/remove)");
                return true;
            }
            String username = strings[1];
            UUID uuid = PoseidonUUID.getPlayerMojangUUID(username);
            if (uuid == null) uuid = PoseidonUUID.getPlayerOfflineUUID(username);
            if (uuid == null) {
                commandSender.sendMessage(ChatColor.RED + "That player couldn't be found.");
                return true;
            }

            if (plugin.getJbExceptions().removeUserFromExceptions(uuid)) {
                commandSender.sendMessage(ChatColor.RED + "Player has been removed from exception list");
            } else {
                commandSender.sendMessage(ChatColor.RED + "Player couldn't be removed from exception list");
            }
            return true;
        }


        return false;
    }
}
