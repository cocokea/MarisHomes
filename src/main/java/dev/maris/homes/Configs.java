package dev.maris.homes;

import dev.maris.homes.util.Msg;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public final class Configs {
    private final MarisHomesPlugin plugin;
    private FileConfiguration gui;
    private FileConfiguration messages;

    public Configs(MarisHomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        saveMissing("gui.yml");
        saveMissing("message.yml");
        plugin.reloadConfig();
        gui = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "message.yml"));
    }

    private void saveMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
    }

    public FileConfiguration gui() { return gui; }
    public FileConfiguration messages() { return messages; }

    public String msg(String path) {
        String prefix = messages.getString("prefix", "");
        String value = messages.getString("messages." + path, "&cMissing message: " + path);
        return Msg.color(value.replace("%prefix%", prefix));
    }

    public String msg(String path, String key, Object value) {
        return msg(path).replace("%" + key + "%", String.valueOf(value));
    }

    public List<String> guiLore(String path) {
        return gui.getStringList(path);
    }
}
