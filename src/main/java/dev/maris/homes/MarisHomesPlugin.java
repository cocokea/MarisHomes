package dev.maris.homes;

import dev.maris.homes.command.HomeAdminCommand;
import dev.maris.homes.command.HomeCommand;
import dev.maris.homes.db.HomeRepository;
import dev.maris.homes.db.StorageManager;
import dev.maris.homes.gui.HomeGui;
import dev.maris.homes.scheduler.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MarisHomesPlugin extends JavaPlugin {
    private PlatformScheduler scheduler;
    private StorageManager storageManager;
    private HomeRepository homeRepository;
    private HomeGui homeGui;
    private Configs configs;

    @Override
    public void onEnable() {
        this.configs = new Configs(this);
        this.configs.load();
        this.scheduler = new PlatformScheduler(this);
        this.storageManager = new StorageManager(this);
        this.storageManager.init();
        this.homeRepository = new HomeRepository(this, storageManager);
        this.homeRepository.initSchema().join();
        this.homeGui = new HomeGui(this, homeRepository, scheduler);

        HomeCommand homeCommand = new HomeCommand(this, homeRepository, homeGui, scheduler);
        getCommand("home").setExecutor(homeCommand);
        getCommand("sethome").setExecutor(homeCommand);
        getCommand("delhome").setExecutor(homeCommand);
        getCommand("homeadmin").setExecutor(new HomeAdminCommand(this, storageManager, homeRepository, homeGui));
        getServer().getPluginManager().registerEvents(homeGui, this);
    }

    @Override
    public void onDisable() {
        if (storageManager != null) storageManager.close();
    }

    public void reloadAll() {
        configs.load();
        if (storageManager != null) storageManager.close();
        storageManager = new StorageManager(this);
        storageManager.init();
        homeRepository = new HomeRepository(this, storageManager);
        homeRepository.initSchema().join();
        homeGui.setRepository(homeRepository);
    }

    public Configs configs() { return configs; }


}

