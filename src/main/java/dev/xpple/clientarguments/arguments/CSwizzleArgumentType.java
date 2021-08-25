package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

public class CSwizzleArgumentType implements ArgumentType<EnumSet<Direction.Axis>> {

	private static final Collection<String> EXAMPLES = Arrays.asList("xyz", "x");
	private static final SimpleCommandExceptionType INVALID_SWIZZLE_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("carguments.swizzle.invalid"));

	public static CSwizzleArgumentType swizzle() {
		return new CSwizzleArgumentType();
	}

	@SuppressWarnings("unchecked")
	public static EnumSet<Direction.Axis> getCSwizzle(final CommandContext<FabricClientCommandSource> context, final String name) {
		return (EnumSet<Direction.Axis>) context.getArgument(name, EnumSet.class);
	}

	@Override
	public EnumSet<Direction.Axis> parse(final StringReader stringReader) throws CommandSyntaxException {
		EnumSet<Direction.Axis> enumSet = EnumSet.noneOf(Direction.Axis.class);

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			char c = stringReader.read();
			Direction.Axis axis4 = switch (c) {
				case 'x' -> Direction.Axis.X;
				case 'y' -> Direction.Axis.Y;
				case 'z' -> Direction.Axis.Z;
				default -> throw INVALID_SWIZZLE_EXCEPTION.create();
			};

			if (enumSet.contains(axis4)) {
				throw INVALID_SWIZZLE_EXCEPTION.create();
			}

			enumSet.add(axis4);
		}

		return enumSet;
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
