package com.johnymuffin.jban.beta;

import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.Arrays;

public class JBConfig extends Configuration {
    private boolean isNew = true;
    private final int configVersion = 6;

    public JBConfig(File file) {
        super(file);
        this.isNew = !file.exists();
        reload();
    }

    public void reload() {
        this.load();
        this.write();
        this.save();
    }

    private void write() {
        generateConfigOption("config-version", configVersion);

        generateConfigOption("serverURL.value", "https://bans.johnymuffin.com");
        generateConfigOption("serverURL.info", "Base URL for the LMBD API server.");
        generateConfigOption("serverName.value", "TestServer");
        generateConfigOption("serverName.info", "Name of this Minecraft server as registered in LMBD.");
        generateConfigOption("serverKey.value", "123pass");
        generateConfigOption("serverKey.info", "Authentication key used by this server when calling write API endpoints.");
        generateConfigOption("require.evidence.info", "Describes if evidence needs to be attached to bans and mutes for the server to consider them valid.");
        generateConfigOption("require.evidence.self.value", false);
        generateConfigOption("require.evidence.self.info", "Require bans and mutes issued by this server to have evidence before they take effect locally.");
        generateConfigOption("require.evidence.others.value", true);
        generateConfigOption("require.evidence.others.info", "Require bans and mutes issued by other servers to have evidence before they take effect locally.");
        generateConfigOption("require.mutes.info", "Describes if mutes from this server and other servers should take effect locally.");
        generateConfigOption("require.mutes.self.value", true);
        generateConfigOption("require.mutes.self.info", "Apply mutes issued by this server locally.");
        generateConfigOption("require.mutes.others.value", true);
        generateConfigOption("require.mutes.others.info", "Apply mutes issued by other servers locally.");
        generateConfigOption("ignore-bans.enabled.value", false);
        generateConfigOption("ignore-bans.enabled.info", "Enable the server ignore list for bans from other servers.");
        generateConfigOption("ignore-bans.info", "Allows a server owner to configure LMBD to ignore the bans from other specified servers. LMBD doesn't allow multiple bans on the same user so this setting could make it difficult to ban a player on your server who is already banned on another that you are ignoring.");
        generateConfigOption("ignore-bans.servers.value", Arrays.asList("Test Ignored Server 1", "Test Ignored Server 2"));
        generateConfigOption("ignore-bans.servers.info", "Server names to ignore when checking bans.");
        generateConfigOption("ignore-mutes.enabled.value", false);
        generateConfigOption("ignore-mutes.enabled.info", "Enable the server ignore list for mutes from other servers.");
        generateConfigOption("ignore-mutes.info", "Allows a server owner to configure LMBD to ignore mutes from other specified servers.");
        generateConfigOption("ignore-mutes.servers.value", Arrays.asList("Test Ignored Server 1", "Test Ignored Server 2"));
        generateConfigOption("ignore-mutes.servers.info", "Server names to ignore when checking mutes.");
        generateConfigOption("mute.blocked-commands.value", Arrays.asList("msg", "tell", "r", "t", "mail", "reply", "m"));
        generateConfigOption("mute.blocked-commands.info", "Commands muted players cannot run. Values are command labels without a leading slash.");

        generateConfigOption("settings.user-ban-url.value", "https://bans.legacyminecraft.com");
        generateConfigOption("settings.user-ban-url.info", "The URL to the LMBD website that users can use to view their bans.");

        generateConfigOption("settings.debug.enabled.value", false);
        generateConfigOption("settings.debug.enabled.info", "Enables debug mode for the plugin. This will output more information to the console.");
    }


    private void generateConfigOption(String key, Object defaultValue) {
        if (this.getProperty(key) == null) {
            this.setProperty(key, defaultValue);
        }
        final Object value = this.getProperty(key);
        this.removeProperty(key);
        this.setProperty(key, value);
    }

    public Object getConfigOption(String key) {
        return this.getProperty(key);
    }

    public Object getConfigOption(String key, Object defaultValue) {
        Object value = getConfigOption(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;

    }

    //Getters Start

    public String getConfigString(String key) {
        return String.valueOf(getConfigOption(key));
    }

    public Integer getConfigInteger(String key) {
        return Integer.valueOf(getConfigString(key));
    }

    public Long getConfigLong(String key) {
        return Long.valueOf(getConfigString(key));
    }

    public Double getConfigDouble(String key) {
        return Double.valueOf(getConfigString(key));
    }

    public Boolean getConfigBoolean(String key) {
        return Boolean.valueOf(getConfigString(key));
    }


    //Getters End

    public boolean isNew() {
        return isNew;
    }
}

