package dev.xpple.clientarguments.arguments;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClientBlockArgument {
    private final BlockState state;
    private final Map<Property<?>, Comparable<?>> properties;
    @Nullable
    private final NbtCompound nbt;

    ClientBlockArgument(BlockArgumentParser.BlockResult result) {
        this.state = result.blockState();
        this.properties = result.properties();
        this.nbt = result.nbt();
    }

    private boolean isSameBlock(Block other) {
        return this.state.getBlock().equals(other);
    }

    private boolean isSameBlockState(BlockState other) {
        return this.state == other;
    }

    private boolean isSameNbt(NbtCompound other) {
        return NbtHelper.matches(this.nbt, other, true);
    }

    public Block getBlock() {
        return this.state.getBlock();
    }

    public BlockState getBlockState() {
        return this.state;
    }

    @Nullable
    public NbtCompound getNbt() {
        return this.nbt;
    }

    public Map<Property<?>, Comparable<?>> getProperties() {
        return this.properties;
    }
}
