package ua.nukshn.mysterymerchant;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.tr("command.spawn.player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (player.hasPermission("mysterymerchant.spawn")) {
            MysteryMerchant.getInstance().getSpawnTask().spawnMerchant();
            player.sendMessage(Language.tr("command.spawn.success"));
            Location loc = player.getLocation();
            player.sendMessage(Language.tr("command.spawn.player-coords", "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ())));
        } else {
            player.sendMessage(Language.tr("command.spawn.no-permission"));
        }
        return true;
    }
}