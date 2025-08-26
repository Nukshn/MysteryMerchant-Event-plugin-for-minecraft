package ua.nukshn.mysterymerchant;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Simple language manager loading language/<code>.yml (preferred) or lang/<code>.yml from disk, fallback to jar resources lang/<code>.yml. */
public final class Language {
    private static final Map<String, String> messages = new HashMap<>();
    private static String current = "en";
    private static final String[] CODES = {"en","ru","uk","de","fr"};

    private Language() {}

    public static void exportDefaultLanguageFiles(JavaPlugin plugin) {
        File dir = new File(plugin.getDataFolder(), "language");
        if (!dir.exists()) dir.mkdirs();
        for (String code : CODES) {
            File out = new File(dir, code + ".yml");
            if (out.exists()) continue; // don't overwrite
            try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
                if (in == null) continue;
                Files.copy(in, out.toPath());
            } catch (IOException ignored) {}
        }
    }

    public static void init(String code) {
        current = code == null ? "en" : code.toLowerCase();
        messages.clear();
        loadLangFile("en"); // base
        if (!"en".equals(current)) loadLangFile(current); // override
    }

    private static void loadLangFile(String code) {
        try {
            var plugin = MysteryMerchant.getInstance();
            // Preferred external override: /language/
            File extLanguage = new File(plugin.getDataFolder(), "language" + File.separator + code + ".yml");
            if (extLanguage.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(extLanguage);
                for (String k : cfg.getKeys(true)) if (!cfg.isConfigurationSection(k)) messages.put(k, cfg.getString(k));
                return;
            }
            // Legacy external override: /lang/
            File extLegacy = new File(plugin.getDataFolder(), "lang" + File.separator + code + ".yml");
            if (extLegacy.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(extLegacy);
                for (String k : cfg.getKeys(true)) if (!cfg.isConfigurationSection(k)) messages.put(k, cfg.getString(k));
                return;
            }
            // Jar resource fallback
            var in = plugin.getResource("lang/" + code + ".yml");
            if (in == null) return;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String k : cfg.getKeys(true)) if (!cfg.isConfigurationSection(k)) messages.put(k, cfg.getString(k));
        } catch (Exception ignored) {}
    }

    public static String tr(String key, String... replacements) {
        String value = messages.getOrDefault(key, key);
        if (replacements != null && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                value = value.replace("%" + replacements[i] + "%", replacements[i+1]);
            }
        }
        return ColorUtil.color(value);
    }
}
