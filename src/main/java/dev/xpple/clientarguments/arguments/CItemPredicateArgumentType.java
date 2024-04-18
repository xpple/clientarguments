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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.packrat.ArgumentParser;
import net.minecraft.command.argument.packrat.PackratParsing;
import net.minecraft.component.DataComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.ItemSubPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class CItemPredicateArgumentType implements ArgumentType<CItemPredicateArgumentType.CItemStackPredicateArgument> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
	static final DynamicCommandExceptionType INVALID_ITEM_ID_EXCEPTION = new DynamicCommandExceptionType(object -> Text.stringifiedTranslatable("argument.item.id.invalid", object));
	static final DynamicCommandExceptionType UNKNOWN_ITEM_TAG_EXCEPTION = new DynamicCommandExceptionType(object -> Text.stringifiedTranslatable("arguments.item.tag.unknown", object));
	static final DynamicCommandExceptionType UNKNOWN_ITEM_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(object -> Text.stringifiedTranslatable("arguments.item.component.unknown", object));
	static final Dynamic2CommandExceptionType MALFORMED_ITEM_COMPONENT_EXCEPTION = new Dynamic2CommandExceptionType((object, object2) -> Text.stringifiedTranslatable("arguments.item.component.malformed", object, object2));
	static final DynamicCommandExceptionType UNKNOWN_ITEM_PREDICATE_EXCEPTION = new DynamicCommandExceptionType(object -> Text.stringifiedTranslatable("arguments.item.predicate.unknown", object));
	static final Dynamic2CommandExceptionType MALFORMED_ITEM_PREDICATE_EXCEPTION = new Dynamic2CommandExceptionType((object, object2) -> Text.stringifiedTranslatable("arguments.item.predicate.malformed", object, object2));
	private static final Identifier COUNT_ID = new Identifier("count");
	static final Map<Identifier, ComponentCheck> SPECIAL_COMPONENT_CHECKS = Stream.of(new ComponentCheck(COUNT_ID, stack -> true, NumberRange.IntRange.CODEC.map(range -> stack -> range.test(stack.getCount())))).collect(Collectors.toUnmodifiableMap(ComponentCheck::id, check -> check));
	static final Map<Identifier, SubPredicateCheck> SPECIAL_SUB_PREDICATE_CHECKS = Stream.of(new SubPredicateCheck(COUNT_ID, NumberRange.IntRange.CODEC.map(range -> stack -> range.test(stack.getCount())))).collect(Collectors.toUnmodifiableMap(SubPredicateCheck::id, check -> check));
	private final ArgumentParser<List<Predicate<ItemStack>>> parser;

	public CItemPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) {
		class_9445 lv = new class_9445(commandRegistryAccess);
		this.parser = PackratParsing.createParser(lv);
	}

	public static CItemPredicateArgumentType itemPredicate(CommandRegistryAccess commandRegistryAccess) {
		return new CItemPredicateArgumentType(commandRegistryAccess);
	}

	@Override
	public CItemStackPredicateArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		return Util.allOf(this.parser.parse(stringReader))::test;
	}

	public static CItemStackPredicateArgument getItemStackPredicate(final CommandContext<FabricClientCommandSource> context, String name) {
		return context.getArgument(name, CItemStackPredicateArgument.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return this.parser.listSuggestions(builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	record ComponentCheck(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {
		public static <T> ComponentCheck read(ImmutableStringReader reader, Identifier id, DataComponentType<T> type) throws CommandSyntaxException {
			Codec<T> codec = type.getCodec();
			if (codec == null) {
				throw UNKNOWN_ITEM_COMPONENT_EXCEPTION.createWithContext(reader, id);
			}
			return new ComponentCheck(id, stack -> stack.contains(type), codec.map(expected -> stack -> {
				T object2 = stack.get(type);
				return Objects.equals(expected, object2);
			}));
		}

		public Predicate<ItemStack> createPredicate(ImmutableStringReader reader, RegistryOps<NbtElement> ops, NbtElement nbt) throws CommandSyntaxException {
			DataResult<? extends Predicate<ItemStack>> dataResult = this.valueChecker.parse(ops, nbt);
			return dataResult.getOrThrow(error -> MALFORMED_ITEM_COMPONENT_EXCEPTION.createWithContext(reader, this.id.toString(), error));
		}
	}

	public interface CItemStackPredicateArgument extends Predicate<ItemStack> {
	}

	record SubPredicateCheck(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
		public SubPredicateCheck(RegistryEntry.Reference<ItemSubPredicate.Type<?>> type) {
			this(type.registryKey().getValue(), type.value().codec().map(predicate -> predicate::test));
		}

		public Predicate<ItemStack> createPredicate(ImmutableStringReader reader, RegistryOps<NbtElement> ops, NbtElement nbt) throws CommandSyntaxException {
			DataResult<? extends Predicate<ItemStack>> dataResult = this.type.parse(ops, nbt);
			return dataResult.getOrThrow(error -> MALFORMED_ITEM_PREDICATE_EXCEPTION.createWithContext(reader, this.id.toString(), error));
		}
	}

	static class class_9445 implements PackratParsing.Callbacks<Predicate<ItemStack>, ComponentCheck, SubPredicateCheck> {
		private final RegistryWrapper.Impl<Item> itemRegistryWrapper;
		private final RegistryWrapper.Impl<DataComponentType<?>> dataComponentTypeRegistryWrapper;
		private final RegistryWrapper.Impl<ItemSubPredicate.Type<?>> itemSubPredicateTypeRegistryWrapper;
		private final RegistryOps<NbtElement> nbtOps;

		class_9445(RegistryWrapper.WrapperLookup registryLookup) {
			this.itemRegistryWrapper = registryLookup.getWrapperOrThrow(RegistryKeys.ITEM);
			this.dataComponentTypeRegistryWrapper = registryLookup.getWrapperOrThrow(RegistryKeys.DATA_COMPONENT_TYPE);
			this.itemSubPredicateTypeRegistryWrapper = registryLookup.getWrapperOrThrow(RegistryKeys.ITEM_SUB_PREDICATE_TYPE);
			this.nbtOps = registryLookup.getOps(NbtOps.INSTANCE);
		}

		public Predicate<ItemStack> itemMatchPredicate(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
			RegistryEntry.Reference<Item> reference = this.itemRegistryWrapper
				.getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier))
				.orElseThrow(() -> INVALID_ITEM_ID_EXCEPTION.createWithContext(immutableStringReader, identifier));
			return stack -> stack.itemMatches(reference);
		}

		public Predicate<ItemStack> tagMatchPredicate(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
			RegistryEntryList<Item> registryEntryList = this.itemRegistryWrapper
				.getOptional(TagKey.of(RegistryKeys.ITEM, identifier))
				.orElseThrow(() -> UNKNOWN_ITEM_TAG_EXCEPTION.createWithContext(immutableStringReader, identifier));
			return stack -> stack.isIn(registryEntryList);
		}

		public ComponentCheck componentCheck(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
			ComponentCheck componentCheck = SPECIAL_COMPONENT_CHECKS.get(identifier);
			if (componentCheck != null) {
				return componentCheck;
			}
			DataComponentType<?> dataComponentType = this.dataComponentTypeRegistryWrapper
				.getOptional(RegistryKey.of(RegistryKeys.DATA_COMPONENT_TYPE, identifier))
				.map(RegistryEntry::value)
				.orElseThrow(() -> UNKNOWN_ITEM_COMPONENT_EXCEPTION.createWithContext(immutableStringReader, identifier));
			return ComponentCheck.read(immutableStringReader, identifier, dataComponentType);
		}

		public Predicate<ItemStack> componentMatchPredicate(ImmutableStringReader immutableStringReader, ComponentCheck componentCheck, NbtElement nbtElement) throws CommandSyntaxException {
			return componentCheck.createPredicate(immutableStringReader, this.nbtOps, nbtElement);
		}

		public Predicate<ItemStack> componentPresencePredicate(ImmutableStringReader immutableStringReader, ComponentCheck componentCheck) {
			return componentCheck.presenceChecker;
		}

		public SubPredicateCheck subPredicateCheck(ImmutableStringReader immutableStringReader, Identifier identifier) throws CommandSyntaxException {
			SubPredicateCheck subPredicateCheck = SPECIAL_SUB_PREDICATE_CHECKS.get(identifier);
			return subPredicateCheck != null ? subPredicateCheck : this.itemSubPredicateTypeRegistryWrapper
				.getOptional(RegistryKey.of(RegistryKeys.ITEM_SUB_PREDICATE_TYPE, identifier))
				.map(SubPredicateCheck::new)
				.orElseThrow(() -> UNKNOWN_ITEM_PREDICATE_EXCEPTION.createWithContext(immutableStringReader, identifier));
		}

		public Predicate<ItemStack> subPredicatePredicate(ImmutableStringReader immutableStringReader, SubPredicateCheck subPredicateCheck, NbtElement nbtElement) throws CommandSyntaxException {
			return subPredicateCheck.createPredicate(immutableStringReader, this.nbtOps, nbtElement);
		}

		@Override
		public Stream<Identifier> streamItemIds() {
			return this.itemRegistryWrapper.streamKeys().map(RegistryKey::getValue);
		}

		@Override
		public Stream<Identifier> streamTags() {
			return this.itemRegistryWrapper.streamTagKeys().map(TagKey::id);
		}

		@Override
		public Stream<Identifier> streamComponentIds() {
			return Stream.concat(
				SPECIAL_COMPONENT_CHECKS.keySet().stream(),
				this.dataComponentTypeRegistryWrapper
					.streamEntries()
					.filter(entry -> !entry.value().shouldSkipSerialization())
					.map(entry -> entry.registryKey().getValue())
			);
		}

		@Override
		public Stream<Identifier> streamSubPredicateIds() {
			return Stream.concat(SPECIAL_SUB_PREDICATE_CHECKS.keySet().stream(), this.itemSubPredicateTypeRegistryWrapper.streamKeys().map(RegistryKey::getValue));
		}

		public Predicate<ItemStack> negate(Predicate<ItemStack> predicate) {
			return predicate.negate();
		}

		public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> list) {
			return Util.anyOf(list);
		}
	}
}
