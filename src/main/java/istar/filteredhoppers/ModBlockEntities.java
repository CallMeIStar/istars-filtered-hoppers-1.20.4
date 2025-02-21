package istar.filteredhoppers;

import istar.filteredhoppers.AdvancedHopperBlockEntity;
import istar.filteredhoppers.IStarsFilteredHoppers;
import istar.filteredhoppers.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModBlockEntities");

    public static BlockEntityType<AdvancedHopperBlockEntity> ADVANCED_HOPPER_BLOCK_ENTITY;

    public static void register() {
        LOGGER.info("Starting block entity registration...");

        try {
            ADVANCED_HOPPER_BLOCK_ENTITY = Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    new Identifier(IStarsFilteredHoppers.MOD_ID, "advanced_hopper"),
                    FabricBlockEntityTypeBuilder.create(
                            AdvancedHopperBlockEntity::new,
                            ModBlocks.ADVANCED_HOPPER
                    ).build()
            );

            LOGGER.info("✅ Successfully registered AdvancedHopperBlockEntity");
            LOGGER.debug("Registered Type: {}", ADVANCED_HOPPER_BLOCK_ENTITY);
        } catch (Exception e) {
            LOGGER.error("❌ Failed to register AdvancedHopperBlockEntity", e);
        }
        LOGGER.info("🔶 BlockEntityType Instance: {}", System.identityHashCode(ADVANCED_HOPPER_BLOCK_ENTITY));
    }
}