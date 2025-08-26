package ua.nukshn.mysterymerchant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

public final class MysteryMerchant extends JavaPlugin {

    private static MysteryMerchant instance;
    private SpawnTask spawnTask;
    private Economy economy;
    private PurchaseManager purchaseManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Language.exportDefaultLanguageFiles(this);
        // Инициализация языков
        Language.init(getConfig().getString("language", "en"));
        if (getConfig().getBoolean("cleanup-on-start", true)) {
            cleanupResidualMerchants();
        }

        purchaseManager = new PurchaseManager();

        if (!setupEconomy()) {
            getLogger().warning(Language.tr("economy.missing"));
        }

        getServer().getPluginManager().registerEvents(new MerchantListener(), this);
        getServer().getPluginManager().registerEvents(new MerchantProtectionListener(), this);
        getCommand("mysterymerchant").setExecutor(new MerchantCommand());
        getCommand("mmspawn").setExecutor(new SpawnCommand());
        getCommand("mmremove").setExecutor(new RemoveMerchantCommand());
        getCommand("mmresetlimits").setExecutor(new ResetLimitsCommand());

        rescheduleSpawnTask();

        getLogger().info(Language.tr("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        if (spawnTask != null) {
            spawnTask.removeMerchant();
            spawnTask.cancel();
        }
        getLogger().info(Language.tr("plugin.disabled"));
    }

    public void rescheduleSpawnTask() {
        try {
            if (spawnTask != null) {
                spawnTask.removeMerchant();
                spawnTask.cancel();
            }
        } catch (Exception ignored) {}
        long interval = getConfig().getLong("spawn-interval", 108000L);
        if (interval < 20L) interval = 20L; // минимум 1 секунда
        spawnTask = new SpawnTask();
        spawnTask.runTaskTimer(this, 0L, interval);
        getLogger().info(Language.tr("spawn.interval.set", "interval", String.valueOf(interval), "seconds", String.format("%.1f", interval/20.0)));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static MysteryMerchant getInstance() {
        return instance;
    }

    public SpawnTask getSpawnTask() {
        return spawnTask;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PurchaseManager getPurchaseManager() {
        return purchaseManager;
    }

    private void cleanupResidualMerchants() {
        int removed = 0;
        try {
            var key = SpawnTask.getMerchantKey();
            String configuredName = ColorUtil.color(getConfig().getString("merchant-name", "§6§lТаинственный Торговец"));
            for (var world : Bukkit.getWorlds()) {
                for (Entity e : world.getEntitiesByClass(Villager.class)) {
                    boolean match = false;
                    try {
                        if (e.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) match = true;
                    } catch (Exception ignored) {}
                    if (!match) {
                        String n = e.getCustomName();
                        if (n != null) {
                            if (n.equalsIgnoreCase(configuredName) || n.contains("Таинственный") || n.toLowerCase().contains("mystery")) {
                                match = true;
                            }
                        }
                    }
                    if (match) {
                        try { e.remove(); removed++; } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        if (removed > 0) {
            getLogger().info(Language.tr("plugin.cleanup-removed", "count", String.valueOf(removed)));
        } else {
            getLogger().info(Language.tr("plugin.cleanup-none"));
        }
    }
}