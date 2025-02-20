// AdvancedHopperScreen.java
package istar.filteredhoppers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class AdvancedHopperScreen extends HandledScreen<AdvancedHopperScreenHandler> {
    private static final Logger LOGGER = LoggerFactory.getLogger("istars_filtered_hoppers_screen");
    private static final Identifier TEXTURE = new Identifier("istars-filtered-hoppers", "textures/gui/container/advanced_hopper.png");
    private final PlayerInventory playerInventory;

    public AdvancedHopperScreen(AdvancedHopperScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.playerInventory = inventory;
        LOGGER.info("AdvancedHopperScreen constructed. Texture: {}", TEXTURE); // Log texture identifier
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("AdvancedHopperScreen initialized. x: {}, y: {}, backgroundWidth: {}, backgroundHeight: {}", x, y, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        try {
            RenderSystem.setShaderTexture(0, TEXTURE);
            LOGGER.debug("Attempting to bind texture: {}", TEXTURE);
            context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
            LOGGER.debug("Texture drawn successfully.");
        } catch (Exception e) {
            LOGGER.error("Error loading texture {}: {}", TEXTURE, e.getMessage(), e);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title, 8, 6, 0x404040, false);
        context.drawText(textRenderer, this.playerInventory.getDisplayName(), 8, this.backgroundHeight - 96 + 2, 0x404040, false);
    }
}