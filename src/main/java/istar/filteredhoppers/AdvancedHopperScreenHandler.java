package istar.filteredhoppers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedHopperScreenHandler extends ScreenHandler {
    private final AdvancedHopperBlockEntity inventory;
    private static final Logger LOGGER = LoggerFactory.getLogger("istars_filtered_hoppers_screen_handler");

    public AdvancedHopperScreenHandler(int syncId, PlayerInventory playerInventory, AdvancedHopperBlockEntity inventory) {
        super(ModScreenHandlers.ADVANCED_HOPPER_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        for (int i = 0; i < 5; i++) {
            this.addSlot(new Slot(inventory, i, 44 + i * 18, 20) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return ((AdvancedHopperBlockEntity) this.inventory).canAcceptItem(stack); // Cast to AdvancedHopperBlockEntity
                }
            });
        }

        // Special filter slot
        this.addSlot(new Slot(inventory, 5, 133, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.WOODEN_SWORD);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }
    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Handle filter slot specially
            if (index == 5) {
                if (!this.insertItem(originalStack, 5, 6, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Handle hopper slots
            else if (index < 5) {
                if (!this.insertItem(originalStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Handle player inventory
            else {
                // Check filter before inserting
                ItemStack filter = this.inventory.getStack(5);
                if (filter.getItem() == Items.WOODEN_SWORD &&
                        !originalStack.isIn(ItemTags.LOGS)) {
                    return ItemStack.EMPTY;
                }

                if (!this.insertItem(originalStack, 0, 5, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }
}
