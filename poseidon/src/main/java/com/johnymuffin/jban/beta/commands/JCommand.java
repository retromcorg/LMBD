package com.johnymuffin.jban.beta.commands;

import com.johnymuffin.jban.beta.JohnyBans;
import org.bukkit.command.CommandExecutor;

public abstract class JCommand implements CommandExecutor {
    protected JohnyBans plugin;

    public JCommand(JohnyBans plugin) {
        this.plugin = plugin;
    }

}
