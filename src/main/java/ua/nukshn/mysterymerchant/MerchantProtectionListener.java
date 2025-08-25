package ua.nukshn.mysterymerchant;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

public class MerchantProtectionListener implements Listener {

    private boolean isProtectedMerchant(Entity e) {
        if (!(e instanceof Villager)) return false;
        try {
            return e.getPersistentDataContainer().has(SpawnTask.getMerchantKey(), PersistentDataType.BYTE);
        } catch (Exception ignored) { return false; }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isProtectedMerchant(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (isProtectedMerchant(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (isProtectedMerchant(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (isProtectedMerchant(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (isProtectedMerchant(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}

