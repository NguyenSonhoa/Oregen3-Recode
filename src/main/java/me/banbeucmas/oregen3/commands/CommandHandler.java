package me.banbeucmas.oregen3.commands;

import me.banbeucmas.oregen3.Oregen3;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private Oregen3 plugin;

    public CommandHandler(Oregen3 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();
        
        // Handle standalone commands like /gen, /ore, /nangcapgen
        if (commandName.equals("gen") || commandName.equals("ore") || commandName.equals("nangcapgen")) {
            new UpgradeCommand(plugin, sender, label, args).execute();
            return true;
        }

        // Handle main command /oregen3
        if (args.length == 0) {
            new UsageCommand(plugin, sender, label).execute();
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                new ReloadCommand(plugin, sender).execute();
                break;
            case "help":
                new HelpCommand(plugin, sender, label).execute();
                break;
            case "info":
                new InformationCommand(plugin, sender, label, args).execute();
                break;
            case "edit":
                new EditCommand(plugin, sender, args).execute();
                break;
            case "upgrade":
                new UpgradeCommand(plugin, sender, label, args).execute();
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, final String[] args) {
        String commandName = command.getName().toLowerCase();
        
        // No tab completion for standalone commands for now
        if (commandName.equals("gen") || commandName.equals("ore") || commandName.equals("nangcapgen")) {
            return Collections.emptyList();
        }

        if (args.length > 1) {
            return null;
        }
        final List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], Arrays.asList("reload", "help", "info", "edit", "upgrade"), completions);
        Collections.sort(completions);
        return completions;
    }
}
