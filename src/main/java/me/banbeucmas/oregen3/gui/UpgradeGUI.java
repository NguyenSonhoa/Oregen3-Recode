package me.banbeucmas.oregen3.gui;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.turtle.tutiencore.api.TuTien;
import com.turtle.tutiencore.api.realm.Realm;
import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.data.Generator;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class UpgradeGUI implements InventoryHandler {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    private final Oregen3 plugin;
    private final Player player;
    private final UUID islandOwnerUUID;
    private Inventory inventory;

    public UpgradeGUI(Oregen3 plugin, Player player, UUID islandOwnerUUID) {
        this.plugin = plugin;
        this.player = player;
        this.islandOwnerUUID = islandOwnerUUID;
    }

    public void open() {
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("upgrade-gui");
        if (guiSection == null) {
            player.sendMessage(ChatColor.RED + "[OreGen3] upgrade-gui section is missing in config.yml!");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', guiSection.getString("title", "&eNâng Cấp Đảo"));
        int size = guiSection.getInt("size", 45);

        inventory = Bukkit.createInventory(this, size, title);

        ConfigurationSection fillerSection = guiSection.getConfigurationSection("filler");
        if (fillerSection != null && fillerSection.getBoolean("enabled", true)) {
            ItemStack filler = buildFiller(fillerSection);
            for (int slot : fillerSection.getIntegerList("slots")) {
                if (slot >= 0 && slot < size) inventory.setItem(slot, filler);
            }
        }

        ConfigurationSection upgradesSection = guiSection.getConfigurationSection("upgrades");
        if (upgradesSection != null) {
            for (String key : upgradesSection.getKeys(false)) {
                ConfigurationSection sec = upgradesSection.getConfigurationSection(key);
                if (sec == null) continue;
                int slot = sec.getInt("slot", -1);
                if (slot < 0 || slot >= size) continue;
                inventory.setItem(slot, buildUpgradeItem(key, sec));
            }
        }

        player.openInventory(inventory);
    }

    private ItemStack buildFiller(ConfigurationSection sec) {
        String matName = sec.getString("material", "GRAY_STAINED_GLASS_PANE");
        XMaterial xMat = XMaterial.matchXMaterial(matName).orElse(XMaterial.GRAY_STAINED_GLASS_PANE);
        ItemStack item = xMat.parseItem();
        if (item == null) item = new ItemStack(Objects.requireNonNull(XMaterial.GRAY_STAINED_GLASS_PANE.parseMaterial()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = sec.getString("name", " ");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(new ArrayList<>());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getBlockDisplayName(String materialName) {
        ConfigurationSection displaySection = plugin.getConfig().getConfigurationSection("material-display");
        if (displaySection != null && displaySection.isSet(materialName)) {
            return ChatColor.translateAlternateColorCodes('&', displaySection.getString(materialName));
        }
        return materialName.replace("_", " ").toLowerCase();
    }

    private ItemStack buildUpgradeItem(String upgradeId, ConfigurationSection sec) {
        String matName = sec.getString("material", "STONE");
        XMaterial xMat = XMaterial.matchXMaterial(matName).orElse(XMaterial.STONE);
        ItemStack item = xMat.parseItem();
        if (item == null) item = new ItemStack(Objects.requireNonNull(XMaterial.STONE.parseMaterial()));

        int currentLevel = plugin.getUpgradeManager().getUpgradeLevel(islandOwnerUUID, upgradeId);
        int maxLevel = sec.getInt("max_level", 10);
        int costBase = sec.getInt("cost_base", 500);
        int costStep = sec.getInt("cost_step", 1000);
        double nextCost = currentLevel < maxLevel ? costBase + (long) costStep * currentLevel : -1;
        double balance = plugin.getEconomyHook().getBalance(player);

        String rawName = sec.getString("name", upgradeId);
        String name = ChatColor.translateAlternateColorCodes('&',
                rawName.replace("{level}", String.valueOf(currentLevel))
                        .replace("{max_level}", String.valueOf(maxLevel)));

        List<String> loreRaw = sec.getStringList("lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreRaw) {
            if (line.contains("{ores}")) {
                int targetLevel = Math.min(currentLevel + 1, maxLevel);
                String generatorId = "tier" + targetLevel;
                if (upgradeId.equalsIgnoreCase("tier")) {
                    Generator gen = plugin.getDataManager().getGenerators().get(generatorId);
                    if (gen != null) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Các quặng sẽ xuất hiện ở Tier " + targetLevel + ":"));
                        double total = gen.getTotalChance();
                        for (Map.Entry<String, Double> entry : gen.getRandom().entrySet()) {
                            double chance = (entry.getValue() / total) * 100;
                            String displayName = getBlockDisplayName(entry.getKey());
                            lore.add(ChatColor.translateAlternateColorCodes('&', " &8• &f" + displayName + ": &e" + PERCENT_FORMAT.format(chance) + "%"));
                        }
                        
                        // Add Realm Requirement info if applicable
                        if (gen.getRealmRequired() > 0 && Bukkit.getPluginManager().isPluginEnabled("TuTienCore")) {
                            Realm realm = TuTien.getApi().getRealmById(gen.getRealmRequired());
                            String realmName = (realm != null) ? realm.getFormattedName() : String.valueOf(gen.getRealmRequired());
                            lore.add("");
                            lore.add(ChatColor.translateAlternateColorCodes('&', "&fYêu cầu Cảnh giới: " + realmName));
                        }
                    }
                }
                continue;
            }

            String processed = line
                    .replace("{level}", String.valueOf(currentLevel))
                    .replace("{max_level}", String.valueOf(maxLevel))
                    .replace("{cost}", nextCost < 0 ? "MAX" : FORMAT.format(nextCost))
                    .replace("{balance}", FORMAT.format(balance));
            
            if (currentLevel >= maxLevel && line.contains("{cost}")) {
                processed = ChatColor.translateAlternateColorCodes('&', "&c✦ Đã đạt cấp tối đa!");
            }
            lore.add(ChatColor.translateAlternateColorCodes('&', processed));
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        int slot = event.getRawSlot();
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("upgrade-gui");
        if (guiSection == null) return;

        ConfigurationSection upgradesSection = guiSection.getConfigurationSection("upgrades");
        if (upgradesSection == null) return;

        for (String upgradeId : upgradesSection.getKeys(false)) {
            ConfigurationSection sec = upgradesSection.getConfigurationSection(upgradeId);
            if (sec == null) continue;
            if (sec.getInt("slot", -1) != slot) continue;

            handleUpgradeClick(upgradeId, sec, guiSection);
            return;
        }
    }

    private void handleUpgradeClick(String upgradeId, ConfigurationSection sec, ConfigurationSection guiSection) {
        int currentLevel = plugin.getUpgradeManager().getUpgradeLevel(islandOwnerUUID, upgradeId);
        int maxLevel = sec.getInt("max_level", 10);

        if (currentLevel >= maxLevel) {
            sendMsg("messages.upgrade.max-level");
            playSound(guiSection, false);
            return;
        }

        // Blocking based on Realm Requirement for Tier upgrades
        if (upgradeId.equalsIgnoreCase("tier") && Bukkit.getPluginManager().isPluginEnabled("TuTienCore")) {
            try {
                int playerRealm = TuTien.getApi().getRealmId(player.getUniqueId());
                int nextTierLevel = currentLevel + 1;
                Generator nextGen = plugin.getDataManager().getGenerators().get("tier" + nextTierLevel);
                
                if (nextGen != null && nextGen.getRealmRequired() > 0) {
                    if (playerRealm < nextGen.getRealmRequired()) {
                        Realm requiredRealm = TuTien.getApi().getRealmById(nextGen.getRealmRequired());
                        String realmName = (requiredRealm != null) ? requiredRealm.getFormattedName() : String.valueOf(nextGen.getRealmRequired());
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&cBạn cần đạt Cảnh giới " + realmName + " &cđể nâng cấp lên Tier này!"));
                        playSound(guiSection, false);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        int costBase = sec.getInt("cost_base", 500);
        int costStep = sec.getInt("cost_step", 1000);
        double cost = costBase + (long) costStep * currentLevel;

        if (!plugin.getEconomyHook().has(player, cost)) {
            double balance = plugin.getEconomyHook().getBalance(player);
            String msg = plugin.getConfig().getString("messages.upgrade.not-enough-money", "");
            msg = msg.replace("{cost}", FORMAT.format(cost))
                     .replace("{balance}", FORMAT.format(balance));
            player.sendMessage(plugin.getStringParser().getColoredPrefixString(msg, player));
            playSound(guiSection, false);
            return;
        }

        plugin.getEconomyHook().withdraw(player, cost);

        int newLevel = currentLevel + 1;
        plugin.getUpgradeManager().setUpgradeLevel(islandOwnerUUID, upgradeId, newLevel);

        List<String> commands = sec.getStringList("commands");
        for (String cmd : commands) {
            String finalCmd = cmd.replace("%player%", player.getName())
                                 .replace("%level%", String.valueOf(newLevel));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        String upgradeName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                sec.getString("name", upgradeId)
                   .replace("{level}", "").replace("{max_level}", "").trim()));
        String msg = plugin.getConfig().getString("messages.upgrade.success", "");
        msg = msg.replace("{upgrade}", upgradeName)
                 .replace("{level}", String.valueOf(newLevel))
                 .replace("{cost}", FORMAT.format(cost));
        player.sendMessage(plugin.getStringParser().getColoredPrefixString(msg, player));

        playSound(guiSection, true);

        Bukkit.getScheduler().runTask(plugin, this::refreshContents);
    }

    private void refreshContents() {
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("upgrade-gui");
        if (guiSection == null) return;
        ConfigurationSection upgradesSection = guiSection.getConfigurationSection("upgrades");
        if (upgradesSection == null) return;

        for (String key : upgradesSection.getKeys(false)) {
            ConfigurationSection sec = upgradesSection.getConfigurationSection(key);
            if (sec == null) continue;
            int slot = sec.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) continue;
            inventory.setItem(slot, buildUpgradeItem(key, sec));
        }
    }

    private void playSound(ConfigurationSection guiSection, boolean success) {
        ConfigurationSection soundSec = guiSection.getConfigurationSection("sound");
        if (soundSec == null) return;
        String soundName = success
                ? soundSec.getString("success", "ENTITY_PLAYER_LEVELUP")
                : soundSec.getString("fail", "ENTITY_VILLAGER_NO");
        float volume = (float) (success
                ? soundSec.getDouble("success-volume", 1.0)
                : soundSec.getDouble("fail-volume", 1.0));
        float pitch = (float) (success
                ? soundSec.getDouble("success-pitch", 1.0)
                : soundSec.getDouble("fail-pitch", 1.0));

        XSound.of(soundName).ifPresent(xs -> {
            org.bukkit.Sound s = xs.get();
            if (s != null) player.playSound(player.getLocation(), s, volume, pitch);
        });
    }

    private void sendMsg(String path) {
        String msg = plugin.getConfig().getString(path, "");
        player.sendMessage(plugin.getStringParser().getColoredPrefixString(msg, player));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
