package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CItemPredicateArgumentType implements ArgumentType<Predicate<ItemStack>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");

	private final RegistryWrapper<Item> registryWrapper;

	public CItemPredicateArgumentType(CommandRegistryAccess registryAccess) {
		this.registryWrapper = registryAccess.createWrapper(RegistryKeys.ITEM);
	}

	public static CItemPredicateArgumentType itemPredicate(CommandRegistryAccess registryAccess) {
		return new CItemPredicateArgumentType(registryAccess);
	}

	@Override
	public Predicate<ItemStack> parse(final StringReader stringReader) throws CommandSyntaxException {
		return ItemStringReader.itemOrTag(registryWrapper, stringReader).map(
				itemResult -> new ItemPredicate(itemResult.item().value(), itemResult.nbt()),
				tagResult -> new TagPredicate(tagResult.tag(), tagResult.nbt())
		);
	}

	@SuppressWarnings("unchecked")
	public static Predicate<ItemStack> getCItemPredicate(CommandContext<FabricClientCommandSource> context, String name) {
		return context.getArgument(name, Predicate.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return ItemStringReader.getSuggestions(registryWrapper, builder, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static class ItemPredicate implements Predicate<ItemStack> {
		private final Item item;
		@Nullable
		private final NbtCompound nbt;

		public ItemPredicate(Item item, @Nullable NbtCompound nbt) {
			this.item = item;
			this.nbt = nbt;
		}

		@Override
		public boolean test(ItemStack itemStack) {
			return itemStack.isOf(this.item) && NbtHelper.matches(this.nbt, itemStack.getNbt(), true);
		}
	}

	private static class TagPredicate implements Predicate<ItemStack> {
		private final RegistryEntryList<Item> tag;
		@Nullable
		private final NbtCompound compound;

		public TagPredicate(RegistryEntryList<Item> tag, @Nullable NbtCompound nbt) {
			this.tag = tag;
			this.compound = nbt;
		}

		@Override
		public boolean test(ItemStack itemStack) {
			return this.tag.contains(itemStack.getRegistryEntry()) && NbtHelper.matches(this.compound, itemStack.getNbt(), true);
		}
	}
}
