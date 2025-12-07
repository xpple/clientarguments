package dev.xpple.clientarguments.arguments;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

public class CBlockInput implements Predicate<BlockInWorld> {
    private final BlockState state;
    private final Set<Property<?>> properties;
    @Nullable
    private final CompoundTag data;

    public CBlockInput(BlockState state, Set<Property<?>> properties, @Nullable CompoundTag data) {
        this.state = state;
        this.properties = properties;
        this.data = data;
    }

    public BlockState getState() {
        return this.state;
    }

    public Set<Property<?>> getDefinedProperties() {
        return this.properties;
    }

    public boolean test(BlockInWorld blockInWorld) {
        BlockState blockState = blockInWorld.getState();
        if (!blockState.is(this.state.getBlock())) {
            return false;
        }
        for (Property<?> property : this.properties) {
            if (blockState.getValue(property) != this.state.getValue(property)) {
                return false;
            }
        }

        if (this.data == null) {
            return true;
        }
        BlockEntity blockEntity = blockInWorld.getEntity();
        return blockEntity != null && NbtUtils.compareNbt(this.data, blockEntity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
    }

    public boolean test(ClientLevel world, BlockPos pos) {
        return this.test(new BlockInWorld(world, pos, false));
    }
}
