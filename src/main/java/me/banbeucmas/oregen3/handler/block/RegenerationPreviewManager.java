package me.banbeucmas.oregen3.handler.block;

import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RegenerationPreviewManager {
    private final Oregen3 plugin;
    private final Map<Location, BlockDisplay> displays = new HashMap<>();
    private final Map<Location, TextDisplay> textDisplays = new HashMap<>();
    private final Map<Location, Set<UUID>> textViewers = new HashMap<>();
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

        Location key = getKey(block.getLocation());
        removeNow(key);

        World world = block.getWorld();
        BlockDisplay display = spawnBlockDisplay(world, key, placer);
        TextDisplay textDisplay = spawnTextDisplay(world, key, durationTicks);
        if (display == null && textDisplay == null) {
            return;
        }

        if (display != null) {
            displays.put(key, display);
        }
        if (textDisplay != null) {
            textDisplays.put(key, textDisplay);
        }
        animate(key, display, textDisplay, durationTicks);
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

        for (Map.Entry<Location, TextDisplay> entry : textDisplays.entrySet()) {
            removeTextDisplay(entry.getKey(), entry.getValue());
        }
        textDisplays.clear();
        textViewers.clear();
    }

    private BlockDisplay spawnBlockDisplay(World world, Location key, BlockPlacer placer) {
        BlockData blockData = placer.getDisplayBlockData();
        if (blockData == null) {
            return null;
        }

        BlockDisplay display = world.spawn(key.clone(), BlockDisplay.class);
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
        return display;
    }

    private TextDisplay spawnTextDisplay(World world, Location key, int durationTicks) {
        if (!plugin.getConfig().getBoolean("global.generators.regeneration-preview.countdown.enabled", true)) {
            return null;
        }

        Location textLocation = getTextLocation(key, 0.0F);
        TextDisplay textDisplay = world.spawn(textLocation, TextDisplay.class);
        textDisplay.setGravity(false);
        textDisplay.setInvulnerable(true);
        textDisplay.setPersistent(false);
        textDisplay.setSilent(true);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setShadowed(true);
        textDisplay.setDefaultBackground(false);
        textDisplay.setSeeThrough(plugin.getConfig().getBoolean("global.generators.regeneration-preview.countdown.see-through", true));
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setInterpolationDuration(getCountdownInterpolationDuration());
        textDisplay.setTeleportDuration(getCountdownTeleportDuration());
        textDisplay.setTransformation(createTextTransformation());
        textDisplay.setTextOpacity((byte) 0);
        updateCountdownText(textDisplay, durationTicks);
        hideTextFromAll(textDisplay);
        return textDisplay;
    }

    private void animate(Location key, BlockDisplay display, TextDisplay textDisplay, int durationTicks) {
        int tickInterval = Math.max(1, plugin.getConfig().getInt("global.generators.regeneration-preview.tick-interval", 1));
        int animationTicks = Math.max(1, durationTicks);
        float minScale = getMinScale();
        float maxScale = getMaxScale(minScale);

        BukkitTask task = new BukkitRunnable() {
            private int elapsedTicks;

            @Override
            public void run() {
                if ((display == null || !display.isValid()) && (textDisplay == null || !textDisplay.isValid())) {
                    tasks.remove(key);
                    displays.remove(key);
                    textDisplays.remove(key);
                    textViewers.remove(key);
                    cancel();
                    return;
                }

                elapsedTicks = Math.min(animationTicks, elapsedTicks + tickInterval);
                float progress = elapsedTicks / (float) animationTicks;
                if (display != null && display.isValid()) {
                    float scale = minScale + ((maxScale - minScale) * progress);
                    display.setTransformation(createTransformation(scale));
                }
                if (textDisplay != null && textDisplay.isValid()) {
                    updateTextFade(textDisplay, key, elapsedTicks);
                    updateCountdownText(textDisplay, animationTicks - elapsedTicks);
                    updateTextVisibility(key, textDisplay);
                }

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

        TextDisplay textDisplay = textDisplays.remove(key);
        if (textDisplay != null) {
            removeTextDisplay(key, textDisplay);
        }
    }

    private float getMinScale() {
        return clampScale((float) plugin.getConfig().getDouble("global.generators.regeneration-preview.min-scale", 0.05D));
    }

    private float getMaxScale(float minScale) {
        float maxScale = clampScale((float) plugin.getConfig().getDouble("global.generators.regeneration-preview.max-scale", 1.0D));
        return Math.max(minScale, maxScale);
    }

    private void updateCountdownText(TextDisplay textDisplay, int remainingTicks) {
        String template = plugin.getConfig().getString("global.generators.regeneration-preview.countdown.text", "&e{seconds}s");
        textDisplay.setText(ChatColor.translateAlternateColorCodes('&',
                template.replace("{seconds}", formatSeconds(remainingTicks / 20.0D))));
    }

    private void updateTextFade(TextDisplay textDisplay, Location key, int elapsedTicks) {
        int fadeDuration = Math.max(1, plugin.getConfig().getInt("global.generators.regeneration-preview.countdown.fade-duration", 10));
        float fadeProgress = Math.min(1.0F, elapsedTicks / (float) fadeDuration);
        textDisplay.teleport(getTextLocation(key, fadeProgress));
        textDisplay.setTextOpacity((byte) Math.round(255.0F * fadeProgress));
    }

    private String formatSeconds(double seconds) {
        String pattern = plugin.getConfig().getString("global.generators.regeneration-preview.countdown.decimal-format", "0.0");
        try {
            return new DecimalFormat(pattern).format(Math.max(0.0D, seconds));
        } catch (IllegalArgumentException ignored) {
            return new DecimalFormat("0.0").format(Math.max(0.0D, seconds));
        }
    }

    private void updateTextVisibility(Location key, TextDisplay textDisplay) {
        Set<UUID> viewers = textViewers.computeIfAbsent(key, unused -> new HashSet<>());
        Set<UUID> lookingPlayers = new HashSet<>();
        double targetRange = plugin.getConfig().getDouble("global.generators.regeneration-preview.countdown.target-range", 6.0D);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isLookingAtBlock(player, key, targetRange)) {
                lookingPlayers.add(player.getUniqueId());
                if (viewers.add(player.getUniqueId())) {
                    player.showEntity(plugin, textDisplay);
                }
            }
        }

        for (UUID viewer : new HashSet<>(viewers)) {
            if (lookingPlayers.contains(viewer)) {
                continue;
            }
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) {
                player.hideEntity(plugin, textDisplay);
            }
            viewers.remove(viewer);
        }
    }

    private boolean isLookingAtBlock(Player player, Location key, double targetRange) {
        if (!player.getWorld().equals(key.getWorld())) {
            return false;
        }

        Location eyeLocation = player.getEyeLocation();
        Vector origin = eyeLocation.toVector();
        Vector direction = eyeLocation.getDirection().normalize();
        double intersection = rayTraceBlock(origin, direction, key);
        return intersection >= 0.0D && intersection <= targetRange;
    }

    private double rayTraceBlock(Vector origin, Vector direction, Location key) {
        double minX = key.getBlockX();
        double minY = key.getBlockY();
        double minZ = key.getBlockZ();
        double maxX = minX + 1.0D;
        double maxY = minY + 1.0D;
        double maxZ = minZ + 1.0D;

        double[] result = updateRayRange(origin.getX(), direction.getX(), minX, maxX, 0.0D, Double.MAX_VALUE);
        if (result == null) return -1.0D;
        result = updateRayRange(origin.getY(), direction.getY(), minY, maxY, result[0], result[1]);
        if (result == null) return -1.0D;
        result = updateRayRange(origin.getZ(), direction.getZ(), minZ, maxZ, result[0], result[1]);
        if (result == null) return -1.0D;
        return result[0];
    }

    private double[] updateRayRange(double origin, double direction, double min, double max, double currentMin, double currentMax) {
        if (Math.abs(direction) < 1.0E-7D) {
            return origin >= min && origin <= max ? new double[] {currentMin, currentMax} : null;
        }

        double first = (min - origin) / direction;
        double second = (max - origin) / direction;
        if (first > second) {
            double temp = first;
            first = second;
            second = temp;
        }

        double nextMin = Math.max(currentMin, first);
        double nextMax = Math.min(currentMax, second);
        return nextMax >= nextMin ? new double[] {nextMin, nextMax} : null;
    }

    private void hideTextFromAll(TextDisplay textDisplay) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideEntity(plugin, textDisplay);
        }
    }

    private void removeTextDisplay(Location key, TextDisplay textDisplay) {
        Set<UUID> viewers = textViewers.remove(key);
        if (viewers != null) {
            for (UUID viewer : viewers) {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) {
                    player.hideEntity(plugin, textDisplay);
                }
            }
        }

        if (textDisplay.isValid()) {
            textDisplay.remove();
        }
    }

    private float clampScale(float scale) {
        return Math.max(0.01F, Math.min(4.0F, scale));
    }

    private int getCountdownInterpolationDuration() {
        return Math.max(0, Math.min(59,
                plugin.getConfig().getInt("global.generators.regeneration-preview.countdown.interpolation-duration", 1)));
    }

    private int getCountdownTeleportDuration() {
        return Math.max(0, Math.min(59,
                plugin.getConfig().getInt("global.generators.regeneration-preview.countdown.teleport-duration", 1)));
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

    private Transformation createTextTransformation() {
        float scale = Math.max(0.1F, Math.min(4.0F,
                (float) plugin.getConfig().getDouble("global.generators.regeneration-preview.countdown.scale", 0.6D)));
        return new Transformation(
                new Vector3f(),
                new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)
        );
    }

    private Location getTextLocation(Location key, float fadeProgress) {
        double height = plugin.getConfig().getDouble("global.generators.regeneration-preview.countdown.height", 1.15D);
        double riseDistance = Math.max(0.0D,
                plugin.getConfig().getDouble("global.generators.regeneration-preview.countdown.rise-distance", 0.35D));
        double yOffset = height - (riseDistance * (1.0D - fadeProgress));
        return key.clone().add(0.5D, yOffset, 0.5D);
    }

    private Location getKey(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
