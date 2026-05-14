package dev.maris.homes.command;

import dev.maris.homes.MarisHomesPlugin;
import dev.maris.homes.db.HomeRepository;
import dev.maris.homes.gui.HomeGui;
import dev.maris.homes.scheduler.PlatformScheduler;
import dev.maris.homes.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HomeCommand implements CommandExecutor, TabCompleter {
    private final MarisHomesPlugin plugin;
    private final HomeRepository repository;
    private final HomeGui gui;
    private final PlatformScheduler scheduler;

    public HomeCommand(MarisHomesPlugin plugin, HomeRepository repository, HomeGui gui, PlatformScheduler scheduler) {
        this.plugin = plugin;
        this.repository = repository;
        this.gui = gui;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configs().msg("player-only"));
            return true;
        }

        String cmd = command.getName().toLowerCase();
        if (cmd.equals("home") || cmd.equals("homes")) {
            if (args.length == 0) {
                gui.openHomes(player);
            } else {
                parseHomeArg(args[0], player).ifPresent(home -> repository.getHome(player.getUniqueId(), home).thenAccept(opt ->
                    opt.ifPresentOrElse(
                        data -> scheduler.runEntity(player, () -> gui.teleportWithCountdown(player, data)),
                        () -> scheduler.runEntity(player, () -> player.sendMessage(plugin.configs().msg("home-not-set")))
                    )
                ));
            }
            return true;
        }

        if (cmd.equals("sethome")) {
            if (gui.isBlacklistedWorld(player.getWorld())) {
                String msg = plugin.configs().msg("cannot-set-home-here");
                player.sendMessage(msg);
                Msg.actionBar(player, msg);
                return true;
            }
            int requested = args.length == 0 ? 1 : parseNumberLoose(args[0], 1);
            repository.getHomes(player.getUniqueId()).thenAccept(homes -> {
                int target = firstAvailableFrom(player, homes, requested);
                if (target == -1) {
                    scheduler.runEntity(player, () -> player.sendMessage(plugin.configs().msg("no-available-home-slot")));
                    return;
                }
                if (!hasHomePermission(player, target)) {
                    scheduler.runEntity(player, () -> player.sendMessage(plugin.configs().msg("no-permission")));
                    return;
                }
                gui.setHome(player, target, false);
            });
            return true;
        }

        if (cmd.equals("delhome")) {
            if (args.length == 0) {
                player.sendMessage(plugin.configs().msg("delhome-usage"));
                return true;
            }
            int home = parseNumberLoose(args[0], 1);
            if (home < 1 || home > plugin.getConfig().getInt("homes.max-homes", 7)) {
                player.sendMessage(plugin.configs().msg("delhome-usage"));
                return true;
            }
            if (!hasHomePermission(player, home)) {
                player.sendMessage(plugin.configs().msg("no-permission"));
                return true;
            }
            gui.deleteHomeFast(player, home);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        String cmd = command.getName().toLowerCase();
        int max = Math.min(7, plugin.getConfig().getInt("homes.max-homes", 7));

        if ((cmd.equals("home") || cmd.equals("homes") || cmd.equals("sethome") || cmd.equals("delhome")) && args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (int i = 1; i <= max; i++) {
                if (!hasHomePermission(player, i)) continue;
                String value = String.valueOf(i);
                if (value.startsWith(prefix)) completions.add(value);
            }
            return completions;
        }

        return Collections.emptyList();
    }

    private int firstAvailableFrom(Player player, Map<Integer, ?> homes, int start) {
        int max = Math.min(7, plugin.getConfig().getInt("homes.max-homes", 7));
        int s = Math.max(1, Math.min(max, start));
        if (plugin.getConfig().getBoolean("homes.sethome-auto-next-free-slot", true)) {
            for (int i = s; i <= max; i++) if (hasHomePermission(player, i) && !homes.containsKey(i)) return i;
            for (int i = 1; i < s; i++) if (hasHomePermission(player, i) && !homes.containsKey(i)) return i;
        }
        return hasHomePermission(player, s) ? s : -1;
    }

    private java.util.Optional<Integer> parseHomeArg(String raw, Player player) {
        int home = parseNumberLoose(raw, -1);
        if (home < 1 || home > plugin.getConfig().getInt("homes.max-homes", 7)) {
            scheduler.runEntity(player, () -> player.sendMessage(plugin.configs().msg("invalid-home")));
            return java.util.Optional.empty();
        }
        if (!hasHomePermission(player, home)) {
            scheduler.runEntity(player, () -> player.sendMessage(plugin.configs().msg("no-permission")));
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(home);
    }

    private int parseNumberLoose(String raw, int def) {
        if (!plugin.getConfig().getBoolean("homes.sethome-loose-number-parser", true)) {
            try { return Integer.parseInt(raw); } catch (Exception e) { return def; }
        }
        String digits = raw.replaceAll("\\D+", "");
        if (digits.isEmpty()) return def;
        try { return Integer.parseInt(digits.substring(0, 1)); } catch (Exception e) { return def; }
    }

    private boolean hasHomePermission(Player player, int home) {
        String format = plugin.getConfig().getString("homes.permission-format", "marishomes.%home%");
        return player.hasPermission(format.replace("%home%", String.valueOf(home)));
    }
}
