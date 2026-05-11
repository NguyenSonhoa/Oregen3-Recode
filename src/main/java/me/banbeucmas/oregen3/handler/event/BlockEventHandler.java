package me.banbeucmas.oregen3.handler.event;

import com.cryptomorin.xseries.XSound;
import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.data.Generator;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

public abstract class BlockEventHandler {
    Oregen3 plugin;

    public BlockEventHandler(Oregen3 plugin) {
        this.plugin = plugin;
    }

    public abstract void generateBlock(final Block block);

    public void regenerateBlock(final Block block, final int delay) {
        if (delay <= 0) {
            generateBlock(block);
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            startDelayedGeneration(block, delay);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> startDelayedGeneration(block, delay));
        }
    }

    void generate(final Block block) {
        final Generator mc = plugin.getUtils().getChosenGenerator(block.getLocation());
        if (mc == null) return;
        BlockPlacer placer = mc.randomChance();
        if (placer == null) return;
        placeGeneratedBlock(block, placer, mc);
    }

    private void startDelayedGeneration(final Block block, final int delay) {
        final Generator mc = plugin.getUtils().getChosenGenerator(block.getLocation());
        if (mc == null) return;
        final BlockPlacer placer = mc.randomChance();
        if (placer == null) return;

        plugin.getRegenerationPreviewManager().show(block, placer, delay);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getRegenerationPreviewManager().remove(block.getLocation());
            placeGeneratedBlock(block, placer, mc);
        }, delay);
    }

    private void placeGeneratedBlock(final Block block, final BlockPlacer placer, final Generator mc) {
        plugin.getBlockPlaceTask().placeBlock(block, placer, mc);
        sendBlockEffect(block, mc);
    }

    private void sendBlockEffect(final Block to, final Generator mc) {
        World world = to.getWorld();
        if (mc.isSoundEnabled())
            world.playSound(to.getLocation(), mc.getSound(), mc.getSoundVolume(), mc.getSoundPitch());
        else if (plugin.getConfig().getBoolean("global.generators.sound.enabled", false)) {
            world.playSound(to.getLocation(),
                    Objects.requireNonNull(XSound.of(plugin.getConfig().getString("global.generators.sound.name", "BLOCK_FIRE_EXTINGUISH")).map(XSound::get).orElse(XSound.BLOCK_FIRE_EXTINGUISH.get())),
                    (float) plugin.getConfig().getDouble("global.generators.sound.volume", 1),
                    (float) plugin.getConfig().getDouble("global.generators.sound.pitch", 1)
            );
        }
    }

    public abstract boolean isAsync();
}
