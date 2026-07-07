package com.aw2towns.block;

import com.aw2towns.economy.TownSavedData;
import com.aw2towns.screen.TownManagerScreenHandler;
import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TownManagerBlock extends Block {

    public static final MapCodec<TownManagerBlock> CODEC = AbstractBlock.createCodec(TownManagerBlock::new);

    public TownManagerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer && world instanceof ServerWorld serverWorld) {
            TownSavedData.get(serverWorld).ensureStarterTown(serverWorld.getTime());
            MenuRegistry.openExtendedMenu(serverPlayer, new TownManagerProvider(pos));
        }
        return ActionResult.SUCCESS;
    }

    private record TownManagerProvider(BlockPos pos) implements ExtendedMenuProvider {
        @Override
        public Text getDisplayName() {
            return Text.translatable("container.aw2towns.town_manager");
        }

        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new TownManagerScreenHandler(syncId, playerInventory, pos);
        }

        @Override
        public void saveExtraData(PacketByteBuf buf) {
            buf.writeBlockPos(pos);
        }
    }
}
