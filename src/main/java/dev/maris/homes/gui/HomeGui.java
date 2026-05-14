package dev.maris.homes.gui;

import dev.maris.homes.MarisHomesPlugin;
import dev.maris.homes.db.HomeData;
import dev.maris.homes.db.HomeRepository;
import dev.maris.homes.scheduler.PlatformScheduler;
import dev.maris.homes.util.Msg;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HomeGui implements Listener {
    private final MarisHomesPlugin plugin;
    private HomeRepository repository;
    private final PlatformScheduler scheduler;
    private final Map<UUID, Integer> pendingConfirm = new ConcurrentHashMap<>();
    private final Set<UUID> reopening = ConcurrentHashMap.newKeySet();
    private final Map<UUID, TeleportSession> activeTeleports = new ConcurrentHashMap<>();

    public HomeGui(MarisHomesPlugin plugin, HomeRepository repository, PlatformScheduler scheduler) {
        this.plugin = plugin;
        this.repository = repository;
        this.scheduler = scheduler;
    }

    public void setRepository(HomeRepository repository) { this.repository = repository; }

    public void openHomes(Player player) {
        repository.getHomes(player.getUniqueId()).thenAccept(homes -> scheduler.runEntity(player, () -> player.openInventory(createHomesInventory(player, homes))));
    }

    private Inventory createHomesInventory(Player player, Map<Integer, HomeData> homes) {
        FileConfiguration gui = plugin.configs().gui();
        int rows = clamp(gui.getInt("homes-gui.rows", 4), 1, 6);
        Inventory inv = Bukkit.createInventory(new HomesHolder(), rows * 9, Msg.color(gui.getString("homes-gui.title", "&8ʜᴏᴍᴇs")));
        fill(inv, "homes-gui.filler");
        int[] bedSlots = slots("homes-gui.bed-slots", new int[]{10, 11, 12, 13, 14, 15, 16});
        int[] actionSlots = slots("homes-gui.action-slots", new int[]{19, 20, 21, 22, 23, 24, 25});
        int maxHomes = Math.min(7, plugin.getConfig().getInt("homes.max-homes", 7));
        for (int i = 1; i <= maxHomes; i++) {
            boolean perm = hasHomePermission(player, i);
            boolean has = homes.containsKey(i);
            if (i - 1 < bedSlots.length && validSlot(inv, bedSlots[i - 1])) inv.setItem(bedSlots[i - 1], item(itemPath("bed", perm, has), i));
            if (i - 1 < actionSlots.length && validSlot(inv, actionSlots[i - 1])) inv.setItem(actionSlots[i - 1], item(itemPath("action", perm, has), i));
        }
        return inv;
    }

    private String itemPath(String type, boolean perm, boolean has) {
        if (!perm) return "homes-gui.items." + type + "-no-permission";
        return "homes-gui.items." + type + (has ? "-set" : "-empty");
    }

    private ItemStack item(String path, int home) {
        FileConfiguration gui = plugin.configs().gui();
        Material mat = material(gui.getString(path + ".material", "STONE"), Material.STONE);
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(replace(gui.getString(path + ".name", ""), home)));
            List<String> lore = new ArrayList<>();
            for (String s : gui.getStringList(path + ".lore")) lore.add(Msg.color(replace(s, home)));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fill(Inventory inv, String path) {
        FileConfiguration gui = plugin.configs().gui();
        if (!gui.getBoolean(path + ".enabled", false)) return;
        ItemStack filler = item(path, 0);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof HomesHolder) && !(holder instanceof ConfirmHolder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getInventory()) return;
        int raw = event.getRawSlot();
        if (holder instanceof HomesHolder) handleHomesClick(player, raw);
        else handleConfirmClick(player, raw, ((ConfirmHolder) holder).home);
    }

    private void handleHomesClick(Player player, int slot) {
        int bedIndex = indexOf(slots("homes-gui.bed-slots", new int[]{10, 11, 12, 13, 14, 15, 16}), slot);
        int dyeIndex = indexOf(slots("homes-gui.action-slots", new int[]{19, 20, 21, 22, 23, 24, 25}), slot);
        if (bedIndex >= 0) {
            int home = bedIndex + 1;
            if (!hasHomePermission(player, home)) return;
            repository.getHome(player.getUniqueId(), home).thenAccept(opt -> opt.ifPresent(data -> scheduler.runEntity(player, () -> {
                player.closeInventory();
                teleportWithCountdown(player, data);
            })));
            return;
        }
        if (dyeIndex >= 0) {
            int home = dyeIndex + 1;
            if (!hasHomePermission(player, home)) return;
            repository.getHome(player.getUniqueId(), home).thenAccept(opt -> {
                if (opt.isPresent()) scheduler.runEntity(player, () -> player.openInventory(createConfirmInventory(home)));
                else scheduler.runEntity(player, () -> setHome(player, home, true));
            });
        }
    }

    public void setHome(Player player, int home, boolean reopen) {
        if (isBlacklistedWorld(player.getWorld())) {
            String msg = plugin.configs().msg("cannot-set-home-here");
            player.sendMessage(msg);
            Msg.actionBar(player, msg);
            return;
        }
        Location l = player.getLocation();
        HomeData data = new HomeData(player.getUniqueId(), home, l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        repository.setHome(data).thenRun(() -> scheduler.runEntity(player, () -> {
            if (reopen) openHomes(player);
        }));
    }

    private Inventory createConfirmInventory(int home) {
        FileConfiguration gui = plugin.configs().gui();
        int rows = clamp(gui.getInt("confirm-delete-gui.rows", 3), 1, 6);
        Inventory inv = Bukkit.createInventory(new ConfirmHolder(home), rows * 9, Msg.color(gui.getString("confirm-delete-gui.title", "&8Confirm Delete")));
        setIfValid(inv, gui.getInt("confirm-delete-gui.cancel.slot", 11), item("confirm-delete-gui.cancel", home));
        setIfValid(inv, gui.getInt("confirm-delete-gui.display.slot", 13), item("confirm-delete-gui.display", home));
        setIfValid(inv, gui.getInt("confirm-delete-gui.confirm.slot", 15), item("confirm-delete-gui.confirm", home));
        return inv;
    }

    private void handleConfirmClick(Player player, int slot, int home) {
        FileConfiguration gui = plugin.configs().gui();
        if (slot == gui.getInt("confirm-delete-gui.cancel.slot", 11)) {
            reopening.add(player.getUniqueId());
            openHomes(player);
            return;
        }
        if (slot == gui.getInt("confirm-delete-gui.confirm.slot", 15)) {
            UUID id = player.getUniqueId();
            if (!Objects.equals(pendingConfirm.get(id), home)) {
                pendingConfirm.put(id, home);
                String raw = plugin.configs().msg("confirm-delete-again");
                if (plugin.configs().messages().getBoolean("settings.confirm-delete-chat", false)) {
                    player.sendMessage(raw);
                }
                if (plugin.configs().messages().getBoolean("settings.confirm-delete-actionbar", true)) {
                    Msg.actionBar(player, raw);
                }
                return;
            }
            pendingConfirm.remove(id);
            repository.deleteHome(id, home).thenRun(() -> scheduler.runEntity(player, () -> {
                reopening.add(id);
                openHomes(player);
            }));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof ConfirmHolder) {
            UUID id = player.getUniqueId();
            if (reopening.remove(id)) return;
            pendingConfirm.remove(id);
            if (plugin.configs().gui().getBoolean("confirm-delete-gui.reopen-homes-on-esc", true)) {
                scheduler.runEntityLater(player, () -> openHomes(player), 1L);
            }
        }
    }

    public void deleteHomeFast(Player player, int home) {
        repository.deleteHome(player.getUniqueId(), home);
    }

    public void teleportWithCountdown(Player player, HomeData data) {
        World world = Bukkit.getWorld(data.world());
        if (world == null) { scheduler.runEntity(player, () -> player.sendMessage(plugin.configs().msg("world-not-found"))); return; }
        int delay = plugin.getConfig().getInt("teleport.delay-seconds", 5);
        Location dest = new Location(world, data.x(), data.y(), data.z(), data.yaw(), data.pitch());
        TeleportSession session = new TeleportSession(player.getLocation());
        activeTeleports.put(player.getUniqueId(), session);

        for (int i = delay; i >= 1; i--) {
            int sec = i;
            scheduler.runEntityLater(player, () -> {
                if (!isActiveTeleport(player, session)) return;
                if (session.hasMoved(player.getLocation())) {
                    cancelTeleport(player, session);
                    return;
                }
                Msg.actionBar(player, plugin.configs().msg("teleport-countdown-actionbar", "cooldown", sec));
                playConfiguredSound(player, "teleport.tick-sound", Sound.BLOCK_NOTE_BLOCK_HAT);
            }, (delay - i) * 20L);
        }
        scheduler.runEntityLater(player, () -> {
            if (!isActiveTeleport(player, session)) return;
            if (session.hasMoved(player.getLocation())) {
                cancelTeleport(player, session);
                return;
            }
            activeTeleports.remove(player.getUniqueId(), session);
            scheduler.teleport(player, dest, () -> {
                player.sendMessage(plugin.configs().msg("teleport-success-chat"));
                Msg.actionBar(player, plugin.configs().msg("teleport-success-actionbar"));
                playConfiguredSound(player, "teleport.complete-sound", Sound.ENTITY_ENDERMAN_TELEPORT);
            });
        }, delay * 20L);
    }

    private boolean isActiveTeleport(Player player, TeleportSession session) {
        return activeTeleports.get(player.getUniqueId()) == session;
    }

    private void cancelTeleport(Player player, TeleportSession session) {
        if (!activeTeleports.remove(player.getUniqueId(), session)) return;
        String msg = plugin.configs().msg("teleport-cancelled-moved");
        player.sendMessage(msg);
        Msg.actionBar(player, msg);
    }

    private void playConfiguredSound(Player player, String path, Sound def) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) return;
        Sound sound;
        try { sound = Sound.valueOf(plugin.getConfig().getString(path + ".sound", def.name())); }
        catch (Exception e) { sound = def; }
        float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0D);
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0D);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public boolean isBlacklistedWorld(World world) {
        if (world == null) return false;
        List<String> list = plugin.getConfig().getStringList("homes.blacklist-worlds");
        for (String name : list) {
            if (matchesWorld(world, name)) return true;
        }
        return false;
    }

    private boolean matchesWorld(World world, String configured) {
        if (configured == null || configured.isBlank()) return false;
        String key = world.getKey().getKey();
        return configured.equalsIgnoreCase(world.getName())
                || configured.equalsIgnoreCase(world.getKey().toString())
                || configured.equalsIgnoreCase(key)
                || configured.equalsIgnoreCase(key.substring(key.lastIndexOf('/') + 1));
    }

    private boolean hasHomePermission(Player player, int home) {
        String format = plugin.getConfig().getString("homes.permission-format", "marishomes.%home%");
        return player.hasPermission(format.replace("%home%", String.valueOf(home)));
    }

    private int[] slots(String path, int[] def) {
        List<Integer> list = plugin.configs().gui().getIntegerList(path);
        if (list.isEmpty()) return def;
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private Material material(String raw, Material def) {
        try { return Material.valueOf(raw == null ? def.name() : raw.toUpperCase(Locale.ROOT)); }
        catch (Exception e) { return def; }
    }

    private String replace(String input, int home) { return input == null ? "" : input.replace("%home%", String.valueOf(home)); }
    private boolean validSlot(Inventory inv, int slot) { return slot >= 0 && slot < inv.getSize(); }
    private void setIfValid(Inventory inv, int slot, ItemStack item) { if (validSlot(inv, slot)) inv.setItem(slot, item); }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private int indexOf(int[] arr, int value) { for (int i = 0; i < arr.length; i++) if (arr[i] == value) return i; return -1; }

    static final class HomesHolder implements InventoryHolder { public Inventory getInventory() { return null; } }
    static final class ConfirmHolder implements InventoryHolder { final int home; ConfirmHolder(int home) { this.home = home; } public Inventory getInventory() { return null; } }

    private static final class TeleportSession {
        private final String world;
        private final int blockX;
        private final int blockY;
        private final int blockZ;

        private TeleportSession(Location start) {
            this.world = start.getWorld() == null ? "" : start.getWorld().getName();
            this.blockX = start.getBlockX();
            this.blockY = start.getBlockY();
            this.blockZ = start.getBlockZ();
        }

        private boolean hasMoved(Location now) {
            String nowWorld = now.getWorld() == null ? "" : now.getWorld().getName();
            return !world.equals(nowWorld) || blockX != now.getBlockX() || blockY != now.getBlockY() || blockZ != now.getBlockZ();
        }
    }
}
