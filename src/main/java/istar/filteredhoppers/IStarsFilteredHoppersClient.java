package istar.filteredhoppers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class IStarsFilteredHoppersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModScreens.register();
    }
}
