package dev.maris.homes;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

final class MarisPluginStartup {
    private static final String DISCORD_URL = "https://discord.gg/9MpGtv2PQM";
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RELEASE_TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RELEASE_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Map<Character, String[]> FONT = font();

    private MarisPluginStartup() {
    }

    static void bootstrap(JavaPlugin plugin, String repoSlug) {
        plugin.saveDefaultConfig();
        printBanner(plugin);
        if (!plugin.getConfig().getBoolean("update-checker", true)) {
            return;
        }
        runAsync(plugin, new UpdateCheckTask(plugin, repoSlug));
    }

    private static void printBanner(JavaPlugin plugin) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String line : render(plugin.getName())) {
            console.sendMessage(color("&b" + line));
        }
        console.sendMessage(color("&8[&b" + plugin.getName() + "&8] &7Discord: &f" + DISCORD_URL));
        console.sendMessage(color("&8[&b" + plugin.getName() + "&8] &7Version: &f" + plugin.getDescription().getVersion()));
    }

    private static String[] render(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        String[] output = new String[] { "", "", "", "", "", "" };
        for (int i = 0; i < upper.length(); i++) {
            String[] glyph = FONT.getOrDefault(upper.charAt(i), FONT.get(' '));
            for (int row = 0; row < output.length; row++) {
                output[row] += glyph[row] + "  ";
            }
        }
        return output;
    }

    private static void runAsync(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private static void runSync(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static String color(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char character : hex.toCharArray()) {
                replacement.append('§').append(character);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private static String normalizeVersion(String value) {
        return value == null ? "" : value.trim().replaceFirst("^[vV]", "");
    }

    private static String extract(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Map<Character, String[]> font() {
        Map<Character, String[]> map = new LinkedHashMap<>();
        map.put(' ', new String[] { "  ", "  ", "  ", "  ", "  ", "  " });
        map.put('-', new String[] { "      ", "      ", " ████ ", "      ", "      ", "      " });
        map.put('A', new String[] { "   ██   ", "  ████  ", " ██  ██ ", " ██████ ", " ██  ██ ", " ██  ██ " });
        map.put('B', new String[] { " █████  ", " ██  ██ ", " █████  ", " ██  ██ ", " ██  ██ ", " █████  " });
        map.put('C', new String[] { "  ████ ", " ██    ", " ██    ", " ██    ", " ██    ", "  ████ " });
        map.put('D', new String[] { " █████  ", " ██  ██ ", " ██   ██", " ██   ██", " ██  ██ ", " █████  " });
        map.put('E', new String[] { " ██████ ", " ██     ", " █████  ", " ██     ", " ██     ", " ██████ " });
        map.put('F', new String[] { " ██████ ", " ██     ", " █████  ", " ██     ", " ██     ", " ██     " });
        map.put('G', new String[] { "  ████ ", " ██    ", " ██ ███", " ██  ██", " ██  ██", "  ████ " });
        map.put('H', new String[] { " ██  ██ ", " ██  ██ ", " ██████ ", " ██  ██ ", " ██  ██ ", " ██  ██ " });
        map.put('I', new String[] { " █████ ", "   ██  ", "   ██  ", "   ██  ", "   ██  ", " █████ " });
        map.put('J', new String[] { "  █████", "    ██ ", "    ██ ", "    ██ ", " ██ ██ ", "  ███  " });
        map.put('K', new String[] { " ██  ██ ", " ██ ██  ", " ████   ", " ██ ██  ", " ██  ██ ", " ██  ██ " });
        map.put('L', new String[] { " ██     ", " ██     ", " ██     ", " ██     ", " ██     ", " ██████ " });
        map.put('M', new String[] { " ██   ██ ", " ███ ███ ", " ██ █ ██ ", " ██   ██ ", " ██   ██ ", " ██   ██ " });
        map.put('N', new String[] { " ██   ██ ", " ███  ██ ", " ████ ██ ", " ██ ████ ", " ██  ███ ", " ██   ██ " });
        map.put('O', new String[] { "  ████  ", " ██  ██ ", " ██  ██ ", " ██  ██ ", " ██  ██ ", "  ████  " });
        map.put('P', new String[] { " █████  ", " ██  ██ ", " ██  ██ ", " █████  ", " ██     ", " ██     " });
        map.put('Q', new String[] { "  ████  ", " ██  ██ ", " ██  ██ ", " ██ ███ ", " ██  ██ ", "  █████ " });
        map.put('R', new String[] { " █████  ", " ██  ██ ", " ██  ██ ", " █████  ", " ██ ██  ", " ██  ██ " });
        map.put('S', new String[] { "  ████ ", " ██    ", "  ███  ", "    ██ ", "    ██ ", " ████  " });
        map.put('T', new String[] { " ██████ ", "   ██   ", "   ██   ", "   ██   ", "   ██   ", "   ██   " });
        map.put('U', new String[] { " ██  ██ ", " ██  ██ ", " ██  ██ ", " ██  ██ ", " ██  ██ ", "  ████  " });
        map.put('V', new String[] { " ██   ██ ", " ██   ██ ", " ██   ██ ", "  ██ ██  ", "  ██ ██  ", "   ███   " });
        map.put('W', new String[] { " ██   ██ ", " ██   ██ ", " ██ █ ██ ", " ██ █ ██ ", " ███ ███ ", " ██   ██ " });
        map.put('X', new String[] { " ██  ██ ", "  ████  ", "   ██   ", "  ████  ", " ██  ██ ", " ██  ██ " });
        map.put('Y', new String[] { " ██   ██ ", "  ██ ██  ", "   ███   ", "   ██    ", "   ██    ", "   ██    " });
        map.put('Z', new String[] { " ██████ ", "    ██  ", "   ██   ", "  ██    ", " ██     ", " ██████ " });
        map.put('0', new String[] { "  ████  ", " ██  ██ ", " ██ ███ ", " ███ ██ ", " ██  ██ ", "  ████  " });
        map.put('1', new String[] { "   ██   ", " ████   ", "   ██   ", "   ██   ", "   ██   ", " ██████ " });
        map.put('2', new String[] { "  ████  ", " ██  ██ ", "    ██  ", "   ██   ", "  ██    ", " ██████ " });
        map.put('3', new String[] { " █████  ", "     ██ ", "  ████  ", "     ██ ", " ██  ██ ", "  ████  " });
        map.put('4', new String[] { "    ██  ", "   ███  ", "  █ ██  ", " ██ ██  ", " ██████ ", "    ██  " });
        map.put('5', new String[] { " ██████ ", " ██     ", " █████  ", "     ██ ", " ██  ██ ", "  ████  " });
        map.put('6', new String[] { "  ████  ", " ██     ", " █████  ", " ██  ██ ", " ██  ██ ", "  ████  " });
        map.put('7', new String[] { " ██████ ", "    ██  ", "   ██   ", "  ██    ", "  ██    ", "  ██    " });
        map.put('8', new String[] { "  ████  ", " ██  ██ ", "  ████  ", " ██  ██ ", " ██  ██ ", "  ████  " });
        map.put('9', new String[] { "  ████  ", " ██  ██ ", " ██  ██ ", "  █████ ", "     ██ ", "  ████  " });
        return map;
    }

    private static final class UpdateCheckTask implements Runnable {
        private final JavaPlugin plugin;
        private final String repoSlug;

        private UpdateCheckTask(JavaPlugin plugin, String repoSlug) {
            this.plugin = plugin;
            this.repoSlug = repoSlug;
        }

        @Override
        public void run() {
            try {
                URL url = URI.create("https://api.github.com/repos/" + this.repoSlug + "/releases/latest").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", this.plugin.getName() + "-Updater");
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return;
                }
                byte[] bytes = connection.getInputStream().readAllBytes();
                String body = new String(bytes, StandardCharsets.UTF_8);
                String latestVersion = extract(RELEASE_TAG_PATTERN, body);
                String downloadUrl = extract(RELEASE_URL_PATTERN, body);
                if (latestVersion == null || downloadUrl == null) {
                    return;
                }
                String current = normalizeVersion(this.plugin.getDescription().getVersion());
                String latest = normalizeVersion(latestVersion);
                runSync(this.plugin, () -> announceResult(this.plugin, latest, current, downloadUrl));
            } catch (IOException ignored) {
            }
        }

        private void announceResult(JavaPlugin plugin, String latest, String current, String downloadUrl) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            if (latest.equalsIgnoreCase(current)) {
                console.sendMessage(color("&#00FF30You are using the latest version of " + plugin.getName() + "."));
                return;
            }
            console.sendMessage(color("&7New version &#00FF30" + latest + " &7for &#00FF30" + plugin.getName()
                    + "&7. Visit &f" + downloadUrl + " &7to download the latest version."));
        }
    }
}