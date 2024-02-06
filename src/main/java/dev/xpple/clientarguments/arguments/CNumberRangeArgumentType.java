package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.predicate.NumberRange;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.Arrays;
import java.util.Collection;

public interface CNumberRangeArgumentType<T extends NumberRange<?>> extends ArgumentType<T> {

	static IntRangeArgumentType intRange() {
		return new IntRangeArgumentType();
	}

	static FloatRangeArgumentType floatRange() {
		return new FloatRangeArgumentType();
	}

	class IntRangeArgumentType implements CNumberRangeArgumentType<NumberRange.IntRange> {

		private static final Collection<String> EXAMPLES = Arrays.asList("0..5", "0", "-5", "-100..", "..100");

		public static NumberRange.IntRange getCRangeArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
			return context.getArgument(name, NumberRange.IntRange.class);
		}

		@Override
		public NumberRange.IntRange parse(final StringReader stringReader) throws CommandSyntaxException {
			return NumberRange.IntRange.parse(stringReader);
		}

		@Override
		public Collection<String> getExamples() {
			return EXAMPLES;
		}
	}

	class FloatRangeArgumentType implements CNumberRangeArgumentType<NumberRange.DoubleRange> {

		private static final Collection<String> EXAMPLES = Arrays.asList("0..5.2", "0", "-5.4", "-100.76..", "..100");

		public static NumberRange.DoubleRange getCRangeArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
			return context.getArgument(name, NumberRange.DoubleRange.class);
		}

		@Override
		public NumberRange.DoubleRange parse(final StringReader stringReader) throws CommandSyntaxException {
			return NumberRange.DoubleRange.parse(stringReader);
		}

		@Override
		public Collection<String> getExamples() {
			return EXAMPLES;
		}
	}
}
