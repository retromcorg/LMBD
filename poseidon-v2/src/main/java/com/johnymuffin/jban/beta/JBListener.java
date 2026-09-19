package com.johnymuffin.jban.beta;

import com.johnymuffin.jban.core.Ban;
import com.johnymuffin.jban.core.Mute;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.johnymuffin.jban.core.JsonUtil;
import com.legacyminecraft.poseidon.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.retromc.retrobridge.api.event.PlayerAuthenticatedEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.logging.Level;

import static com.johnymuffin.jban.core.JsonReader.readJsonFromUrl;
import static com.johnymuffin.jban.core.Util.postToURL;
import static com.johnymuffin.jban.core.Util.verifyJSONArguments;
import static com.johnymuffin.jban.beta.JBEnforcementPolicy.hasRequiredEvidence;
import static com.johnymuffin.jban.beta.JBEnforcementPolicy.isBanServerIgnored;
import static com.johnymuffin.jban.beta.JBEnforcementPolicy.isMuteEffective;

public class JBListener implements Listener {
    private JohnyBans plugin;

    public JBListener(JohnyBans plugin) {
        this.plugin = plugin;
    }

    //V2 removed addConnectionPause/cancelPlayerLogin, and the login pipeline is already async, so the
    //lookups run inline on this login thread instead of pausing the connection. They run in sequence,
    //so an effective UUID ban skips the IP check; each is bounded by JsonReader's socket timeouts.
    //ignoreCancelled has no effect here as the event is not Cancellable, hence the explicit result check.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLogin(final AsyncPlayerPreLoginEvent event) {
        if (event.getResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID uuid = event.getUniqueId();


        if (plugin.getJbExceptions().isUserAnException(uuid)) {
            this.plugin.logger(Level.INFO, event.getName() + " Has an exception for LMBD and will be allowed to join.");
            return;
        }

        //Check if UUID is banned
        String uuidURL = plugin.getGetServerURL() + "/api/v1/isUUIDBanned?uuid=" + uuid.toString() + "&info=true";
        if (checkBan(event, uuid, uuidURL, "UUID")) {
            return;
        }

        //Check if IP is banned
        String ipURL = plugin.getGetServerURL() + "/api/v1/isIPBanned?ip=" + event.getAddress().getHostAddress() + "&info=true";
        checkBan(event, uuid, ipURL, "IP");
    }

