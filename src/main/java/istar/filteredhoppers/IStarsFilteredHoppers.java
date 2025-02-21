package istar.filteredhoppers;


import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IStarsFilteredHoppers implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("istars_filtered_hoppers");
	public static final String MOD_ID = "istars_filtered_hoppers";

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModBlockEntities.register();
		ModScreenHandlers.register();
		LOGGER.info("IStars Filtered Hoppers Mod Initialized!");
	}
}