package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.critereon.MinMaxBounds;

import java.util.Arrays;
import java.util.Collection;

public interface CRangeArgument<T extends MinMaxBounds<?>> extends ArgumentType<T> {
	static Ints intRange() {
		return new Ints();
	}

	static Floats floatRange() {
		return new Floats();
	}

	class Floats implements CRangeArgument<MinMaxBounds.Doubles> {
		private static final Collection<String> EXAMPLES = Arrays.asList("0..5.2", "0", "-5.4", "-100.76..", "..100");

		public static MinMaxBounds.Doubles getRangeArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
			return context.getArgument(name, MinMaxBounds.Doubles.class);
		}

		@Override
		public MinMaxBounds.Doubles parse(final StringReader stringReader) throws CommandSyntaxException {
			return MinMaxBounds.Doubles.fromReader(stringReader);
		}

		@Override
		public Collection<String> getExamples() {
			return EXAMPLES;
		}
	}

	class Ints implements CRangeArgument<MinMaxBounds.Ints> {
		private static final Collection<String> EXAMPLES = Arrays.asList("0..5", "0", "-5", "-100..", "..100");

		public static MinMaxBounds.Ints getRangeArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
			return context.getArgument(name, MinMaxBounds.Ints.class);
		}

		@Override
		public MinMaxBounds.Ints parse(final StringReader stringReader) throws CommandSyntaxException {
			return MinMaxBounds.Ints.fromReader(stringReader);
		}

		@Override
		public Collection<String> getExamples() {
			return EXAMPLES;
		}
	}
}
