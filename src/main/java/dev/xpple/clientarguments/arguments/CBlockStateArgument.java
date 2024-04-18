package dev.xpple.clientarguments.arguments;

import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CBlockStateArgument implements Predicate<CachedBlockPosition> {
    private final BlockState state;
    private final Set<Property<?>> properties;
    @Nullable
    private final NbtCompound data;

    public CBlockStateArgument(BlockState state, Set<Property<?>> properties, @Nullable NbtCompound data) {
        this.state = state;
        this.properties = properties;
        this.data = data;
    }

    public BlockState getBlockState() {
        return this.state;
    }

    public Set<Property<?>> getProperties() {
        return this.properties;
    }

    public boolean test(CachedBlockPosition cachedBlockPosition) {
        BlockState blockState = cachedBlockPosition.getBlockState();
        if (!blockState.isOf(this.state.getBlock())) {
            return false;
        }
        for (Property<?> property : this.properties) {
            if (blockState.get(property) != this.state.get(property)) {
                return false;
            }
        }

        if (this.data == null) {
            return true;
        }
        BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
        return blockEntity != null && NbtHelper.matches(this.data, blockEntity.createNbtWithIdentifyingData(cachedBlockPosition.getWorld().getRegistryManager()), true);
    }

    public boolean test(ClientWorld world, BlockPos pos) {
        return this.test(new CachedBlockPosition(world, pos, false));
    }
}
