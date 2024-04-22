package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.Arrays;
import java.util.Collection;

public class CCompoundTagArgument implements ArgumentType<CompoundTag> {
	private static final Collection<String> EXAMPLES = Arrays.asList("{}", "{foo=bar}");

	private CCompoundTagArgument() {
	}

	public static CCompoundTagArgument compoundTag() {
		return new CCompoundTagArgument();
	}

	public static <S> CompoundTag getCompoundTag(final CommandContext<S> context, final String name) {
		return context.getArgument(name, CompoundTag.class);
	}

	@Override
	public CompoundTag parse(final StringReader stringReader) throws CommandSyntaxException {
		return new TagParser(stringReader).readStruct();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
