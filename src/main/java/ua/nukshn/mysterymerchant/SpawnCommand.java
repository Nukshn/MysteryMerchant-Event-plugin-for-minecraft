package ua.nukshn.mysterymerchant;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mysterymerchant.spawn")) {
                MysteryMerchant.getInstance().getSpawnTask().spawnMerchant();
                player.sendMessage("§aТаинственный торговец призван!");

                // Дебаг информация
                Location playerLoc = player.getLocation();
                player.sendMessage("§7Ваши координаты: §e" +
                        playerLoc.getBlockX() + " " + playerLoc.getBlockY() + " " + playerLoc.getBlockZ());

            } else {
                player.sendMessage("§cНет прав на использование этой команды!");
            }
        }
        return true;
    }
}