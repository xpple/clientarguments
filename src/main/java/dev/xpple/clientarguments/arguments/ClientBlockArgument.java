package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Property;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagManager;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldView;

import java.util.Map;

public class ClientBlockArgument {

    private static final DynamicCommandExceptionType UNKNOWN_TAG_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("carguments.block.tag.unknown", id));
    private static final DynamicCommandExceptionType NULL_POINTER_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("carguments.block.null", arg));

    private final Block block;
    private final BlockState blockState;
    private final NbtCompound nbt;
    private final Identifier identifier;
    private final Map<String, String> properties;

    private boolean ignoreNbt = false;

    ClientBlockArgument(BlockArgumentParser parser) {
        BlockState blockState = parser.getBlockState();
        if (blockState == null) {
            this.identifier = parser.getTagId();
            this.properties = parser.getProperties();

            this.block = null;
            this.blockState = null;
        } else {
            this.block = blockState.getBlock();
            this.blockState = blockState;

            this.identifier = null;
            this.properties = null;
        }
        this.nbt = parser.getNbtData();
    }

    ClientBlockArgument ignoreNbt() {
        this.ignoreNbt = true;
        return this;
    }

    private boolean isSameBlock(Block other) {
        return this.block.equals(other);
    }

    private boolean isSameBlockState(BlockState other) {
        if (!this.blockState.isOf(other.getBlock())) {
            return false;
        }
        for (Property<?> property : this.blockState.getProperties()) {
            if (this.blockState.get(property) != other.get(property)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSameNbt(NbtCompound other) {
        return NbtHelper.matches(this.nbt, other, true);
    }

    public Block getBlock() throws CommandSyntaxException {
        if (this.block == null) {
            throw NULL_POINTER_EXCEPTION.create("block");
        }
        return this.block;
    }

    public BlockState getBlockState() throws CommandSyntaxException {
        if (this.blockState == null) {
            throw NULL_POINTER_EXCEPTION.create("block state");
        }
        return this.blockState;
    }

    public NbtCompound getNbt() throws CommandSyntaxException {
        if (this.nbt == null) {
            throw NULL_POINTER_EXCEPTION.create("nbt");
        }
        return this.nbt;
    }

    public Identifier getIdentifier() throws CommandSyntaxException {
        if (this.identifier == null) {
            throw NULL_POINTER_EXCEPTION.create("identifier");
        }
        return this.identifier;
    }

    public Map<String, String> getProperties() throws CommandSyntaxException {
        if (this.properties == null) {
            throw NULL_POINTER_EXCEPTION.create("properties");
        }
        return this.properties;
    }

    public boolean test(WorldView world, BlockPos pos) throws CommandSyntaxException {
        if (this.blockState == null) {
            TagManager tagManager = MinecraftClient.getInstance().getNetworkHandler().getTagManager();
            BlockState blockState = world.getBlockState(pos);
            Tag<Block> tag = tagManager.getTag(Registry.BLOCK_KEY, this.identifier, id -> UNKNOWN_TAG_EXCEPTION.create(id.toString()));
            if (!blockState.isIn(tag)) {
                return false;
            }
            for (Map.Entry<String, String> entry : this.properties.entrySet()) {
                Property<?> property = blockState.getBlock().getStateManager().getProperty(entry.getKey());
                if (property == null) {
                    return false;
                }

                Comparable<?> comparable = property.parse(entry.getValue()).orElse(null);
                if (comparable == null) {
                    return false;
                }

                if (blockState.get(property) != comparable) {
                    return false;
                }
            }
            if (this.ignoreNbt) {
                return true;
            }
            BlockEntity be = world.getBlockEntity(pos);
            if (be == null) {
                return true;
            }
            return isSameNbt(be.writeNbt(new NbtCompound()));
        } else {
            BlockState other = world.getBlockState(pos);
            if (!isSameBlock(other.getBlock())) {
                return false;
            }
            if (!isSameBlockState(other)) {
                return false;
            }
            if (this.ignoreNbt) {
                return true;
            }
            BlockEntity be = world.getBlockEntity(pos);
            if (be == null) {
                return true;
            }
            return isSameNbt(be.writeNbt(new NbtCompound()));
        }
    }

    @FunctionalInterface
    public interface ClientBlockPredicate {
        boolean test(WorldView world, BlockPos pos) throws CommandSyntaxException;
    }
}
