package ua.nukshn.mysterymerchant;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpawnTask extends BukkitRunnable {

    private Villager currentMerchant;
    private final Set<Material> UNSAFE_BLOCKS = new HashSet<>();
    private final Set<Material> SAFE_GROUND_BLOCKS = new HashSet<>();
    private BossBar merchantBar;
    private BukkitTask countdownTask;
    private BukkitTask particleTask;
    private long despawnAtMillis;
    private long despawnDurationSeconds; // configurable
    private static final NamespacedKey MERCHANT_KEY = new NamespacedKey(MysteryMerchant.getInstance(), "mystery_merchant");
    private static final Pattern COORD_PATTERN = Pattern.compile("\\[(-?\\d+)\\s*;\\s*(-?\\d+)\\s*;\\s*(-?\\d+)]");

    public SpawnTask() { setupBlockLists(); }

    private void setupBlockLists() {
        Collections.addAll(UNSAFE_BLOCKS,
                Material.WATER, Material.LAVA, Material.FIRE, Material.CACTUS,
                Material.MAGMA_BLOCK, Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW
        );
        Collections.addAll(SAFE_GROUND_BLOCKS,
                Material.GRASS_BLOCK, Material.DIRT, Material.STONE, Material.SAND,
                Material.GRAVEL, Material.COBBLESTONE, Material.OAK_PLANKS,
                Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.NETHERRACK,
                Material.END_STONE, Material.BLACKSTONE, Material.BASALT,
                Material.ANDESITE, Material.GRANITE, Material.DIORITE,
                Material.PODZOL, Material.MYCELIUM, Material.CRIMSON_NYLIUM,
                Material.WARPED_NYLIUM
        );
    }

    @Override public void run() { spawnMerchant(); }

    public void spawnMerchant() {
        removeOldMerchant();
        World world = Bukkit.getWorld("world");
        if (world == null) { Bukkit.getLogger().warning(Language.tr("spawn.world-missing")); return; }
        var players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) { Bukkit.getLogger().warning(Language.tr("spawn.no-players")); return; }
        var cfg = MysteryMerchant.getInstance().getConfig();
        int radius = Math.max(5, cfg.getInt("spawn-radius", 30));
        int attempts = Math.max(5, cfg.getInt("spawn-attempts", 50));
        Location fallbackLocation = null; int fallbackScore = Integer.MIN_VALUE;
        for (Player player : players) {
            Location spawnLocation = findSafeLocationNearPlayer(player.getLocation(), radius, attempts);
            if (spawnLocation != null) { spawnMerchantAtLocation(world, spawnLocation); return; }
            Location bestEffort = findBestEffortLocationNearPlayer(player.getLocation(), radius, attempts * 2);
            int score = evaluateLocationSafety(bestEffort);
            if (score > fallbackScore && bestEffort != null) { fallbackLocation = bestEffort; fallbackScore = score; }
        }
        if (fallbackLocation != null) {
            Bukkit.getLogger().warning(Language.tr("spawn.fallback"));
            spawnMerchantAtLocation(world, fallbackLocation); return; }
        Bukkit.getLogger().warning(Language.tr("spawn.no-safe"));
    }

    private boolean isLocationInRiverRegion(Location loc) { int y = loc.getBlockY(); int x = loc.getBlockX(); int z = loc.getBlockZ(); return y < 65 && x > -200 && x < 200 && z > -200 && z < 200; }

    private Location findSafeLocationNearPlayer(Location center, int radius, int attempts) {
        Random random = new Random(); World world = center.getWorld();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2) - radius;
            int groundY = world.getHighestBlockYAt(x, z);
            int y = groundY + 1; Location testLoc = new Location(world, x + 0.5, y, z + 0.5);
            if (isLocationSafe(testLoc) && !isLocationInRiverRegion(testLoc)) { return testLoc; }
        } return null;
    }

    private Location findBestEffortLocationNearPlayer(Location center, int radius, int attempts) {
        Random random = new Random(); World world = center.getWorld(); Location bestLoc = null; int bestScore = Integer.MIN_VALUE;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2) - radius;
            int groundY = world.getHighestBlockYAt(x, z);
            int y = groundY + 1; Location testLoc = new Location(world, x + 0.5, y, z + 0.5);
            int score = evaluateLocationSafety(testLoc);
            if (!isLocationInRiverRegion(testLoc) && score > bestScore) { bestScore = score; bestLoc = testLoc; }
        } return bestLoc;
    }

    private int evaluateLocationSafety(Location loc) {
        try {
            World world = loc.getWorld(); int x = loc.getBlockX(); int y = loc.getBlockY(); int z = loc.getBlockZ(); int score = 0;
            Block ground = world.getBlockAt(x, y - 1, z); if (SAFE_GROUND_BLOCKS.contains(ground.getType()) && ground.getType().isSolid()) score += 3; else if (ground.getType().isSolid()) score += 1;
            Block feet = world.getBlockAt(x, y, z); if (feet.getType().isAir() || feet.isPassable()) score += 2; if (!UNSAFE_BLOCKS.contains(feet.getType())) score += 1;
            Block head = world.getBlockAt(x, y + 1, z); if (head.getType().isAir() || head.isPassable()) score += 2; if (!UNSAFE_BLOCKS.contains(head.getType())) score += 1;
            if (feet.getLightLevel() >= 5) score += 2; else if (feet.getLightLevel() >= 1) score += 1; return score;
        } catch (Exception e) { return Integer.MIN_VALUE; }
    }

    private boolean isLocationSafe(Location loc) {
        try {
            World world = loc.getWorld(); int x = loc.getBlockX(); int y = loc.getBlockY(); int z = loc.getBlockZ();
            Block ground = world.getBlockAt(x, y - 1, z); if (!SAFE_GROUND_BLOCKS.contains(ground.getType()) || !ground.getType().isSolid()) return false;
            Block feet = world.getBlockAt(x, y, z); if (UNSAFE_BLOCKS.contains(feet.getType()) || (!feet.getType().isAir() && !feet.isPassable())) return false;
            Block head = world.getBlockAt(x, y + 1, z); if (UNSAFE_BLOCKS.contains(head.getType()) || (!head.getType().isAir() && !head.isPassable())) return false;
            if (feet.getLightLevel() < 5) return false; return true;
        } catch (Exception e) { return false; }
    }

    private void spawnMerchantAtLocation(World world, Location loc) {
        try {
            var cfg = MysteryMerchant.getInstance().getConfig();
            String rawName = cfg.getString("merchant-name", "§6§lТаинственный Торговец");
            String name = ColorUtil.color(rawName);
            String profStr = cfg.getString("merchant-profession", "NITWIT").toUpperCase(Locale.ROOT);
            String typeStr = cfg.getString("merchant-type", "PLAINS").toUpperCase(Locale.ROOT);
            Villager.Profession profession; Villager.Type type;
            try { profession = Villager.Profession.valueOf(profStr); } catch (Exception ex) { profession = Villager.Profession.NITWIT; }
            try { type = Villager.Type.valueOf(typeStr); } catch (Exception ex) { type = Villager.Type.PLAINS; }
            Villager merchant = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
            merchant.setCustomName(name); merchant.setCustomNameVisible(true);
            merchant.setAI(false); merchant.setInvulnerable(true); merchant.setSilent(true); merchant.setGravity(true);
            merchant.setProfession(profession); merchant.setVillagerType(type);
            merchant.getPersistentDataContainer().set(MERCHANT_KEY, PersistentDataType.BYTE, (byte)1);
            currentMerchant = merchant;
            despawnDurationSeconds = Math.max(30, cfg.getLong("despawn-seconds", 900));
            broadcastSpawnMessage(loc, despawnDurationSeconds); playGlobalSpawnSounds(); setupBossBar(loc, despawnDurationSeconds); startParticleTask();
            Bukkit.getLogger().info(Language.tr("spawn.log.spawned", "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ())));
        } catch (Exception e) { Bukkit.getLogger().warning("Spawn error: " + e.getMessage()); }
    }

    private void startParticleTask() {
        if (particleTask != null) particleTask.cancel(); var cfg = MysteryMerchant.getInstance().getConfig(); if (!cfg.getBoolean("particles.enabled", true)) return;
        final String typeStr = cfg.getString("particles.type", "ENCHANTMENT_TABLE"); final int interval = cfg.getInt("particles.interval-ticks", 10);
        final int count = Math.max(1, cfg.getInt("particles.count", 8)); final double radius = cfg.getDouble("particles.radius", 0.6D);
        Particle parsed; try { parsed = Particle.valueOf(typeStr.toUpperCase(Locale.ROOT)); } catch (Exception ex) { parsed = null; String[] fallbacks = {"ENCHANTMENT_TABLE", "VILLAGER_HAPPY", "PORTAL", "CRIT"}; for (String fb : fallbacks) { try { parsed = Particle.valueOf(fb); break; } catch (Exception ignore) {} } if (parsed == null) { parsed = Particle.FLAME; } }
        final Particle particle = parsed;
        particleTask = new BukkitRunnable() { double angle = 0; @Override public void run() { if (currentMerchant == null || currentMerchant.isDead()) { cancel(); return; } Location base = currentMerchant.getLocation().clone().add(0, 1.0, 0); for (int i = 0; i < count; i++) { double a = angle + (2 * Math.PI / count) * i; double x = Math.cos(a) * radius; double z = Math.sin(a) * radius; base.getWorld().spawnParticle(particle, base.getX() + x, base.getY() + 0.1 * Math.sin(angle + i), base.getZ() + z, 1, 0,0,0,0); } angle += Math.PI / 20; } }.runTaskTimer(MysteryMerchant.getInstance(), 0L, Math.max(1L, interval)); }

    private void broadcastSpawnMessage(Location loc, long despawnSeconds) {
        var cfg = MysteryMerchant.getInstance().getConfig();
        String template = cfg.contains("spawn-message") ? cfg.getString("spawn-message") : Language.tr("merchant.spawn-message");
        String timeStr = formatTime(despawnSeconds);
        String msg = template.replace("%x", String.valueOf(loc.getBlockX()))
                .replace("%y", String.valueOf(loc.getBlockY()))
                .replace("%z", String.valueOf(loc.getBlockZ()))
                .replace("%time%", timeStr);
        msg = ColorUtil.color(msg);
        String[] lines = msg.split("\\n"); int x = loc.getBlockX(); int y = loc.getBlockY(); int z = loc.getBlockZ();
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                Matcher m = COORD_PATTERN.matcher(line);
                if (m.find()) {
                    if (p.isOp() || p.hasPermission("mysterymerchant.tp")) {
                        String before = line.substring(0, m.start()); String coordsText = line.substring(m.start(), m.end()); String after = line.substring(m.end());
                        TextComponent beforeComp = new TextComponent(before); TextComponent coordsComp = new TextComponent(coordsText);
                        coordsComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teleport " + x + " " + y + " " + z));
                        coordsComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ColorUtil.color(Language.tr("coords.teleport-hover"))).create()));
                        TextComponent afterComp = new TextComponent(after); TextComponent full = new TextComponent("");
                        full.addExtra(beforeComp); full.addExtra(coordsComp); full.addExtra(afterComp); p.spigot().sendMessage(full);
                    } else { p.sendMessage(line); }
                } else { p.sendMessage(line); }
            }
        }
    }

    private void playGlobalSpawnSounds() {
        var cfg = MysteryMerchant.getInstance().getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("sounds.spawn");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection entry = section.getConfigurationSection(key);
                    if (entry == null) continue;
                    String soundName = entry.getString("sound", "ENTITY_ILLUSIONER_PREPARE_MIRROR").toUpperCase(Locale.ROOT);
                    float volume = (float) entry.getDouble("volume", 1.0);
                    float pitch = (float) entry.getDouble("pitch", 1.0);
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf(soundName), volume, pitch);
                    } catch (Exception ignored) {}
                }
            } else {
                var list = cfg.getList("sounds.spawn");
                if (list != null) {
                    for (Object o : list) {
                        if (o instanceof Map) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) o;
                            String soundName = String.valueOf(map.getOrDefault("sound", "ENTITY_ILLUSIONER_PREPARE_MIRROR"));
                            float volume;
                            float pitch;
                            try { volume = Float.parseFloat(String.valueOf(map.getOrDefault("volume", 1.0))); } catch (NumberFormatException ex) { volume = 1.0f; }
                            try { pitch = Float.parseFloat(String.valueOf(map.getOrDefault("pitch", 1.0))); } catch (NumberFormatException ex) { pitch = 1.0f; }
                            try { p.playSound(p.getLocation(), Sound.valueOf(soundName.toUpperCase(Locale.ROOT)), volume, pitch); } catch (Exception ignored) {}
                        }
                    }
                } else {
                    // default fallback sounds
                    p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.2f, 1.2f);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }
    }

    private void setupBossBar(Location loc, long despawnSeconds) {
        removeBossBar();
        merchantBar = Bukkit.createBossBar(Language.tr("bossbar.loading"), BarColor.YELLOW, BarStyle.SEGMENTED_12);
        merchantBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            merchantBar.addPlayer(p);
        }
        despawnAtMillis = System.currentTimeMillis() + despawnSeconds * 1000L;
        startCountdown(loc, despawnSeconds);
    }

    public void addPlayerToBossBar(Player p) { if (merchantBar != null) merchantBar.addPlayer(p); }

    private void startCountdown(Location loc, long totalSeconds) {
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = new BukkitRunnable() { @Override public void run() { if (currentMerchant == null || currentMerchant.isDead()) { cancel(); removeBossBar(); return; } long remaining = (despawnAtMillis - System.currentTimeMillis()) / 1000L; if (remaining <= 0) { removeMerchant(); Bukkit.broadcastMessage(Language.tr("merchant.despawned")); MysteryMerchant.getInstance().getPurchaseManager().resetAll(); cancel(); return; } String timeStr = formatTime(remaining); double progress = Math.max(0.0, Math.min(1.0, (double) remaining / totalSeconds)); String title = Language.tr("bossbar.countdown", "time", timeStr, "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ())); merchantBar.setTitle(title); merchantBar.setProgress(progress); } }.runTaskTimer(MysteryMerchant.getInstance(), 0L, 20L);
    }

    private String formatTime(long seconds) { long m = seconds / 60; long s = seconds % 60; return String.format("%02d:%02d", m, s); }

    private void removeBossBar() { if (merchantBar != null) { merchantBar.removeAll(); merchantBar = null; } }

    public void removeMerchant() { if (currentMerchant != null) { try { currentMerchant.remove(); } catch (Exception ignored) {} currentMerchant = null; } if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; } if (particleTask != null) { particleTask.cancel(); particleTask = null; } removeBossBar(); }

    private void removeOldMerchant() { removeMerchant(); }

    public Location getCurrentMerchantLocation() { return (currentMerchant != null && !currentMerchant.isDead()) ? currentMerchant.getLocation() : null; }
    public boolean isMerchantAlive() { return currentMerchant != null && !currentMerchant.isDead(); }

    public Villager getCurrentMerchant() { return currentMerchant; }
    public static NamespacedKey getMerchantKey() { return MERCHANT_KEY; }
}
