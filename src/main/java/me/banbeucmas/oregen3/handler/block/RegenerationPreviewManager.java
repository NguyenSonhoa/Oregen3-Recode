package me.banbeucmas.oregen3.handler.block;

import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class RegenerationPreviewManager {
    private final Oregen3 plugin;
    private final Map<Location, BlockDisplay> displays = new HashMap<>();
    private final Map<Location, BukkitTask> tasks = new HashMap<>();

    public RegenerationPreviewManager(Oregen3 plugin) {
        this.plugin = plugin;
    }

    public void show(Block block, BlockPlacer placer, int durationTicks) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> show(block, placer, durationTicks));
            return;
        }

        if (!plugin.getConfig().getBoolean("global.generators.regeneration-preview.enabled", true)
                || durationTicks <= 0) {
            return;
        }

        BlockData blockData = placer.getDisplayBlockData();
        if (blockData == null) {
            return;
        }

        Location key = getKey(block.getLocation());
        removeNow(key);

        World world = block.getWorld();
        Location spawnLocation = key.clone();
        BlockDisplay display = world.spawn(spawnLocation, BlockDisplay.class);
        display.setBlock(blockData);
        display.setGravity(false);
        display.setInvulnerable(true);
        display.setPersistent(false);
        display.setSilent(true);
        display.setTransformation(createTransformation(getMinScale()));

        int brightness = plugin.getConfig().getInt("global.generators.regeneration-preview.brightness", 15);
        if (brightness >= 0) {
            int clampedBrightness = Math.max(0, Math.min(15, brightness));
            display.setBrightness(new Display.Brightness(clampedBrightness, clampedBrightness));
        }

        displays.put(key, display);
        animate(key, display, durationTicks);
    }

    public void remove(Location location) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> remove(location));
            return;
        }

        removeNow(getKey(location));
    }

    public void clear() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::clear);
            return;
        }

        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();

        for (BlockDisplay display : displays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
    }

    private void animate(Location key, BlockDisplay display, int durationTicks) {
        int tickInterval = Math.max(1, plugin.getConfig().getInt("global.generators.regeneration-preview.tick-interval", 1));
        int animationTicks = Math.max(1, durationTicks);
        float minScale = getMinScale();
        float maxScale = getMaxScale(minScale);

        BukkitTask task = new BukkitRunnable() {
            private int elapsedTicks;

            @Override
            public void run() {
                if (!display.isValid()) {
                    tasks.remove(key);
                    displays.remove(key);
                    cancel();
                    return;
                }

                elapsedTicks = Math.min(animationTicks, elapsedTicks + tickInterval);
                float progress = elapsedTicks / (float) animationTicks;
                float scale = minScale + ((maxScale - minScale) * progress);
                display.setTransformation(createTransformation(scale));

                if (elapsedTicks >= animationTicks) {
                    tasks.remove(key);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, tickInterval);

        tasks.put(key, task);
    }

    private void removeNow(Location key) {
        BukkitTask task = tasks.remove(key);
        if (task != null) {
            task.cancel();
        }

        BlockDisplay display = displays.remove(key);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private float getMinScale() {
        return clampScale((float) plugin.getConfig().getDouble("global.generators.regeneration-preview.min-scale", 0.05D));
    }

    private float getMaxScale(float minScale) {
        float maxScale = clampScale((float) plugin.getConfig().getDouble("global.generators.regeneration-preview.max-scale", 1.0D));
        return Math.max(minScale, maxScale);
    }

    private float clampScale(float scale) {
        return Math.max(0.01F, Math.min(4.0F, scale));
    }

    private Transformation createTransformation(float scale) {
        float offset = (1.0F - scale) / 2.0F;
        return new Transformation(
                new Vector3f(offset, offset, offset),
                new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)
        );
    }

    private Location getKey(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
