package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;

import java.util.Arrays;
import java.util.Collection;

public class CNbtElementArgumentType implements ArgumentType<NbtElement> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0", "0b", "0l", "0.0", "\"foo\"", "{foo=bar}", "[0]");

	private CNbtElementArgumentType() {
	}

	public static CNbtElementArgumentType nbtElement() {
		return new CNbtElementArgumentType();
	}

	public static <S> NbtElement getNbtElement(final CommandContext<S> context, final String name) {
		return context.getArgument(name, NbtElement.class);
	}

	@Override
	public NbtElement parse(final StringReader stringReader) throws CommandSyntaxException {
		return new StringNbtReader(stringReader).parseElement();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
