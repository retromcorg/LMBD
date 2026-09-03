package com.johnymuffin.jban.beta;

import com.johnymuffin.jban.core.Ban;
import com.johnymuffin.jban.core.Mute;

public final class JBEnforcementPolicy {
    private JBEnforcementPolicy() {
    }

    public static boolean isBanEffective(JohnyBans plugin, Ban playerBan) {
        boolean evidenceBypass = !hasRequiredEvidence(plugin, playerBan.getServerName(), playerBan.getEvidences().size());
        boolean serverIgnored = isBanServerIgnored(plugin, playerBan.getServerName());
        return !evidenceBypass && !serverIgnored;
    }

    public static boolean isMuteEffective(JohnyBans plugin, Mute mute) {
        if (mute == null || !mute.isMuteActive()) {
            return false;
        }

        if (isMuteServerIgnored(plugin, mute.getServerName())) {
            return false;
        }

        if (!hasRequiredEvidence(plugin, mute.getServerName(), mute.getEvidences().size())) {
            return false;
        }

        if (mute.getServerName().equalsIgnoreCase(plugin.getServerName())) {
            return plugin.getJbConfig().getConfigBoolean("require.mutes.self.value");
        }
        return plugin.getJbConfig().getConfigBoolean("require.mutes.others.value");
    }

    public static boolean hasRequiredEvidence(JohnyBans plugin, String serverName, int evidenceCount) {
        if (evidenceCount > 0) {
            return true;
        }
        if (serverName.equalsIgnoreCase(plugin.getServerName())) {
            return !plugin.getJbConfig().getConfigBoolean("require.evidence.self.value");
        }
        return !plugin.getJbConfig().getConfigBoolean("require.evidence.others.value");
    }

    public static boolean isBanServerIgnored(JohnyBans plugin, String serverName) {
        return isServerIgnored(plugin, "ignore-bans", serverName);
    }

    public static boolean isMuteServerIgnored(JohnyBans plugin, String serverName) {
        return isServerIgnored(plugin, "ignore-mutes", serverName);
    }

    public static boolean isServerIgnored(JohnyBans plugin, String configPrefix, String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return false;
        }
        if (!plugin.getJbConfig().getConfigBoolean(configPrefix + ".enabled.value")) {
            return false;
        }

        Object ignoredServers = plugin.getJbConfig().getConfigOption(configPrefix + ".servers.value", null);
        if (ignoredServers instanceof Iterable) {
            for (Object ignoredServer : (Iterable<?>) ignoredServers) {
                if (isIgnoredServerMatch(ignoredServer, serverName)) {
                    return true;
                }
            }
            return false;
        }

        return isIgnoredServerMatch(ignoredServers, serverName);
    }

    private static boolean isIgnoredServerMatch(Object ignoredServer, String serverName) {
        if (ignoredServer == null) {
            return false;
        }
        String serverCheckName = String.valueOf(ignoredServer).trim();
        return !serverCheckName.isEmpty() && serverCheckName.equalsIgnoreCase(serverName.trim());
    }
}
