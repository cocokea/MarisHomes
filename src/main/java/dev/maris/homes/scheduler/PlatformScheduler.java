package dev.maris.homes.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PlatformScheduler {
    private final Plugin plugin;
    private final boolean folia;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = hasClass("io.papermc.paper.threadedregions.RegionizedServer")
                || hasMethod(Bukkit.class, "getGlobalRegionScheduler");
    }

    public boolean isFolia() { return folia; }

    public void runEntity(Player player, Runnable runnable) {
        if (folia) {
            if (invokeEntityScheduler(player, "run", runnable, 0L)) return;
            invokeGlobalScheduler("run", runnable, 0L);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runEntityLater(Player player, Runnable runnable, long ticks) {
        if (folia) {
            if (invokeEntityScheduler(player, "runDelayed", runnable, Math.max(1L, ticks))) return;
            invokeGlobalScheduler("runDelayed", runnable, Math.max(1L, ticks));
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
    }

    public void async(Runnable runnable) {
        if (folia) {
            try {
                Object async = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNow = findMethod(async.getClass(), "runNow", 2);
                if (runNow != null) {
                    runNow.setAccessible(true);
                    runNow.invoke(async, plugin, (Consumer<Object>) task -> runnable.run());
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not schedule async Folia task: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            CompletableFuture.runAsync(runnable);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public void teleport(Player player, Location location, Runnable after) {
        runEntity(player, () -> {
            try {
                Method teleportAsync = player.getClass().getMethod("teleportAsync", Location.class);
                Object future = teleportAsync.invoke(player, location);
                if (future instanceof CompletableFuture<?> cf) {
                    cf.thenAccept(ok -> runEntity(player, after));
                } else {
                    after.run();
                }
            } catch (Throwable ignored) {
                player.teleport(location);
                after.run();
            }
        });
    }

    private boolean invokeEntityScheduler(Player player, String methodName, Runnable runnable, long ticks) {
        try {
            Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
            int paramCount = methodName.equals("runDelayed") ? 4 : 3;
            Method method = findMethod(scheduler.getClass(), methodName, paramCount);
            if (method == null) return false;
            method.setAccessible(true);
            Consumer<Object> task = ignored -> runnable.run();
            if (methodName.equals("runDelayed")) method.invoke(scheduler, plugin, task, null, ticks);
            else method.invoke(scheduler, plugin, task, null);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not schedule Folia entity task " + methodName + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private boolean invokeGlobalScheduler(String methodName, Runnable runnable, long ticks) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            int paramCount = methodName.equals("runDelayed") ? 4 : 3;
            Method method = findMethod(scheduler.getClass(), methodName, paramCount);
            if (method == null) return false;
            method.setAccessible(true);
            Consumer<Object> task = ignored -> runnable.run();
            if (methodName.equals("runDelayed")) method.invoke(scheduler, plugin, task, ticks);
            else method.invoke(scheduler, plugin, task);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not schedule Folia global task " + methodName + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) return method;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) return method;
        }
        return null;
    }

    private static boolean hasClass(String name) {
        try { Class.forName(name); return true; } catch (ClassNotFoundException e) { return false; }
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) if (method.getName().equals(name)) return true;
        return false;
    }
}
