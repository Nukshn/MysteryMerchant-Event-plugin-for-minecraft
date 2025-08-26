package ua.nukshn.mysterymerchant;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.ArrayList;

public class MerchantGUI {

    private static final NamespacedKey PRICE_KEY = new NamespacedKey(MysteryMerchant.getInstance(), "price");
    private static final NamespacedKey MERCHANT_ITEM_KEY = new NamespacedKey(MysteryMerchant.getInstance(), "merchant_item");
    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(MysteryMerchant.getInstance(), "item_id");
    private static final NamespacedKey LIMIT_KEY = new NamespacedKey(MysteryMerchant.getInstance(), "limit");

    private static String guiTitle() { return ColorUtil.color(MysteryMerchant.getInstance().getConfig().getString("gui-title", Language.tr("gui.title"))); }

    public static void openMerchantGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, guiTitle());
        setupItems(gui, player);
        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
    }

    private static void addPricedItem(Inventory inv, int slot, String itemId, Material mat, String displayName, List<String> baseLore, int price, int limit, Player viewer) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.color(displayName));
        List<String> lore = new ArrayList<>();
        for (String l : baseLore) lore.add(ColorUtil.color(l));
        lore.add(Language.tr("item.price", "price", String.valueOf(price)));
        if (limit >= 0) {
            int remaining = MysteryMerchant.getInstance().getPurchaseManager().remaining(viewer.getUniqueId(), itemId, limit);
            String limitLine = Language.tr("item.limit", "remaining", String.valueOf(remaining), "limit", String.valueOf(limit));
            if ("item.limit".equals(limitLine)) {
                limitLine = ColorUtil.color("§7Лимит: §e" + remaining + "§7/§6" + limit);
            }
            lore.add(limitLine);
        } else {
            String inf = Language.tr("item.limit.infinite");
            if ("item.limit.infinite".equals(inf)) inf = ColorUtil.color("§7Лимит: §a∞");
            lore.add(inf);
        }
        lore.add(Language.tr("item.click-to-buy"));
        meta.setLore(lore);
        var pdc = meta.getPersistentDataContainer();
        pdc.set(PRICE_KEY, PersistentDataType.INTEGER, price);
        pdc.set(MERCHANT_ITEM_KEY, PersistentDataType.BYTE, (byte)1);
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);
        pdc.set(LIMIT_KEY, PersistentDataType.INTEGER, limit);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public static boolean isMerchantItem(ItemStack stack) { if (stack == null || !stack.hasItemMeta()) return false; ItemMeta meta = stack.getItemMeta(); return meta.getPersistentDataContainer().has(MERCHANT_ITEM_KEY, PersistentDataType.BYTE); }

    public static int getPrice(ItemStack stack) { if (stack == null || !stack.hasItemMeta()) return -1; ItemMeta meta = stack.getItemMeta(); Integer p = meta.getPersistentDataContainer().get(PRICE_KEY, PersistentDataType.INTEGER); return p == null ? -1 : p; }

    public static String getItemId(ItemStack stack) { if (stack == null || !stack.hasItemMeta()) return null; return stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING); }

    public static int getLimit(ItemStack stack) { if (stack == null || !stack.hasItemMeta()) return -1; Integer v = stack.getItemMeta().getPersistentDataContainer().get(LIMIT_KEY, PersistentDataType.INTEGER); return v == null ? -1 : v; }

    private static void setupItems(Inventory gui, Player viewer) {
        var plugin = MysteryMerchant.getInstance(); var cfg = plugin.getConfig(); ConfigurationSection section = cfg.getConfigurationSection("items"); if (section == null) return; int[] preferredSlots = {11,13,15,10,16,12,14}; int index = 0;
        for (String id : section.getKeys(false)) {
            ConfigurationSection it = section.getConfigurationSection(id); if (it == null) continue; String matStr = it.getString("material", "STONE"); Material mat = Material.matchMaterial(matStr.toUpperCase()); if (mat == null) mat = Material.STONE; int price = it.getInt("price", 0); int limit = it.getInt("limit", -1); String name = it.getString("name", id); List<String> rawLore = it.getStringList("lore"); List<String> lore = new ArrayList<>(rawLore); int slot = index < preferredSlots.length ? preferredSlots[index] : index; addPricedItem(gui, slot, id, mat, name, lore, price, limit, viewer); index++; }
    }

    public static boolean isGuiTitle(String title) { return guiTitle().equals(title); }
}