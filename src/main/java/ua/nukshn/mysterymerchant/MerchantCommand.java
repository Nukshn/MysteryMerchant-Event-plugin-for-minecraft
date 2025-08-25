package ua.nukshn.mysterymerchant;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MerchantCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("mysterymerchant.reload")) {
                MysteryMerchant plugin = MysteryMerchant.getInstance();
                plugin.reloadConfig();
                // Перезапускаем задачу спавна с новым интервалом
                plugin.rescheduleSpawnTask();
                sender.sendMessage("§aКонфиг перезагружен! Интервал и настройки обновлены.");
                return true;
            }
            sender.sendMessage("§cНет прав.");
            return true;
        }
        sender.sendMessage("Использование: /mysterymerchant reload");
        return true;
    }
}