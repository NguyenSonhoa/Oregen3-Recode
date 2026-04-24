package me.banbeucmas.oregen3.handler.block.placetask;

import me.banbeucmas.oregen3.Oregen3;
import me.banbeucmas.oregen3.data.Generator;
import me.banbeucmas.oregen3.handler.block.placer.BlockPlacer;
import org.bukkit.block.Block;

public abstract class BlockPlaceTask {
    public boolean preventOverrideBlocks;
    protected Oregen3 plugin;

    public BlockPlaceTask(Oregen3 plugin) {
        this.plugin = plugin;
        preventOverrideBlocks = plugin.getConfig().getBoolean("preventOverrideBlocks", false);
    }

    /* The actual placing */
    boolean place(Block block, BlockPlacer placer, Generator generator) {
        if (preventOverrideBlocks && !block.isEmpty()) return false;
        placer.placeBlock(block);
        if (generator != null) {
            plugin.getDataManager().getGeneratedBlocks().put(block.getLocation(), generator.getId());
        }
        return true;
    }

    public abstract void placeBlock(Block block, BlockPlacer blockPlacer, Generator generator);

    public void stop() {}
}
