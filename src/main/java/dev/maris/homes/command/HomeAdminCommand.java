package dev.maris.homes.command;

import dev.maris.homes.MarisHomesPlugin;
import dev.maris.homes.db.HomeRepository;
import dev.maris.homes.db.StorageManager;
import dev.maris.homes.gui.HomeGui;
import org.bukkit.command.*;

public final class HomeAdminCommand implements CommandExecutor {
    private final MarisHomesPlugin plugin;
    public HomeAdminCommand(MarisHomesPlugin plugin, StorageManager sm, HomeRepository repo, HomeGui gui) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("marishomes.admin")) { sender.sendMessage(plugin.configs().msg("no-permission")); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.configs().msg("reload"));
            return true;
        }
        sender.sendMessage(plugin.configs().msg("reload-usage"));
        return true;
    }
}
