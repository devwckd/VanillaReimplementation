package net.minestom.vanilla.blockupdatesystem;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A utility class used to facilitate block updates
 */
public class BlockUpdateManager {
    // Block update manager by instance
    private static final Map<Instance, BlockUpdateManager> instance2BlockUpdateManager =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void init(EventNode<Event> eventNode) {
        eventNode.addListener(InstanceTickEvent.class, BlockUpdateManager::instanceTick);
        eventNode.addListener(PlayerBlockBreakEvent.class, event ->
                BlockUpdateManager.from(event.getPlayer().getInstance())
                        .scheduleNeighborsUpdate(event.getBlockPosition(),
                                BlockUpdateInfo.DESTROY_BLOCK(event.getBlockPosition()))
        );
        eventNode.addListener(PlayerBlockPlaceEvent.class, event ->
                BlockUpdateManager.from(event.getPlayer().getInstance())
                        .scheduleNeighborsUpdate(event.getBlockPosition(),
                                BlockUpdateInfo.PLACE_BLOCK(event.getBlockPosition()))
        );
    }

    private static void instanceTick(InstanceTickEvent event) {
        Instance instance = event.getInstance();
        from(instance).tick(event.getDuration());
    }

    public static @NotNull BlockUpdateManager from(@NotNull Instance instance) {
        return instance2BlockUpdateManager.computeIfAbsent(instance, BlockUpdateManager::new);
    }

    private final Map<Point, BlockUpdateInfo> updateNeighbors = Collections.synchronizedMap(new LinkedHashMap<>());
    private final BlockUpdateManager.UpdateHandler updateHandler;

    public BlockUpdateManager(@NotNull BlockUpdateManager.UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }

    private BlockUpdateManager(@NotNull Instance instance) {
        this.updateHandler = (pos, info) -> {
            if (instance.getBlock(pos).handler() instanceof BlockUpdatable updatable) {
                updatable.blockUpdate(instance, pos, info);
            }
        };
    }

    public interface UpdateHandler {
        void update(@NotNull Point pos, @NotNull BlockUpdateInfo info);
    }

    // Public api methods

    /**
     * Schedules this position's neighbors to be updated next tick.
     */
    public void scheduleNeighborsUpdate(Point pos, BlockUpdateInfo info) {
        updateNeighbors.put(pos, info);
    }

    // Public api methods end

    private void tick(int duration) {
        updateNeighbors(duration);
    }

    private void updateNeighbors(int duration) {
        if (updateNeighbors.size() == 0) {
            return;
        }

        // Update all the neighbors
        for (Map.Entry<Point, BlockUpdateInfo> entry : updateNeighbors.entrySet()) {
            Point pos = entry.getKey();
            BlockUpdateInfo info = entry.getValue();

            int x = pos.blockX();
            int y = pos.blockY();
            int z = pos.blockZ();

            // For each surrounding block
            for (int offsetX = -1; offsetX < 2; offsetX++) {
                for (int offsetY = -1; offsetY < 2; offsetY++) {
                    for (int offsetZ = -1; offsetZ < 2; offsetZ++) {

                        // If block is not the original block
                        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) {
                            continue;
                        }

                        // Get the block handler at the position
                        Point blockPos = new Pos(x + offsetX, y + offsetY, z + offsetZ);
                        updateHandler.update(blockPos, info);
                    }
                }
            }
        }

        updateNeighbors.clear();
    }
}
