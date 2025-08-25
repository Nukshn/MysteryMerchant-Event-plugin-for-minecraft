package ua.nukshn.mysterymerchant;

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

        purchaseManager = new PurchaseManager();

        if (!setupEconomy()) {
            getLogger().warning("Vault или экономия не найдены. Покупки будут отключены.");
        }

        getServer().getPluginManager().registerEvents(new MerchantListener(), this);
        getServer().getPluginManager().registerEvents(new MerchantProtectionListener(), this);
        getCommand("mysterymerchant").setExecutor(new MerchantCommand());
        getCommand("mmspawn").setExecutor(new SpawnCommand());
        getCommand("mmremove").setExecutor(new RemoveMerchantCommand());

        rescheduleSpawnTask();

        getLogger().info("Таинственный торговец активирован!");
    }

    @Override
    public void onDisable() {
        if (spawnTask != null) {
            spawnTask.removeMerchant();
            spawnTask.cancel();
        }
        getLogger().info("Таинственный торговец деактивирован!");
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
        getLogger().info("Интервал спавна торговца установлен: " + interval + " тиков (~" + (interval/20.0) + " сек)");
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
}