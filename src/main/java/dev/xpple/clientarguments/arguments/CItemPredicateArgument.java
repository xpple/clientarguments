package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CItemPredicateArgument implements ArgumentType<CItemPredicateArgument.CItemStackPredicateArgument> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
	static final DynamicCommandExceptionType INVALID_ITEM_ID_EXCEPTION = new DynamicCommandExceptionType(object -> Component.translatableEscape("argument.item.id.invalid", object));
	static final DynamicCommandExceptionType UNKNOWN_ITEM_TAG_EXCEPTION = new DynamicCommandExceptionType(object -> Component.translatableEscape("arguments.item.tag.unknown", object));
	static final DynamicCommandExceptionType UNKNOWN_ITEM_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(object -> Component.translatableEscape("arguments.item.component.unknown", object));
	static final Dynamic2CommandExceptionType MALFORMED_ITEM_COMPONENT_EXCEPTION = new Dynamic2CommandExceptionType((object, object2) -> Component.translatableEscape("arguments.item.component.malformed", object, object2));
	static final DynamicCommandExceptionType UNKNOWN_ITEM_PREDICATE_EXCEPTION = new DynamicCommandExceptionType(object -> Component.translatableEscape("arguments.item.predicate.unknown", object));
	static final Dynamic2CommandExceptionType MALFORMED_ITEM_PREDICATE_EXCEPTION = new Dynamic2CommandExceptionType((object, object2) -> Component.translatableEscape("arguments.item.predicate.malformed", object, object2));
	private static final Identifier COUNT_ID = Identifier.withDefaultNamespace("count");
	static final Map<Identifier, ComponentWrapper> SPECIAL_COMPONENT_CHECKS = Stream.of(new ComponentWrapper(COUNT_ID, stack -> true, MinMaxBounds.Ints.CODEC.map(range -> stack -> range.matches(stack.getCount())))).collect(Collectors.toUnmodifiableMap(ComponentWrapper::id, check -> check));
	static final Map<Identifier, PredicateWrapper> SPECIAL_SUB_PREDICATE_CHECKS = Stream.of(new PredicateWrapper(COUNT_ID, MinMaxBounds.Ints.CODEC.map(range -> stack -> range.matches(stack.getCount())))).collect(Collectors.toUnmodifiableMap(PredicateWrapper::id, check -> check));
	private final Grammar<List<Predicate<ItemStack>>> parser;

	public CItemPredicateArgument(CommandBuildContext commandBuildContext) {
		Context context = new Context(commandBuildContext);
		this.parser = ComponentPredicateParser.createGrammar(context);
	}

	public static CItemPredicateArgument itemPredicate(CommandBuildContext commandRegistryAccess) {
		return new CItemPredicateArgument(commandRegistryAccess);
	}

	@Override
	public CItemStackPredicateArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		return Util.allOf(this.parser.parseForCommands(stringReader))::test;
	}

	public static CItemStackPredicateArgument getItemStackPredicate(final CommandContext<FabricClientCommandSource> context, String name) {
		return context.getArgument(name, CItemStackPredicateArgument.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return this.parser.parseForSuggestions(builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	record ComponentWrapper(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {
		public static <T> ComponentWrapper read(ImmutableStringReader reader, Identifier id, DataComponentType<T> type) throws CommandSyntaxException {
			Codec<T> codec = type.codec();
			if (codec == null) {
				throw UNKNOWN_ITEM_COMPONENT_EXCEPTION.createWithContext(reader, id);
			}
			return new ComponentWrapper(id, stack -> stack.has(type), codec.map(expected -> stack -> {
				T object2 = stack.get(type);
				return Objects.equals(expected, object2);
			}));
		}

		public Predicate<ItemStack> createPredicate(ImmutableStringReader reader, Dynamic<?> nbt) throws CommandSyntaxException {
			DataResult<? extends Predicate<ItemStack>> dataResult = this.valueChecker.parse(nbt);
			return dataResult.getOrThrow(error -> MALFORMED_ITEM_COMPONENT_EXCEPTION.createWithContext(reader, this.id.toString(), error));
		}
	}

	public interface CItemStackPredicateArgument extends Predicate<ItemStack> {
	}

	record PredicateWrapper(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
		public PredicateWrapper(Holder.Reference<DataComponentPredicate.Type<?>> type) {
			this(type.key().identifier(), type.value().codec().map(predicate -> predicate::matches));
		}

		public Predicate<ItemStack> createPredicate(ImmutableStringReader reader, Dynamic<?> nbt) throws CommandSyntaxException {
			DataResult<? extends Predicate<ItemStack>> dataResult = this.type.parse(nbt);
			return dataResult.getOrThrow(error -> MALFORMED_ITEM_PREDICATE_EXCEPTION.createWithContext(reader, this.id.toString(), error));
		}
	}

	static class Context implements ComponentPredicateParser.Context<Predicate<ItemStack>, ComponentWrapper, PredicateWrapper> {
		private final HolderLookup.Provider registries;
		private final HolderLookup.RegistryLookup<Item> itemHolderLookup;
		private final HolderLookup.RegistryLookup<DataComponentType<?>> dataComponentTypeHolderLookup;
		private final HolderLookup.RegistryLookup<DataComponentPredicate.Type<?>> itemSubPredicateTypeHolderLookup;

		Context(HolderLookup.Provider registries) {
			this.registries = registries;
			this.itemHolderLookup = registries.lookupOrThrow(Registries.ITEM);
			this.dataComponentTypeHolderLookup = registries.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
			this.itemSubPredicateTypeHolderLookup = registries.lookupOrThrow(Registries.DATA_COMPONENT_PREDICATE_TYPE);
		}

		@Override
		public Predicate<ItemStack> forElementType(ImmutableStringReader immutableStringReader, Identifier id) throws CommandSyntaxException {
			Holder.Reference<Item> reference = this.itemHolderLookup
				.get(ResourceKey.create(Registries.ITEM, id))
				.orElseThrow(() -> INVALID_ITEM_ID_EXCEPTION.createWithContext(immutableStringReader, id));
			return stack -> stack.is(reference);
		}

		@Override
		public Predicate<ItemStack> forTagType(ImmutableStringReader immutableStringReader, Identifier id) throws CommandSyntaxException {
			HolderSet<Item> registryEntryList = this.itemHolderLookup
				.get(TagKey.create(Registries.ITEM, id))
				.orElseThrow(() -> UNKNOWN_ITEM_TAG_EXCEPTION.createWithContext(immutableStringReader, id));
			return stack -> stack.is(registryEntryList);
		}

		@Override
		public ComponentWrapper lookupComponentType(ImmutableStringReader immutableStringReader, Identifier id) throws CommandSyntaxException {
			ComponentWrapper componentWrapper = SPECIAL_COMPONENT_CHECKS.get(id);
			if (componentWrapper != null) {
				return componentWrapper;
			}
			DataComponentType<?> dataComponentType = this.dataComponentTypeHolderLookup
				.get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, id))
				.map(Holder::value)
				.orElseThrow(() -> UNKNOWN_ITEM_COMPONENT_EXCEPTION.createWithContext(immutableStringReader, id));
			return ComponentWrapper.read(immutableStringReader, id, dataComponentType);
		}

		@Override
		public Predicate<ItemStack> createComponentTest(ImmutableStringReader immutableStringReader, ComponentWrapper componentWrapper, Dynamic<?> nbtElement) throws CommandSyntaxException {
			return componentWrapper.createPredicate(immutableStringReader, RegistryOps.injectRegistryContext(nbtElement, registries));
		}

		@Override
		public Predicate<ItemStack> createComponentTest(ImmutableStringReader immutableStringReader, ComponentWrapper componentWrapper) {
			return componentWrapper.presenceChecker;
		}

		@Override
		public PredicateWrapper lookupPredicateType(ImmutableStringReader immutableStringReader, Identifier id) throws CommandSyntaxException {
			PredicateWrapper predicateWrapper = SPECIAL_SUB_PREDICATE_CHECKS.get(id);
			return predicateWrapper != null ? predicateWrapper : this.itemSubPredicateTypeHolderLookup
				.get(ResourceKey.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, id))
				.map(PredicateWrapper::new)
				.orElseThrow(() -> UNKNOWN_ITEM_PREDICATE_EXCEPTION.createWithContext(immutableStringReader, id));
		}

		@Override
		public Predicate<ItemStack> createPredicateTest(ImmutableStringReader immutableStringReader, PredicateWrapper predicateWrapper, Dynamic<?> nbtElement) throws CommandSyntaxException {
			return predicateWrapper.createPredicate(immutableStringReader, RegistryOps.injectRegistryContext(nbtElement, registries));
		}

		@Override
		public Stream<Identifier> listElementTypes() {
			return this.itemHolderLookup.listElementIds().map(ResourceKey::identifier);
		}

		@Override
		public Stream<Identifier> listTagTypes() {
			return this.itemHolderLookup.listTagIds().map(TagKey::location);
		}

		@Override
		public Stream<Identifier> listComponentTypes() {
			return Stream.concat(
				SPECIAL_COMPONENT_CHECKS.keySet().stream(),
				this.dataComponentTypeHolderLookup
					.listElements()
					.filter(entry -> !entry.value().isTransient())
					.map(entry -> entry.key().identifier())
			);
		}

		@Override
		public Stream<Identifier> listPredicateTypes() {
			return Stream.concat(SPECIAL_SUB_PREDICATE_CHECKS.keySet().stream(), this.itemSubPredicateTypeHolderLookup.listElementIds().map(ResourceKey::identifier));
		}

		@Override
		public Predicate<ItemStack> negate(Predicate<ItemStack> predicate) {
			return predicate.negate();
		}

		@Override
		public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> list) {
			return Util.anyOf(list);
		}
	}
}
