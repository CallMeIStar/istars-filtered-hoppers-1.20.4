package istar.filteredhoppers;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class AdvancedHopperBlockEntity extends BlockEntity implements Inventory, SidedInventory, ExtendedScreenHandlerFactory, Hopper { // Implement Hopper
    private static final int FILTER_SLOT = 5;
    private final DefaultedList<ItemStack> inventory;
    private int transferCooldown;
    private long lastTickTime;



    public AdvancedHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_HOPPER_BLOCK_ENTITY, pos, state);
        this.inventory = DefaultedList.ofSize(6, ItemStack.EMPTY);
        this.transferCooldown = 0;

    }

    public static void tick(World world, BlockPos pos, BlockState state, AdvancedHopperBlockEntity blockEntity) {
        if (world.isClient || blockEntity.transferCooldown > 0) {
            // Log the cooldown value to check if it's being decremented properly
            System.out.println("Cooldown: " + blockEntity.transferCooldown);
            blockEntity.transferCooldown--;
            return;
        }

        if (blockEntity.transfer()) {
            blockEntity.transferCooldown = 8;
            markDirty(world, pos, state);
            // Log when transfer happens and cooldown is reset
            System.out.println("Transfer successful, cooldown reset to 8.");
        }
    }

    public static Inventory getInventoryAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof Inventory) {
            return (Inventory) blockEntity;
        }
        return null;  // Return null if there's no inventory at that position
    }


    public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
        if (to instanceof SidedInventory sidedInventory && side != null) {
            int[] is = sidedInventory.getAvailableSlots(side);

            for (int i = 0; i < is.length && !stack.isEmpty(); i++) {
                stack = transfer(from, to, stack, is[i], side);
            }

            return stack;
        }

        int j = to.size();

        for (int i = 0; i < j && !stack.isEmpty(); i++) {
            stack = transfer(from, to, stack, i, side);
        }

        return stack;
    }

    private static boolean canInsert(Inventory inventory, ItemStack stack, int slot, @Nullable Direction side) {
        if (!inventory.isValid(slot, stack)) {
            return false;
        } else {
            if (inventory instanceof SidedInventory sidedInventory && !sidedInventory.canInsert(slot, stack, side)) {
                return false;
            }

            return true;
        }
    }

    private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction side) {
        ItemStack itemStack = to.getStack(slot);
        if (canInsert(to, stack, slot, side)) {
            boolean bl = false;
            boolean bl2 = to.isEmpty();
            if (itemStack.isEmpty()) {
                to.setStack(slot, stack);
                stack = ItemStack.EMPTY;
                bl = true;
            } else if (canMergeItems(itemStack, stack)) {
                int i = stack.getMaxCount() - itemStack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.decrement(j);
                itemStack.increment(j);
                bl = j > 0;
            }

            if (bl) {
                if (bl2 && to instanceof AdvancedHopperBlockEntity advancedHopperBlockEntity && !advancedHopperBlockEntity.isDisabled()) {
                    int j = 0;
                    if (from instanceof AdvancedHopperBlockEntity advancedHopperBlockEntity2 && advancedHopperBlockEntity.lastTickTime >= advancedHopperBlockEntity2.lastTickTime) {
                        j = 1;
                    }

                    advancedHopperBlockEntity.setTransferCooldown(8 - j);
                }

                to.markDirty();
            }
        }

        return stack;
    }




    private boolean transfer() {
        if (this.world == null) return false;

        Direction facing = getCachedState().get(AdvancedHopperBlock.FACING);
        boolean transferred = false;

        // Extract from above first
        Inventory sourceInventory = AdvancedHopperBlockEntity.getInventoryAt(this.world, this.pos.up());
        if (sourceInventory != null) {
            transferred = transferFrom(sourceInventory);
        } else if (!isBlockedAbove()) {
            transferred = extractItems();
        }

        // Transfer items to the container in the hopper's facing direction
        Inventory targetInventory = AdvancedHopperBlockEntity.getInventoryAt(this.world, this.pos.offset(facing));
        if (targetInventory != null) {
            transferred |= transferTo(targetInventory, facing);
        }

        return transferred;
    }

    private boolean extractItems() {
        Box pickupBox = new Box(this.pos).expand(0.2, 0.1, 0.2);
        List<ItemEntity> itemEntities = world.getEntitiesByClass(ItemEntity.class, pickupBox, EntityPredicates.VALID_ENTITY);

        for (ItemEntity itemEntity : itemEntities) {
            if (extract(this, itemEntity)) return true;
        }
        return false;
    }

    private static boolean extract(Inventory destinationInventory, ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getStack();
        ItemStack leftover = AdvancedHopperBlockEntity.transfer(null, destinationInventory, itemStack, null);

        if (leftover.isEmpty()) {
            itemEntity.discard();
            return true;
        } else {
            itemEntity.setStack(leftover);
            return false;
        }
    }

    private boolean transferFrom(Inventory source) {
        for (int i = 0; i < source.size(); i++) {
            ItemStack stack = source.getStack(i);
            if (!stack.isEmpty() && canAcceptItem(stack)) {
                if (!AdvancedHopperBlockEntity.transfer(source, this, stack.split(1), null).isEmpty()) {
                    source.markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean transferTo(Inventory target, Direction direction) {
        for (int i = 0; i < this.size() - 1; i++) { // Exclude filter slot
            ItemStack stack = this.getStack(i);
            if (!stack.isEmpty()) {
                ItemStack transferred = AdvancedHopperBlockEntity.transfer(this, target, stack.split(1), direction.getOpposite());
                if (!transferred.isEmpty()) {
                    this.markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canAcceptItem(ItemStack stack) {
        ItemStack filter = this.getStack(FILTER_SLOT);
        return filter.isEmpty() || !filter.isOf(Items.WOODEN_SWORD) || stack.isIn(ItemTags.LOGS);
    }

    private boolean isBlockedAbove() {
        BlockState stateAbove = world.getBlockState(pos.up());
        return stateAbove.isFullCube(world, pos.up()) || world.getBlockEntity(pos.up()) != null;
    }

    @Override
    public double getHopperX() { return this.pos.getX() + 0.5; }
    @Override
    public double getHopperY() { return this.pos.getY() + 0.5; }
    @Override
    public double getHopperZ() { return this.pos.getZ() + 0.5; }
    @Override
    public VoxelShape getInputAreaShape() { return VoxelShapes.cuboid(0.2, 0.1, 0.2, 0.8, 0.8, 0.8); }

    private boolean isDisabled() {
        return this.transferCooldown > 8;
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        return first.getCount() <= first.getMaxCount() && ItemStack.canCombine(first, second);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, this.inventory);
        this.transferCooldown = nbt.getInt("TransferCooldown");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, this.inventory);
        nbt.putInt("TransferCooldown", this.transferCooldown);
        super.writeNbt(nbt);
    }

    // Inventory Implementation
    @Override
    public int size() { return 6; }

    @Override
    public boolean isEmpty() {
        return this.inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) { return inventory.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    public void clear() { inventory.clear(); }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    // SidedInventory Implementation
    @Override
    public int[] getAvailableSlots(Direction side) {
        return side == Direction.DOWN ?
                new int[]{0, 1, 2, 3, 4} :
                new int[]{0, 1, 2, 3, 4, FILTER_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == FILTER_SLOT) return stack.isOf(Items.WOODEN_SWORD);
        return canAcceptItem(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot != FILTER_SLOT;
    }

    // Screen Handling
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
        buf.writeBlockPos(pos);
    }

    private void setTransferCooldown(int cooldown) {
        this.transferCooldown = cooldown;
    }
}