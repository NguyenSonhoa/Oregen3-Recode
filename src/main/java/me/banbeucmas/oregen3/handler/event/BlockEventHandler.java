package me.banbeucmas.oregen3.handler.event;

import com.cryptomorin.xseries.XSound;
import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.data.Generator;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class BlockEventHandler {
    Oregen3 plugin;
    private final Map<Location, PendingRegeneration> pendingRegenerations = new HashMap<>();

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

    public void clearPendingRegenerations() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::clearPendingRegenerations);
            return;
        }

        for (PendingRegeneration pending : new ArrayList<>(pendingRegenerations.values())) {
            if (pending.task != null) {
                pending.task.cancel();
            }
            plugin.getRegenerationPreviewManager().remove(pending.block.getLocation());
            placeGeneratedBlock(pending.block, pending.placer, pending.generator);
        }
        pendingRegenerations.clear();
    }

    void generate(final Block block) {
        final Generator mc = plugin.getUtils().getChosenGenerator(block.getLocation());
        if (mc == null) return;
        BlockPlacer placer = mc.randomChance();
        if (placer == null) return;
        placeGeneratedBlock(block, placer, mc);
    }

    private void startDelayedGeneration(final Block block, final int delay) {
        final Location key = getBlockKey(block.getLocation());
        if (pendingRegenerations.containsKey(key)) {
            return;
        }

        final Generator mc = plugin.getUtils().getChosenGenerator(block.getLocation());
        if (mc == null) return;
        final BlockPlacer placer = mc.randomChance();
        if (placer == null) return;

        plugin.getRegenerationPreviewManager().show(block, placer, delay);
        PendingRegeneration pending = new PendingRegeneration(block, placer, mc);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingRegeneration active = pendingRegenerations.remove(key);
            if (active == null) {
                return;
            }
            plugin.getRegenerationPreviewManager().remove(active.block.getLocation());
            placeGeneratedBlock(active.block, active.placer, active.generator);
        }, delay);
        pending.task = task;
        pendingRegenerations.put(key, pending);
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

    private Location getBlockKey(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static class PendingRegeneration {
        private final Block block;
        private final BlockPlacer placer;
        private final Generator generator;
        private BukkitTask task;

        private PendingRegeneration(Block block, BlockPlacer placer, Generator generator) {
            this.block = block;
            this.placer = placer;
            this.generator = generator;
        }
    }
}
