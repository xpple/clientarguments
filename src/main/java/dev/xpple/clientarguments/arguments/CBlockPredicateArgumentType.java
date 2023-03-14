package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CBlockPredicateArgumentType implements ArgumentType<CBlockPredicateArgumentType.BlockPredicate> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
	private final RegistryWrapper<Block> registryWrapper;

	public CBlockPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) {
		this.registryWrapper = commandRegistryAccess.createWrapper(RegistryKeys.BLOCK);
	}

	public static CBlockPredicateArgumentType blockPredicate(CommandRegistryAccess commandRegistryAccess) {
		return new CBlockPredicateArgumentType(commandRegistryAccess);
	}

	@Override
	public CBlockPredicateArgumentType.BlockPredicate parse(final StringReader stringReader) throws CommandSyntaxException {
		return parse(this.registryWrapper, stringReader);
	}

	public static CBlockPredicateArgumentType.BlockPredicate parse(RegistryWrapper<Block> registryWrapper, StringReader reader) throws CommandSyntaxException {
		return BlockArgumentParser.blockOrTag(registryWrapper, reader, true).map((result) -> new StatePredicate(result.blockState(), result.properties().keySet(), result.nbt()), (result) -> new TagPredicate(result.tag(), result.vagueProperties(), result.nbt()));
	}

	public static Predicate<CachedBlockPosition> getCBlockPredicate(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CBlockPredicateArgumentType.BlockPredicate.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return BlockArgumentParser.getSuggestions(this.registryWrapper, builder, true, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public interface BlockPredicate extends Predicate<CachedBlockPosition> {
		boolean hasNbt();
	}

	static class TagPredicate implements CBlockPredicateArgumentType.BlockPredicate {
		private final RegistryEntryList<Block> tag;
		@Nullable
		private final NbtCompound nbt;
		private final Map<String, String> properties;

		TagPredicate(RegistryEntryList<Block> tag, Map<String, String> properties, @Nullable NbtCompound nbt) {
			this.tag = tag;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(CachedBlockPosition cachedBlockPosition) {
			BlockState blockState = cachedBlockPosition.getBlockState();
			if (!blockState.isIn(this.tag)) {
				return false;
			} else {

				for (Map.Entry<String, String> stringStringEntry : this.properties.entrySet()) {
					Property<?> property = blockState.getBlock().getStateManager().getProperty(stringStringEntry.getKey());
					if (property == null) {
						return false;
					}

					Comparable<?> comparable = property.parse(stringStringEntry.getValue()).orElse(null);
					if (comparable == null) {
						return false;
					}

					if (blockState.get(property) != comparable) {
						return false;
					}
				}

				if (this.nbt == null) {
					return true;
				} else {
					BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
					return blockEntity != null && NbtHelper.matches(this.nbt, blockEntity.createNbtWithIdentifyingData(), true);
				}
			}
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}

	static class StatePredicate implements CBlockPredicateArgumentType.BlockPredicate {
		private final BlockState state;
		private final Set<Property<?>> properties;
		@Nullable
		private final NbtCompound nbt;

		public StatePredicate(BlockState state, Set<Property<?>> properties, @Nullable NbtCompound nbt) {
			this.state = state;
			this.properties = properties;
			this.nbt = nbt;
		}

		public boolean test(CachedBlockPosition cachedBlockPosition) {
			BlockState blockState = cachedBlockPosition.getBlockState();
			if (!blockState.isOf(this.state.getBlock())) {
				return false;
			} else {
				for (Property<?> value : this.properties) {
					if (blockState.get(value) != this.state.get(value)) {
						return false;
					}
				}

				if (this.nbt == null) {
					return true;
				} else {
					BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
					return blockEntity != null && NbtHelper.matches(this.nbt, blockEntity.createNbtWithIdentifyingData(), true);
				}
			}
		}

		@Override
		public boolean hasNbt() {
			return this.nbt != null;
		}
	}
}
