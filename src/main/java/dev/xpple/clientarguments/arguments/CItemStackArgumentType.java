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
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CItemStackArgumentType implements ArgumentType<ItemStackArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");

	private final RegistryWrapper<Item> registryWrapper;

	public CItemStackArgumentType(CommandRegistryAccess registryAccess) {
		this.registryWrapper = registryAccess.createWrapper(RegistryKeys.ITEM);
	}

	public static CItemStackArgumentType itemStack(CommandRegistryAccess registryAccess) {
		return new CItemStackArgumentType(registryAccess);
	}

	@Override
	public ItemStackArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		var result = ItemStringReader.item(this.registryWrapper, stringReader);
		return new ItemStackArgument(result.item(), result.nbt());
	}

	public static <S> ItemStackArgument getCItemStackArgument(CommandContext<S> context, String name) {
		return context.getArgument(name, ItemStackArgument.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return ItemStringReader.getSuggestions(this.registryWrapper, builder, false);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
