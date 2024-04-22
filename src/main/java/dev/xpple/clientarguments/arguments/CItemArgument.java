package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CItemArgument implements ArgumentType<ItemInput> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");
	private final ItemParser reader;

	public CItemArgument(CommandBuildContext commandBuildContext) {
		this.reader = new ItemParser(commandBuildContext);
	}

	public static CItemArgument itemStack(CommandBuildContext commandBuildContext) {
		return new CItemArgument(commandBuildContext);
	}

	public static <S> ItemInput getItemStackArgument(final CommandContext<S> context, final String name) {
		return context.getArgument(name, ItemInput.class);
	}

	@Override
	public ItemInput parse(final StringReader stringReader) throws CommandSyntaxException {
		ItemParser.ItemResult itemResult = this.reader.parse(stringReader);
		return new ItemInput(itemResult.item(), itemResult.components());
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return this.reader.fillSuggestions(builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
