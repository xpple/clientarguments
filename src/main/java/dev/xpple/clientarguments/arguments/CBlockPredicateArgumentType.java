package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CBlockPredicateArgumentType implements ArgumentType<ClientBlockArgument.ClientBlockPredicate> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
	private static final DynamicCommandExceptionType UNKNOWN_TAG_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("carguments.block.tag.unknown", id));

	private boolean ignoreNbt;

	public CBlockPredicateArgumentType(boolean ignoreNbt) {
		this.ignoreNbt = ignoreNbt;
	}

	public static CBlockPredicateArgumentType blockPredicate() {
		return new CBlockPredicateArgumentType(false);
	}

	public CBlockPredicateArgumentType ignoreNbt() {
		this.ignoreNbt = true;
		return this;
	}

	public static ClientBlockArgument.ClientBlockPredicate getBlockPredicate(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ClientBlockArgument.ClientBlockPredicate.class);
	}

	@Override
	public ClientBlockArgument.ClientBlockPredicate parse(final StringReader stringReader) throws CommandSyntaxException {
		BlockArgumentParser blockArgumentParser = (new BlockArgumentParser(stringReader, true)).parse(true);
		ClientBlockArgument blockArgument = new ClientBlockArgument(blockArgumentParser);
		blockArgument = this.ignoreNbt ? blockArgument.ignoreNbt() : blockArgument;
		return blockArgument::test;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		BlockArgumentParser blockArgumentParser = new BlockArgumentParser(stringReader, true);

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
