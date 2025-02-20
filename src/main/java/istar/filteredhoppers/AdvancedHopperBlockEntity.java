package istar.filteredhoppers;

import istar.filteredhoppers.AdvancedHopperScreenHandler;
import istar.filteredhoppers.ModBlockEntities;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.IntStream;

public class AdvancedHopperBlockEntity extends HopperBlockEntity implements SidedInventory {
    private static final Logger LOGGER = LoggerFactory.getLogger("AdvancedHopperBlockEntity");

    public AdvancedHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        LOGGER.info("Creating AdvancedHopperBlockEntity at {}", pos);
        this.setInvStackList(DefaultedList.ofSize(6, ItemStack.EMPTY)); // Inventory size 6
    }

    @Override
    public int size() {
        return 6;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, AdvancedHopperBlockEntity blockEntity) {
        if (!world.isClient) {
            LOGGER.info("Hopper checking for items at {}", pos);

            List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class,
                    new Box(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1),
                    item -> !item.isRemoved() && item.getStack() != null);

            LOGGER.info("Found {} items above the hopper", items.size());

            blockEntity.transferItems();
        }
    }


    public void transferItems() {
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class,
                new Box(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1),
                item -> !item.isRemoved() && item.getStack() != null);

        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getStack();
            if (!stack.isEmpty() && canAcceptItem(stack)) {
                for (int i = 0; i < size(); i++) {
                    if (getStack(i).isEmpty()) {
                        setStack(i, stack.copy());
                        itemEntity.discard();
                        break;  // Stop after picking one item per tick
                    }
                }
            }
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.ADVANCED_HOPPER_BLOCK_ENTITY;
    }
    @Override
    public Text getDisplayName() {
        return Text.translatable("container.advanced_hopper");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AdvancedHopperScreenHandler(syncId, playerInventory, this);
    }

    // Override SidedInventory methods to enforce filter
    @Override
    public int[] getAvailableSlots(Direction side) {
        return side == Direction.DOWN ? new int[]{0, 1, 2, 3, 4} : IntStream.range(0, 6).toArray();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        if (slot == 5) {
            return stack.isOf(Items.WOODEN_SWORD); // Filter slot only accepts wooden swords
        } else if (slot < 5) {
            ItemStack filter = getStack(5);
            if (filter.getItem() == Items.WOODEN_SWORD) {
                return stack.isIn(ItemTags.LOGS); // Only allow logs if filter is present
            }
            return true; // Accept any item if no filter
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot < 5; // Allow extracting from main slots
    }

    // Handle NBT data for additional slot
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        // Additional data if needed
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // Additional data if needed
    }
    public boolean canAcceptItem(ItemStack stack) {
        ItemStack filter = this.getStack(5); // Get the filter item from slot 5
        if (filter.getItem() == Items.WOODEN_SWORD) {
            return stack.isIn(ItemTags.LOGS); // Only allow logs if the filter is a wooden sword
        }
        return true; // Allow all items if no filter is present
    }
}