package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CItemPredicateArgumentType implements ArgumentType<CItemPredicateArgumentType.ItemPredicateArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
	private static final DynamicCommandExceptionType UNKNOWN_TAG_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("carguments.item.tag.unknown", id));

	public static CItemPredicateArgumentType itemPredicate() {
		return new CItemPredicateArgumentType();
	}

	public static Predicate<ItemStack> getItemPredicate(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, ItemPredicateArgument.class).create(context);
	}

	@Override
	public ItemPredicateArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		ItemStringReader itemStringReader = (new ItemStringReader(stringReader, true)).consume();
		if (itemStringReader.getItem() != null) {
			ItemPredicate itemPredicate = new ItemPredicate(itemStringReader.getItem(), itemStringReader.getNbt());
			return (context) -> itemPredicate;
		} else {
			Identifier identifier = itemStringReader.getId();
			return context -> {
				Tag<Item> tag = context.getSource().getWorld().getTagManager().getTag(Registry.ITEM_KEY, identifier, (id) -> UNKNOWN_TAG_EXCEPTION.create(id.toString()));
				return new TagPredicate(tag, itemStringReader.getNbt());
			};
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		ItemStringReader itemStringReader = new ItemStringReader(stringReader, true);

		try {
			itemStringReader.consume();
		} catch (CommandSyntaxException ignored) {
		}

		return itemStringReader.getSuggestions(builder, ItemTags.getTagGroup());
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

		public boolean test(ItemStack itemStack) {
			return itemStack.isOf(this.item) && NbtHelper.matches(this.nbt, itemStack.getNbt(), true);
		}
	}

	public interface ItemPredicateArgument {
		Predicate<ItemStack> create(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException;
	}

	private static class TagPredicate implements Predicate<ItemStack> {
		private final Tag<Item> tag;
		@Nullable
		private final NbtCompound compound;

		public TagPredicate(Tag<Item> tag, @Nullable NbtCompound nbt) {
			this.tag = tag;
			this.compound = nbt;
		}

		public boolean test(ItemStack itemStack) {
			return itemStack.isIn(this.tag) && NbtHelper.matches(this.compound, itemStack.getNbt(), true);
		}
	}
}
