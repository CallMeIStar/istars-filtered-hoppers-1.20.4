package istar.filteredhoppers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedHopperBlock extends HopperBlock implements InventoryProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("AdvancedHopperBlock-Ticker");

    public AdvancedHopperBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        IStarsFilteredHoppers.LOGGER.info("AdvancedHopperBlockEntity created at {}", pos); // Add this line
        return new AdvancedHopperBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AdvancedHopperBlockEntity) {
                NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
                if (screenHandlerFactory != null) {
                    LOGGER.info("Player {} opened advanced hopper at {}.", player.getName().getString(), pos);
                    player.openHandledScreen(screenHandlerFactory);
                }
            } else {
                LOGGER.error("Block entity at {} is not an AdvancedHopperBlockEntity.", pos);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SidedInventory) {
            return (SidedInventory) blockEntity;
        }
        return null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModBlockEntities.ADVANCED_HOPPER_BLOCK_ENTITY, AdvancedHopperBlockEntity::tick);
    }
}