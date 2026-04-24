package me.banbeucmas.oregen3.handler.block.placetask;

import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.data.Generator;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LimitedBlockPlaceTask extends BlockPlaceTask {
    private Queue<QueuedBlockPlace> tasks;
    private BukkitTask task;
    private long maxBlockPlacePerTick;

    public LimitedBlockPlaceTask(Oregen3 plugin) {
        super(plugin);
        if (plugin.getBlockEventHandler().isAsync()) {
            tasks = new ConcurrentLinkedQueue<>();
        } else {
            tasks = new ArrayDeque<>();
        }
        maxBlockPlacePerTick = plugin.getConfig().getLong("global.generators.maxBlockPlacePerTick", Integer.MAX_VALUE);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long blockPlaced = 0;
            while (!tasks.isEmpty() && blockPlaced < maxBlockPlacePerTick) {
                QueuedBlockPlace queuedBlockPlace = tasks.poll();
                if (place(queuedBlockPlace.block, queuedBlockPlace.placer, queuedBlockPlace.generator)) blockPlaced++;
            }
        }, 0, 1);
    }

    @Override
    public void stop() {
        task.cancel();
        while (!tasks.isEmpty()) {
            QueuedBlockPlace queuedBlockPlace = tasks.poll();
            place(queuedBlockPlace.block, queuedBlockPlace.placer, queuedBlockPlace.generator);
        }
    }

    @Override
    public void placeBlock(Block block, BlockPlacer placer, Generator generator) {
        tasks.add(new QueuedBlockPlace(block, placer, generator));
    }

    public static class QueuedBlockPlace {
        public Block block;
        public BlockPlacer placer;
        public Generator generator;

        public QueuedBlockPlace(Block block, BlockPlacer placer, Generator generator) {
            this.block = block;
            this.placer = placer;
            this.generator = generator;
        }
    }
}
