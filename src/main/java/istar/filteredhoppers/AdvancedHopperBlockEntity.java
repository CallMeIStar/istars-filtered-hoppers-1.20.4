package istar.filteredhoppers;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.ItemTags;
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
import java.util.Optional;
import java.util.stream.IntStream;

public class AdvancedHopperBlockEntity extends HopperBlockEntity implements SidedInventory, ExtendedScreenHandlerFactory {
    private static final int FILTER_SLOT = 5;
    private int transferCooldown = -1;
    private long lastTickTime;
    private static final Logger LOGGER = LoggerFactory.getLogger("AdvancedHopperBlockEntity");

    public AdvancedHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        this.setInvStackList(DefaultedList.ofSize(6, ItemStack.EMPTY));
    }

    private boolean needsCooldown() {
        return this.transferCooldown > 0;
    }

    private void setTransferCooldown(int cooldown) {
        this.transferCooldown = cooldown;
    }

    @Override
    public int size() {
        return 6;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        LOGGER.info("NBT Data Loaded: {}", nbt);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        LOGGER.info("NBT Data Saved: {}", nbt);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, AdvancedHopperBlockEntity blockEntity) {
        blockEntity.transferCooldown--;
        blockEntity.lastTickTime = world.getTime();

        if (!blockEntity.needsCooldown()) {
            blockEntity.setTransferCooldown(0);
            blockEntity.tick(world, pos, state);
        }
    }

    private void tick(World world, BlockPos pos, BlockState state) {
        if (!world.isClient && state.get(HopperBlock.ENABLED)) {
            boolean transferred = false;
            transferred = extractFromWorld(world);
            if (!this.isEmpty()) {
                transferred |= insertIntoInventory(world, pos, state);
            }
            if (transferred) {
                LOGGER.info("Item transfer successful at {}", pos);
                this.setTransferCooldown(8);
                this.markDirty();
            }
        }
    }

    private boolean insertIntoInventory(World world, BlockPos pos, BlockState state) {
        Direction hopperFacing = state.get(HopperBlock.FACING);
        Optional<Storage<ItemVariant>> destination = getItemStorage(world, pos.offset(hopperFacing), hopperFacing.getOpposite());

        if (destination.isEmpty()) return false;

        Storage<ItemVariant> storage = destination.get();
        boolean transferred = false;

        try (Transaction transaction = Transaction.openOuter()) {
            for (int i = 0; i < 5; i++) {
                ItemStack stack = this.getStack(i);
                if (!stack.isEmpty()) {
                    long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                    if (inserted > 0) {
                        stack.decrement((int) inserted);
                        transferred = true;
                        if (stack.isEmpty()) {
                            this.setStack(i, ItemStack.EMPTY);
                        }
                    }
                }
            }
            transaction.commit();
        }

        return transferred;
    }

    private boolean extractFromWorld(World world) {
        Optional<Storage<ItemVariant>> source = getItemStorage(world, this.getPos().up(), Direction.DOWN);
        if (source.isPresent()) {
            return extractFromInventory(source.get());
        }
        return extractFromItemEntities(world);
    }

    private boolean extractFromInventory(Storage<ItemVariant> storage) {
        boolean extracted = false;
        try (Transaction transaction = Transaction.openOuter()) {
            for (StorageView<ItemVariant> slot : storage.nonEmptyViews()) {
                if (slot.isResourceBlank() || slot.getAmount() <= 0) continue;
                ItemStack extractedItem = slot.getResource().toStack(1);

                for (int j = 0; j < 5; j++) {
                    if (this.canInsert(j, extractedItem, Direction.DOWN)) {
                        ItemStack destStack = this.getStack(j);
                        if (destStack.isEmpty()) {
                            this.setStack(j, extractedItem);
                        } else {
                            destStack.increment(1);
                        }
                        storage.extract(slot.getResource(), 1, transaction);
                        extracted = true;
                        break;
                    }
                }
            }
            if (extracted) transaction.commit();
            else transaction.abort();
        }
        return extracted;
    }

    private boolean extractFromItemEntities(World world) {
        Box detectionBox = new Box(getHopperX() - 0.5, getHopperY(), getHopperZ() - 0.5,
                getHopperX() + 0.5, getHopperY() + 1.5, getHopperZ() + 0.5);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, detectionBox, e -> canAcceptItem(e.getStack()));

        for (ItemEntity item : items) {
            ItemStack itemStack = item.getStack();
            for (int slot = 0; slot < 5; slot++) {
                if (this.canInsert(slot, itemStack, Direction.DOWN)) {
                    ItemStack destStack = this.getStack(slot);
                    if (destStack.isEmpty()) {
                        this.setStack(slot, itemStack);
                        item.discard();
                        return true;
                    } else if (destStack.getCount() < destStack.getMaxCount()) {
                        destStack.increment(1);
                        itemStack.decrement(1);
                        if (itemStack.isEmpty()) item.discard();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Optional<Storage<ItemVariant>> getItemStorage(World world, BlockPos pos, Direction direction) {
        return Optional.ofNullable(ItemStorage.SIDED.find(world, pos, direction));
    }

    public boolean canAcceptItem(ItemStack stack) {
        ItemStack filter = this.getStack(FILTER_SLOT);
        return filter.isOf(Items.WOODEN_SWORD) ? stack.isIn(ItemTags.LOGS) : true;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return side == Direction.DOWN ? new int[]{0, 1, 2, 3, 4} : new int[]{0, 1, 2, 3, 4, FILTER_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return slot != FILTER_SLOT && canAcceptItem(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot != FILTER_SLOT;
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

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }
}
