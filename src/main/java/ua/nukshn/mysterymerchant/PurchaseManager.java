package ua.nukshn.mysterymerchant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PurchaseManager {
    // player -> (itemId -> count)
    private final Map<UUID, Map<String, Integer>> data = new HashMap<>();

    public int getBought(UUID player, String itemId) {
        return data.getOrDefault(player, Map.of()).getOrDefault(itemId, 0);
    }

    public boolean canBuy(UUID player, String itemId, int limit) {
        if (limit < 0) return true; // unlimited
        return getBought(player, itemId) < limit;
    }

    public void recordBuy(UUID player, String itemId) {
        data.computeIfAbsent(player, k -> new HashMap<>())
                .merge(itemId, 1, Integer::sum);
    }

    public int remaining(UUID player, String itemId, int limit) {
        if (limit < 0) return -1; // unlimited
        return Math.max(0, limit - getBought(player, itemId));
    }

    public void resetAll() {
        data.clear();
    }
}

