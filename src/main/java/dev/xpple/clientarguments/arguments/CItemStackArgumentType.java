package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CItemStackArgumentType implements ArgumentType<ItemStackArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");

	public static CItemStackArgumentType itemStack() {
		return new CItemStackArgumentType();
	}

	@Override
	public ItemStackArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		ItemStringReader itemStringReader = (new ItemStringReader(stringReader, false)).consume();
		return new ItemStackArgument(itemStringReader.getItem(), itemStringReader.getNbt());
	}

	public static <S> ItemStackArgument getCItemStackArgument(CommandContext<S> context, String name) {
		return context.getArgument(name, ItemStackArgument.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		ItemStringReader itemStringReader = new ItemStringReader(stringReader, false);
		try {
			itemStringReader.consume();
		} catch (CommandSyntaxException ignored) {
		}
		return itemStringReader.getSuggestions(builder, Registry.ITEM);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
