package istar.filteredhoppers;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<AdvancedHopperBlockEntity> ADVANCED_HOPPER_BLOCK_ENTITY;

    public static void registerBlockEntities() {
        // Register the block entity type dynamically
        ADVANCED_HOPPER_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(IStarsFilteredHoppers.MOD_ID, "advanced_hopper"),
                BlockEntityType.Builder.create(
                        AdvancedHopperBlockEntity::new, // Constructor reference
                        ModBlocks.ADVANCED_HOPPER // MUST match your AdvancedHopperBlock instance
                ).build()
        );
    }
}