package istar.filteredhoppers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class ModScreens {
    public static void register() {
        try {
            HandledScreens.register(ModScreenHandlers.ADVANCED_HOPPER_SCREEN_HANDLER, AdvancedHopperScreen::new);
            IStarsFilteredHoppers.LOGGER.info("Advanced Hopper Screen Registered!");
        } catch (Exception e) {
            IStarsFilteredHoppers.LOGGER.error("Error registering screen: {}", e.getMessage(), e); // Log any exceptions
        }
    }
}