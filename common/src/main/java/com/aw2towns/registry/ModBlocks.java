package com.aw2towns.registry;

import com.aw2towns.AW2Towns;
import com.aw2towns.block.TownManagerBlock;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {

    public static final RegistrySupplier<Block> TOWN_MANAGER = AW2Towns.blocks().register(
            AW2Towns.id("town_manager"),
            () -> new TownManagerBlock(
                    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
                            .mapColor(MapColor.OAK_TAN)
                            .strength(2.5F, 6.0F)
                            .sounds(BlockSoundGroup.WOOD))
    );

    private ModBlocks() {}

    public static void init() {}
}
