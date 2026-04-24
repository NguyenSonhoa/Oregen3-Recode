package me.banbeucmas.oregen3.commands;

import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.gui.UpgradeGUI;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UpgradeCommand extends AbstractCommand {
    public UpgradeCommand(Oregen3 plugin, CommandSender sender, String label, String[] args) {
        super(plugin, "oregen3.upgrade", sender, label, args);
    }

    @Override
    protected ExecutionResult run() {
        if (!(sender instanceof Player)) {
            return ExecutionResult.NON_PLAYER;
        }
        Player player = (Player) sender;
        OfflinePlayer owner = plugin.getUtils().getOwner(player.getLocation());
        
        if (owner == null) {
            player.sendMessage(plugin.getStringParser().getColoredPrefixString(plugin.getConfig().getString("messages.noIsland"), player));
            return ExecutionResult.SUCCESS;
        }

        UUID ownerUUID = owner.getUniqueId();
        new UpgradeGUI(plugin, player, ownerUUID).open();
        
        return ExecutionResult.SUCCESS;
    }
}
