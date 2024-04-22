package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

import java.util.Arrays;
import java.util.Collection;

public class CNbtTagArgument implements ArgumentType<Tag> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0", "0b", "0l", "0.0", "\"foo\"", "{foo=bar}", "[0]");

	private CNbtTagArgument() {
	}

	public static CNbtTagArgument nbtTag() {
		return new CNbtTagArgument();
	}

	public static <S> Tag getNbtTag(final CommandContext<S> context, final String name) {
		return context.getArgument(name, Tag.class);
	}

	@Override
	public Tag parse(final StringReader stringReader) throws CommandSyntaxException {
		return new TagParser(stringReader).readValue();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
