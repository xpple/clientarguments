package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;

import java.util.Arrays;
import java.util.Collection;

public class CNbtTagArgument extends ParserBasedArgument<Tag> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0", "0b", "0l", "0.0", "\"foo\"", "{foo=bar}", "[0]");
	private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(NbtOps.INSTANCE);

	private CNbtTagArgument() {
		super(TAG_PARSER);
	}

	public static CNbtTagArgument nbtTag() {
		return new CNbtTagArgument();
	}

	public static <S> Tag getNbtTag(final CommandContext<S> context, final String name) {
		return context.getArgument(name, Tag.class);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
