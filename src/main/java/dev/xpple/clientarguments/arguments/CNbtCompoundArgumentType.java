package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;

import java.util.Arrays;
import java.util.Collection;

public class CNbtCompoundArgumentType implements ArgumentType<NbtCompound> {
	private static final Collection<String> EXAMPLES = Arrays.asList("{}", "{foo=bar}");

	private CNbtCompoundArgumentType() {
	}

	public static CNbtCompoundArgumentType nbtCompound() {
		return new CNbtCompoundArgumentType();
	}

	public static <S> NbtCompound getNbtCompound(final CommandContext<S> context, final String name) {
		return context.getArgument(name, NbtCompound.class);
	}

	@Override
	public NbtCompound parse(final StringReader stringReader) throws CommandSyntaxException {
		return new StringNbtReader(stringReader).parseCompound();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
