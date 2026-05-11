package me.banbeucmas.oregen3.listener;

import com.cryptomorin.xseries.XBlock;
import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.util.BlockChecker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

public class BlockListener implements Listener {
    private Oregen3 plugin;
    private final FileConfiguration config;

    public BlockListener(final Oregen3 plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    private boolean canGenerateBlock(Block src,
                                     Block to,
                                     boolean waterBlock,
                                     boolean lavaBlock) {
        final Material material = src.getType();
        for (final BlockFace face : BlockChecker.FACES) {
            final Block check = to.getRelative(face);
            if (plugin.getBlockChecker().isBlock(check)
                    && (XBlock.isWater(material))
                    && waterBlock) {
                return true;
            }
            else if (plugin.getBlockChecker().isBlock(check)
                    && (XBlock.isLava(material))
                    && lavaBlock) {
                return true;
            }
        }
        return false;
    }

    /*
    Checks for Water + Lava, block will use another method to prevent confusion
     */
    private boolean canGenerate(final Material material, final Block b) {
        final boolean check = XBlock.isWater(material);
        for (final BlockFace face : BlockChecker.FACES) {
            final Material type = b.getRelative(face).getType();
            if (((check && XBlock.isLava(type)) || (!check && XBlock.isWater(type)))) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onOre(final BlockFromToEvent event) {
        final Block source = event.getBlock();
        final Block to = event.getToBlock();
        final Material sourceMaterial = source.getType();
        final Material toMaterial = to.getType();

        if (XBlock.isAir(sourceMaterial))
            return;

        if (config.getBoolean("global.generators.world.enabled", false)
                && config.getBoolean("global.generators.world.blacklist", true)
                == config.getStringList("global.generators.world.list").contains(to.getWorld().getName())) {
            return;
        }

        if (XBlock.isWater(sourceMaterial) || XBlock.isLava(sourceMaterial)) {
            // New logic: Check for liquid flowing onto a generator block (e.g., water onto a fence)
            if (plugin.getBlockChecker().isBlock(to)) { // If 'to' is a fence/generator block
                boolean hasOtherLiquidAdjacentToSource = false;
                for (final BlockFace face : BlockChecker.FACES) {
                    if (face == BlockFace.SELF) continue; // Skip self, we need an adjacent block
                    Block adjacentToSource = source.getRelative(face);
                    if ((XBlock.isWater(sourceMaterial) && XBlock.isLava(adjacentToSource.getType())) ||
                        (XBlock.isLava(sourceMaterial) && XBlock.isWater(adjacentToSource.getType()))) {
                        hasOtherLiquidAdjacentToSource = true;
                        break;
                    }
                }
                if (hasOtherLiquidAdjacentToSource) {
                    event.setCancelled(true);
                    int delay = XBlock.isWater(sourceMaterial) 
                        ? config.getInt("global.generators.check-regen.mode.waterBlock", 0)
                        : config.getInt("global.generators.check-regen.mode.lavaBlock", 0);
                    generateWithDelay(to, delay);
                    return;
                }
            }

            // Existing logic for waterLava and waterBlock/lavaBlock modes
            if ((XBlock.isAir(toMaterial) || XBlock.isWater(toMaterial))
                    && XBlock.isWaterStationary(source)
                    && config.getBoolean("mode.waterLava")
                    && canGenerate(sourceMaterial, to)
                    && event.getFace() != BlockFace.DOWN) {
                if (XBlock.isLava(sourceMaterial) && !BlockChecker.isSurroundedByWater(to.getLocation())) {
                    return;
                }
                event.setCancelled(true);
                int delay = config.getInt("global.generators.check-regen.mode.waterLava", 0);
                generateWithDelay(to, delay);
            }
            else if (canGenerateBlock(source,
                    to,
                    config.getBoolean("mode.waterBlock"),
                    config.getBoolean("mode.lavaBlock"))) {
                event.setCancelled(true);
                int delay = XBlock.isWater(sourceMaterial) 
                        ? config.getInt("global.generators.check-regen.mode.waterBlock", 0)
                        : config.getInt("global.generators.check-regen.mode.lavaBlock", 0);
                generateWithDelay(to, delay);
            }
        }
    }

    private void generateWithDelay(Block block, int delay) {
        int finalDelay = delay;
        if (finalDelay > 0) {
            OfflinePlayer owner = plugin.getUtils().getOwner(block.getLocation());
            if (owner != null) {
                double level = plugin.getUpgradeManager().getUpgradeLevel(owner.getUniqueId(), "linhmach");
                if (level > 0) {
                    double percentPerLevel = config.getDouble("upgrade-gui.upgrades.linhmach.percent_per_level", 5.0);
                    double speedFactor = 1.0 + (percentPerLevel * level / 100.0);
                    finalDelay = (int) Math.round(delay / speedFactor);
                }
            }
        }

        if (finalDelay <= 0) {
            plugin.getBlockEventHandler().generateBlock(block);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getBlockEventHandler().generateBlock(block), finalDelay);
        }
    }
}
