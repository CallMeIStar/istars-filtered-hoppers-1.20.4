package istar.filteredhoppers;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModBlocks {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModBlocks");
    public static final Block ADVANCED_HOPPER =
            new AdvancedHopperBlock(FabricBlockSettings.copyOf(Blocks.HOPPER));

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier(IStarsFilteredHoppers.MOD_ID, "advanced_hopper"), ADVANCED_HOPPER);
        Registry.register(Registries.ITEM, new Identifier(IStarsFilteredHoppers.MOD_ID, "advanced_hopper"), new BlockItem(ADVANCED_HOPPER, new Item.Settings()));
        LOGGER.info("🔷 Block Instance: {}", System.identityHashCode(ADVANCED_HOPPER));
    }
}
