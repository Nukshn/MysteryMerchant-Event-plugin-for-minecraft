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
                Language.init(plugin.getConfig().getString("language", "en"));
                plugin.rescheduleSpawnTask();
                sender.sendMessage(Language.tr("command.reload.done"));
                return true;
            }
            sender.sendMessage(Language.tr("command.reload.no-permission"));
            return true;
        }
        sender.sendMessage(Language.tr("command.reload.usage"));
        return true;
    }
}