package ua.nukshn.mysterymerchant;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.milkbowl.vault.economy.Economy;

import java.util.HashMap;

public class MerchantListener implements Listener {

    private static final String NEW_NAME = "§6§lТаинственный Торговец";
    private static final String OLD_NAME = "§6§lПластический синдрю"; // на случай старых сущностей

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        boolean isMerchant = false;
        try {
            if (villager.getPersistentDataContainer().has(SpawnTask.getMerchantKey(), PersistentDataType.BYTE)) {
                isMerchant = true;
            }
        } catch (Exception ignored) {}
        if (!isMerchant) {
            String name = villager.getCustomName();
            if (name != null && (name.equals(NEW_NAME) || name.equals(OLD_NAME))) {
                isMerchant = true; // fallback по имени
            }
        }
        if (!isMerchant) return;
        event.setCancelled(true);
        MerchantGUI.openMerchantGUI(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Подключившемуся игроку показываем текущий боссбар, если торговец ещё активен
        MysteryMerchant plugin = MysteryMerchant.getInstance();
        if (plugin != null) {
            SpawnTask task = plugin.getSpawnTask();
            if (task != null && task.isMerchantAlive()) {
                task.addPlayerToBossBar(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!MerchantGUI.isGuiTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        var current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;
        if (!MerchantGUI.isMerchantItem(current)) return;

        String itemId = MerchantGUI.getItemId(current);
        int limit = MerchantGUI.getLimit(current);
        PurchaseManager pm = MysteryMerchant.getInstance().getPurchaseManager();
        if (limit >= 0 && !pm.canBuy(player.getUniqueId(), itemId, limit)) {
            player.sendMessage("§cВы достигли лимита покупок для этого предмета.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            return;
        }

        Economy eco = MysteryMerchant.getInstance().getEconomy();
        if (eco == null) {
            player.sendMessage("§cЭкономика не настроена (Vault не найден). Покупка невозможна.");
            return;
        }
        int price = MerchantGUI.getPrice(current);
        if (price <= 0) {
            player.sendMessage("§cОшибка: цена не задана.");
            return;
        }
        if (!eco.has(player, price)) {
            player.sendMessage("§cНедостаточно средств. Нужно: §e" + price + "$");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        var resp = eco.withdrawPlayer(player, price);
        if (!resp.transactionSuccess()) {
            player.sendMessage("§cОшибка списания средств: " + resp.errorMessage);
            return;
        }
        ItemStack reward = new ItemStack(current);
        reward.setAmount(1);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        if (!leftover.isEmpty()) leftover.values().forEach(is -> player.getWorld().dropItemNaturally(player.getLocation(), is));
        pm.recordBuy(player.getUniqueId(), itemId);
        player.sendMessage("§aПокупка успешна! Списано §e" + price + "$§a.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        // Обновляем GUI, чтобы показать оставшийся лимит
        MerchantGUI.openMerchantGUI(player);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!MerchantGUI.isGuiTitle(event.getView().getTitle())) return;
        // Ничего нельзя перетаскивать
        event.setCancelled(true);
    }
}