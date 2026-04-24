package me.banbeucmas.oregen3.hook.economy;

import me.banbeucmas.oregen3.Oregen3;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyHook {
    private Oregen3 plugin;
    private Economy econ = null;

    public VaultEconomyHook(Oregen3 plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean hasDependency() {
        return econ != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!hasDependency()) return 0;
        return econ.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!hasDependency()) return false;
        return econ.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!hasDependency()) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}
