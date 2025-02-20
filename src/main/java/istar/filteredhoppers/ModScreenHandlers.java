package istar.filteredhoppers;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModScreenHandlers {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModScreenHandlers");

    public static final ScreenHandlerType<AdvancedHopperScreenHandler> ADVANCED_HOPPER_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerExtended(
                    new Identifier(IStarsFilteredHoppers.MOD_ID, "advanced_hopper"),
                    (syncId, playerInventory, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        World world = playerInventory.player.getWorld();
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        if (blockEntity instanceof AdvancedHopperBlockEntity) {
                            LOGGER.info("Created screen handler for advanced hopper at {}.", pos);
                            return new AdvancedHopperScreenHandler(syncId, playerInventory, (AdvancedHopperBlockEntity) blockEntity);
                        } else {
                            LOGGER.error("Invalid block entity at {}. Expected AdvancedHopperBlockEntity, found {}.", pos, blockEntity != null ? blockEntity.getClass().getName() : "null");
                            return null;
                        }
                    });

    public static void register() {
        LOGGER.info("Advanced Hopper Screen Handler Registered!");
    }
}
