package ua.nukshn.mysterymerchant;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemoveMerchantCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MysteryMerchant.getInstance().getSpawnTask().removeMerchant();
        sender.sendMessage(Language.tr("remove.done"));
        return true;
    }
}
