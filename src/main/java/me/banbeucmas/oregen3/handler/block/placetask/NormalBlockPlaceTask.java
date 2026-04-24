package me.banbeucmas.oregen3.handler.block.placetask;

import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.data.Generator;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public class NormalBlockPlaceTask extends BlockPlaceTask {
    private BlockPlaceTask placeTask;

    public NormalBlockPlaceTask(Oregen3 plugin) {
        super(plugin);
        if (plugin.getBlockEventHandler().isAsync()) {
            placeTask = new SyncBlockPlaceTask(plugin);
        }
        else {
            placeTask = new DefaultBlockPlaceTask(plugin);
        }
    }

    @Override
    public void placeBlock(Block block, BlockPlacer placer, Generator generator) {
        placeTask.placeBlock(block, placer, generator);
    }

    private class SyncBlockPlaceTask extends BlockPlaceTask {
        public SyncBlockPlaceTask(Oregen3 plugin) {
            super(plugin);
        }
        @Override
        public void placeBlock(Block block, BlockPlacer blockPlacer, Generator generator) {
            Bukkit.getScheduler().runTask(plugin, () -> place(block, blockPlacer, generator));
        }
    }

    private class DefaultBlockPlaceTask extends BlockPlaceTask {
        public DefaultBlockPlaceTask(Oregen3 plugin) {
            super(plugin);
        }
        @Override
        public void placeBlock(Block block, BlockPlacer blockPlacer, Generator generator) {
            place(block, blockPlacer, generator);
        }
    }
}
