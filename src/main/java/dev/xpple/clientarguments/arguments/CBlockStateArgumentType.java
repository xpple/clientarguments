package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

public class CBlockStateArgumentType implements ArgumentType<CBlockStateArgument> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");
	private final RegistryWrapper<Block> registryWrapper;

	public CBlockStateArgumentType(CommandRegistryAccess commandRegistryAccess) {
		this.registryWrapper = commandRegistryAccess.getWrapperOrThrow(RegistryKeys.BLOCK);
	}

	public static CBlockStateArgumentType blockState(CommandRegistryAccess commandRegistryAccess) {
		return new CBlockStateArgumentType(commandRegistryAccess);
	}

	@Override
	public CBlockStateArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		BlockArgumentParser.BlockResult blockResult = BlockArgumentParser.block(this.registryWrapper, stringReader, true);
		return new CBlockStateArgument(blockResult.blockState(), blockResult.properties().keySet(), blockResult.nbt());
	}

	public static CBlockStateArgument getBlockState(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CBlockStateArgument.class);
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