    /**
     * Runs one ban lookup and disallows the login if an effective ban comes back. A lookup that fails
     * lets the player through, as it did in V1.
     *
     * @return true when the login was disallowed.
     */
    private boolean checkBan(AsyncPlayerPreLoginEvent event, UUID uuid, String url, String banType) {
        final JsonObject jsonObject;
        try {
            jsonObject = readJsonFromUrl(url);
        } catch (Exception e) {
            plugin.getLog().log(Level.WARNING, "Invalid response, or connection error when checking for " + banType + " ban", e);
            return false;
        }
        return processResponse(event, uuid, plugin, jsonObject, banType);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerAuthenticated(PlayerAuthenticatedEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getAddress() == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();
        String username = player.getName();
        savePlayerIP(uuid, username, ip);
        this.plugin.logDebug(Level.INFO, "IP for " + username + " sent to API after verification with RetroBridge auth provider " + event.getProviderName());
    }

    public void savePlayerIP(UUID uuid, String username, String userIP) {
        JsonObject joinData = new JsonObject();
        final String url = plugin.getGetServerURL() + "/api/v1/userJoined";
        joinData.addProperty("username", username);
        joinData.addProperty("uuid", uuid.toString());
        joinData.addProperty("userIP", userIP);
        joinData.addProperty("key", plugin.getServerKey());


        Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            try {
                postToURL(joinData.toString(), "application/json", url);
                plugin.refreshMuteStatus(uuid);
                plugin.refreshWarningStatus(uuid);
            } catch (Exception exception) {
                plugin.logger(Level.WARNING, "Failed to send join information to the Ban server for " + username);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Mute[] cachedMutes = plugin.getCachedMutes(event.getPlayer().getUniqueId());
        if (cachedMutes.length == 0) {
            return;
        }

        for (Mute mute : cachedMutes) {
            if (isMuteEffective(plugin, mute)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You are muted. Reason: " + mute.getReason());
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!isMuted(event.getPlayer())) {
            return;
        }
        if (!isMutedCommandBlocked(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "You are muted and cannot use that command.");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (isMuted(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You are muted and cannot place signs.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isSignMaterial(event.getBlockPlaced().getType())) {
            return;
        }
        if (isMuted(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You are muted and cannot place signs.");
        }
    }

    private boolean isMuted(Player player) {
        if (player == null) {
            return false;
        }
        Mute[] cachedMutes = plugin.getCachedMutes(player.getUniqueId());
        if (cachedMutes.length == 0) {
            return false;
        }

        for (Mute mute : cachedMutes) {
            if (isMuteEffective(plugin, mute)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMutedCommandBlocked(String message) {
        if (message == null) {
            return false;
        }

        String commandLabel = normalizeCommand(message.trim().split(" ", 2)[0]);
        if (commandLabel.isEmpty()) {
            return false;
        }

        Object blockedCommands = plugin.getJbConfig().getConfigOption("mute.blocked-commands.value", null);
        if (!(blockedCommands instanceof Iterable)) {
            return false;
        }

        for (Object blockedCommand : (Iterable<?>) blockedCommands) {
            if (commandLabel.equals(normalizeCommand(blockedCommand))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeCommand(Object command) {
        if (command == null) {
            return "";
        }
        return String.valueOf(command).trim().toLowerCase().replaceFirst("^/", "");
    }

    private boolean isSignMaterial(Material material) {
        return material == Material.SIGN || material == Material.SIGN_POST || material == Material.WALL_SIGN;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.clearCachedMutes(event.getPlayer().getUniqueId());
        plugin.clearCachedWarnings(event.getPlayer().getUniqueId());
    }

    public String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    public boolean processResponse(AsyncPlayerPreLoginEvent event, UUID uuid, JohnyBans plugin, JsonObject jsonObject, String banType) {
        if (!verifyJSONArguments(jsonObject, "isKnown", "isBanned", "error")) {
            plugin.getLog().log(Level.WARNING, "Invalid JSON Response when " + event.getName() + " attempted to connect: " + jsonObject.toString());
            return false;
        }

        //First character of banType is always uppercase
        String statisticsCode = "" + banType.toUpperCase().charAt(0);

        //If user is banned currently
        if (JsonUtil.getBoolean(jsonObject, "isBanned", false)) {
            //Load Ban Objects
            JsonArray banArray = JsonUtil.getArray(jsonObject, "activeBans");
            Ban[] bans = new Ban[banArray.size()];
            for (int i = 0; i < banArray.size(); i++) {
                bans[i] = new Ban(banArray.get(i).getAsJsonObject());
            }

            String thisServerName = plugin.getServerName();
            //Order the ban array so bans from this server are first
            int index = 0;
            for (int i = 0; i < bans.length; i++) {
                if (bans[i].getServerName().equalsIgnoreCase(thisServerName)) {
                    Ban temp = bans[index];
                    bans[index] = bans[i];
                    bans[i] = temp;
                    index++;
                }
            }


            //Loop through the bans and check if any of them are valid
            for (Ban ban : bans) {
                boolean evidenceBypass = !hasRequiredEvidence(plugin, ban.getServerName(), ban.getEvidences().size());

                boolean serverIgnoreBypass = isBanServerIgnored(plugin, ban.getServerName());

                String lmbdURL = plugin.getJbConfig().getConfigString("settings.user-ban-url.value");


                if (!evidenceBypass && !serverIgnoreBypass) {
                    plugin.logger(Level.INFO, event.getName() + " has been blocked from joining as they have a UUID ban with a ID of " + ban.getBanID());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.RED + "You are banned. Visit: " + lmbdURL + "/b/" + ban.getBanID() + statisticsCode);
                    return true;
                } else {
                    if (evidenceBypass) {
                        plugin.logger(Level.INFO, event.getName() + " is banned on the server " + ban.getServerName() + " with the BanID " + ban.getBanID() + ", however, the required evidence for the ban to be valid doesn't exist according to the plugin's config.");
                    } else {
                        plugin.logger(Level.INFO, event.getName() + " is banned on the server " + ban.getServerName() + " with the BanID " + ban.getBanID() + ", however, the server the ban originates from has been ignored in the plugin's config.");

                    }
                }
            }
        }
        plugin.logger(Level.INFO, event.getName() + " has has passed " + banType + " ban checks as no effective bans were found.");
        return false;
    }

}
