package com.johnymuffin.jban.beta;

import org.bukkit.plugin.Plugin;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.HashMap;

public class JBLang extends Configuration {
    private static JBLang singleton = null;
    private HashMap<String, String> map;

    private JBLang(Plugin plugin) {
        super(new File(plugin.getDataFolder(), "language.yml"));
        map = new HashMap<String, String>();
        loadDefaults();
        loadFile();
    }

    private void loadDefaults() {
        //General Stuff
        map.put("no_permission", "&4Sorry, you don't have permission for this command.");
        map.put("unavailable_to_console", "&4Sorry, console can't run this command.");
        map.put("player_not_found_full", "&4Can't find a player called &9%username%");
        map.put("generic_error", "&4Sorry, an error occurred running that command, please contact staff!");
        map.put("generic_error_player", "&4Sorry, an error occurred:&f %var1%");


    }

    private void loadFile() {
        this.load();
        for (String key : map.keySet()) {
            if (this.getString(key) == null) {
                this.setProperty(key, map.get(key));
            } else {
                map.put(key, this.getString(key));
            }
        }
        this.save();
    }

    public String getMessage(String msg) {
        String loc = map.get(msg);
        if (loc != null) {
            return loc.replace("&", "\u00a7");
        }
        return msg;
    }


    public static JBLang getInstance() {
        if (JBLang.singleton == null) {
            throw new RuntimeException("A instance of Fundamentals hasn't been passed into JBLang yet.");
        }
        return JBLang.singleton;
    }

    public static JBLang getInstance(JohnyBans plugin) {
        if (JBLang.singleton == null) {
            JBLang.singleton = new JBLang(plugin);
        }
        return JBLang.singleton;
    }


}