package ua.nukshn.mysterymerchant;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ResetLimitsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mysterymerchant.resetlimits")) {
            sender.sendMessage(Language.tr("command.resetlimits.no-permission"));
            return true;
        }
        MysteryMerchant.getInstance().getPurchaseManager().resetAll();
        sender.sendMessage(Language.tr("command.resetlimits.done"));
        return true;
    }
}

