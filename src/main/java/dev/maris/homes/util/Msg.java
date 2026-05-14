package dev.maris.homes.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Msg {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private Msg() {}

    public static String color(String input) {
        Matcher m = HEX.matcher(input == null ? "" : input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder repl = new StringBuilder("§x");
            for (char c : hex.toCharArray()) repl.append('§').append(c);
            m.appendReplacement(sb, repl.toString());
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static void actionBar(Player player, String message) {
        String colored = color(message);
        try {
            Method sendActionBar = player.getClass().getMethod("sendActionBar", String.class);
            sendActionBar.invoke(player, colored);
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object actionBar = Enum.valueOf((Class<Enum>) chatMessageType.asSubclass(Enum.class), "ACTION_BAR");
            Object components = textComponent.getMethod("fromLegacyText", String.class).invoke(null, colored);
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Method send = null;
            for (Method method : spigot.getClass().getMethods()) {
                if (method.getName().equals("sendMessage") && method.getParameterCount() == 2) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params[0].getName().equals("net.md_5.bungee.api.ChatMessageType") && params[1].isArray()) {
                        send = method;
                        break;
                    }
                }
            }
            if (send != null) send.invoke(spigot, actionBar, components);
        } catch (Throwable ignored) {
            // Do not fallback to chat. Some servers do not expose the actionbar API through reflection,
            // and falling back to chat duplicates confirmation messages.
        }
    }
}
