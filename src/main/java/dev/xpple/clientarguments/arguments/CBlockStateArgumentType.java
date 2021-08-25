package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.tag.BlockTags;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CBlockStateArgumentType implements ArgumentType<ClientBlockArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");

	public static CBlockStateArgumentType blockState() {
		return new CBlockStateArgumentType();
	}

	public static ClientBlockArgument getCBlockState(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ClientBlockArgument.class);
	}

	@Override
	public ClientBlockArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		BlockArgumentParser blockArgumentParser = (new BlockArgumentParser(stringReader, true)).parse(true);
		return new ClientBlockArgument(blockArgumentParser);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		BlockArgumentParser blockArgumentParser = new BlockArgumentParser(stringReader, false);

		try {
			blockArgumentParser.parse(true);
		} catch (CommandSyntaxException ignored) {
		}

		return blockArgumentParser.getSuggestions(builder, BlockTags.getTagGroup());
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
