package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStringReader;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CItemStackArgumentType implements ArgumentType<ItemStackArgument> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");
	private final ItemStringReader reader;

	public CItemStackArgumentType(CommandRegistryAccess commandRegistryAccess) {
		this.reader = new ItemStringReader(commandRegistryAccess);
	}

	public static CItemStackArgumentType itemStack(CommandRegistryAccess commandRegistryAccess) {
		return new CItemStackArgumentType(commandRegistryAccess);
	}

	public static <S> ItemStackArgument getItemStackArgument(final CommandContext<S> context, final String name) {
		return context.getArgument(name, ItemStackArgument.class);
	}

	@Override
	public ItemStackArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		ItemStringReader.ItemResult itemResult = this.reader.consume(stringReader);
		return new ItemStackArgument(itemResult.item(), itemResult.components());
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return this.reader.getSuggestions(builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
