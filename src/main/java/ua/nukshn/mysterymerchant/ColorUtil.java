package ua.nukshn.mysterymerchant;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utility for translating legacy color codes & and hex patterns like '&#FFAABB' */
public final class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    public static String color(String input) {
        if (input == null || input.isEmpty()) return input;
        // Replace hex patterns first
        Matcher m = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        m.appendTail(sb);
        // Translate traditional & codes (&a, &l, etc.)
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static List<String> color(List<String> lines) {
        if (lines == null) return null;
        return lines.stream().map(ColorUtil::color).collect(Collectors.toList());
    }
}

