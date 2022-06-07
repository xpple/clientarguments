package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandRegistryWrapper;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CBlockStateArgumentType implements ArgumentType<ClientBlockArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");

	private final CommandRegistryWrapper<Block> registryWrapper;

	protected CBlockStateArgumentType(CommandRegistryAccess registryAccess) {
		this.registryWrapper = registryAccess.createWrapper(Registry.BLOCK_KEY);
	}

	public static CBlockStateArgumentType blockState(CommandRegistryAccess registryAccess) {
		return new CBlockStateArgumentType(registryAccess);
	}

	public static ClientBlockArgument getCBlockState(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ClientBlockArgument.class);
	}

	@Override
	public ClientBlockArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		var result = BlockArgumentParser.block(this.registryWrapper, stringReader, true);
		return new ClientBlockArgument(result);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return BlockArgumentParser.getSuggestions(this.registryWrapper, builder, false, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
