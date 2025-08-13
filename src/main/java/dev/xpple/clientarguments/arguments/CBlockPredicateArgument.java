package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CBlockPredicateArgument implements ArgumentType<CBlockPredicateArgument.BlockPredicate> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
	private final HolderLookup<Block> holderLookup;

	public CBlockPredicateArgument(CommandBuildContext commandBuildContext) {
		this.holderLookup = commandBuildContext.lookupOrThrow(Registries.BLOCK);
	}

	public static CBlockPredicateArgument blockPredicate(CommandBuildContext commandBuildContext) {
		return new CBlockPredicateArgument(commandBuildContext);
	}

	@Override
	public CBlockPredicateArgument.BlockPredicate parse(final StringReader stringReader) throws CommandSyntaxException {
		return parse(this.holderLookup, stringReader);
	}

	public static CBlockPredicateArgument.BlockPredicate parse(HolderLookup<Block> holderLookup, StringReader reader) throws CommandSyntaxException {
		return BlockStateParser.parseForTesting(holderLookup, reader, true)
			.map(
				result -> new CBlockPredicateArgument.StatePredicate(result.blockState(), result.properties().keySet(), result.nbt()),
				result -> new CBlockPredicateArgument.TagPredicate(result.tag(), result.vagueProperties(), result.nbt())
			);
	}

	public static Predicate<BlockInWorld> getBlockPredicate(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CBlockPredicateArgument.BlockPredicate.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return BlockStateParser.fillSuggestions(this.holderLookup, builder, true, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public interface BlockPredicate extends Predicate<BlockInWorld> {
		boolean hasNbt();
	}

	static class StatePredicate implements CBlockPredicateArgument.BlockPredicate {
		private final BlockState state;
		private final Set<Property<?>> properties;
		@Nullable
		private final CompoundTag nbt;

		public StatePredicate(BlockState state, Set<Property<?>> properties, @Nullable CompoundTag nbt) {
			this.state = state;
			this.properties = properties;
			this.nbt = nbt;
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

			if (this.nbt == null) {
				return true;
			}
			BlockEntity blockEntity = blockInWorld.getEntity();
			return blockEntity != null && NbtUtils.compareNbt(this.nbt, blockEntity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}

	static class TagPredicate implements CBlockPredicateArgument.BlockPredicate {
		private final HolderSet<Block> tag;
		@Nullable
		private final CompoundTag nbt;
		private final Map<String, String> properties;

		TagPredicate(HolderSet<Block> tag, Map<String, String> properties, @Nullable CompoundTag nbt) {
			this.tag = tag;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(BlockInWorld blockInWorld) {
			BlockState blockState = blockInWorld.getState();
			if (!blockState.is(this.tag)) {
				return false;
			}
			for (Entry<String, String> entry : this.properties.entrySet()) {
				Property<?> property = blockState.getBlock().getStateDefinition().getProperty(entry.getKey());
				if (property == null) {
					return false;
				}

				Comparable<?> comparable = property.getValue(entry.getValue()).orElse(null);
				if (comparable == null) {
					return false;
				}

				if (blockState.getValue(property) != comparable) {
					return false;
				}
			}

			if (this.nbt == null) {
				return true;
			}
			BlockEntity blockEntity = blockInWorld.getEntity();
			return blockEntity != null && NbtUtils.compareNbt(this.nbt, blockEntity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}
}
