package net.minestom.vanilla.blocks;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.*;
import net.minestom.vanilla.inventory.ChestInventory;
import net.minestom.vanilla.inventory.ItemStackUtils;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTList;
import org.jglrxavpok.hephaistos.nbt.NBTTypes;

import java.util.Objects;
import java.util.Random;

/**
 * Base class for Ender Chest, Chest and Trapped Chest
 *
 * This class needs onPlace to be able to change the block being placed
 */
public abstract class ChestLikeBlockHandler extends VanillaBlockHandler {

    public static final Tag<NBTList<NBTCompound>> TAG_ITEMS = Tag.NBT("ITEMS");
    protected static final Random rng = new Random();

    public ChestLikeBlockHandler(Block baseBlock) {
        super(baseBlock);
    }

    @Override
    public void onPlace(Placement placement) {
        Block block = placement.getBlock();

        NBTList<NBTCompound> items = block.getTag(TAG_ITEMS);

        if (items != null) {
            return;
        }

        Instance instance = placement.getInstance();
        Point pos = placement.getBlockPosition();
        NBTList<NBTCompound> nbtCompoundNBTList = new NBTList<>(NBTTypes.TAG_Compound);

        Block blockToSet = block.withTag(TAG_ITEMS, nbtCompoundNBTList);
        // TODO: Override blockToSet
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        Block block = destroy.getBlock();
        Point pos = destroy.getBlockPosition();

        if (!dropContentsOnDestroy()) {
            return;
        }

        NBTList<NBTCompound> items = getItems(block);

        for (NBTCompound item : items) {
            ItemStack itemStack = ItemStackUtils.fromNBTCompound(item);

            if (itemStack == null) {
                continue;
            }

            ItemEntity entity = new ItemEntity(itemStack);

            entity.setInstance(destroy.getInstance());
            entity.teleport(
                    new Pos(
                            (pos.x() + rng.nextDouble()),
                            pos.y() + .5f,
                            (pos.z() + rng.nextDouble())
                    )
            );
        }
    }

//    @Override
//    public short getVisualBlockForPlacement(Player player, Player.Hand hand, BlockPosition position) {
//        // TODO: handle double chests
//        boolean waterlogged = Block.fromStateId(player.getInstance().getBlockStateId(position.getX(), position.getY(), position.getZ())) == Block.WATER;
//        float yaw = player.getPosition().getYaw();
//        Direction direction = MathUtils.getHorizontalDirection(yaw).opposite();
//        return getBaseBlockState().with("facing", direction.name().toLowerCase()).with("waterlogged", String.valueOf(waterlogged)).getBlockId();
//    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        // TODO: handle double chests
        // TODO: Handle crouching players

        Block block = interaction.getBlock();
        Instance instance = interaction.getInstance();
        Point pos = interaction.getBlockPosition();
        Player player = interaction.getPlayer();

        Block above = instance.getBlock(pos.blockX(), pos.blockY() + 1, pos.blockZ());

        if (above.isSolid()) { // FIXME: chests below transparent blocks cannot be opened
            return false;
        }

        Inventory chestInventory = new ChestInventory(getItems(block));
        player.openInventory(chestInventory);
        return true;
    }

    public abstract boolean dropContentsOnDestroy();

    /**
     * Gets the items in this block only
     * @param block the block
     * @return the items
     */
    protected @NotNull NBTList<NBTCompound> getItems(Block block) {
        return Objects.requireNonNull(block.getTag(TAG_ITEMS));
    }

    /**
     * Gets all items represented by this position in this instance
     * @param instance the instance
     * @param pos the position
     * @return all items in the position in the instance
     */
    protected NBTList<NBTCompound> getAllItems(Instance instance, Point pos, Player player) {
        Block block = instance.getBlock(pos);
        NBTList<NBTCompound> items = getItems(block);

        Point positionOfOtherChest = pos;
        Direction facing = Direction.valueOf(block.getProperty("facing").toUpperCase());
        String type = block.getProperty("type");

        switch (type) {
            case "single":
                return items;
            case "left":
                positionOfOtherChest = positionOfOtherChest.add(-facing.normalZ(), 0, facing.normalX());
                break;
            case "right":
                positionOfOtherChest = positionOfOtherChest.add(facing.normalZ(), 0, -facing.normalX());
                break;
            default:
                throw new IllegalArgumentException("Invalid chest type: " + type);
        }

        Block otherBlock = instance.getBlock(positionOfOtherChest);
        BlockHandler handler = otherBlock.handler();

        if (handler instanceof ChestLikeBlockHandler) {
            NBTList<NBTCompound> otherItems = ((ChestLikeBlockHandler) handler)
                    .getItems(otherBlock)
                    .deepClone();

            for (NBTCompound item : items) {
                otherItems.add(item.deepClone());
            }

            return otherItems;
        }

        return items;
    }

//    @Override
//    public Data readBlockEntity(NBTCompound nbt, Instance instance, BlockPosition position, Data originalData) {
//        ChestBlockEntity data;
//        if (originalData instanceof ChestBlockEntity) {
//            data = (ChestBlockEntity) originalData;
//        } else {
//            data = new ChestBlockEntity(position.copy());
//        }
//
//        // TODO: CustomName
//        // TODO: Lock
//        // TODO: LootTable
//        // TODO: LootTableSeed
//
//        if (nbt.containsKey("Items")) {
//            NBTUtils.loadAllItems(nbt.getList("Items"), data.getInventory());
//        }
//
//        return data;
//    }

//    @Override
//    public void writeBlockEntity(BlockPosition position, Data blockData, NBTCompound nbt) {
//        // TODO: CustomName
//        // TODO: Lock
//        // TODO: LootTable
//        // TODO: LootTableSeed
//        if (blockData instanceof ChestBlockEntity) {
//            ChestBlockEntity data = (ChestBlockEntity) blockData;
//            NBTList<NBTCompound> list = new NBTList<>(NBTTypes.TAG_Compound);
//            NBTUtils.saveAllItems(list, data.getInventory());
//            nbt.set("Items", list);
//        }
//    }
}
