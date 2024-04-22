package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

public class CSwizzleArgument implements ArgumentType<EnumSet<Direction.Axis>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("xyz", "x");
	private static final SimpleCommandExceptionType INVALID_SWIZZLE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("arguments.swizzle.invalid"));

	public static CSwizzleArgument swizzle() {
		return new CSwizzleArgument();
	}

	@SuppressWarnings("unchecked")
    public static EnumSet<Direction.Axis> getSwizzle(final CommandContext<FabricClientCommandSource> context, final String name) {
		return (EnumSet<Direction.Axis>) context.getArgument(name, EnumSet.class);
	}

	@Override
	public EnumSet<Direction.Axis> parse(final StringReader stringReader) throws CommandSyntaxException {
		EnumSet<Direction.Axis> enumSet = EnumSet.noneOf(Direction.Axis.class);

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			char c = stringReader.read();

			Direction.Axis axis = switch(c) {
				case 'x' -> Direction.Axis.X;
				case 'y' -> Direction.Axis.Y;
				case 'z' -> Direction.Axis.Z;
				default -> throw INVALID_SWIZZLE_EXCEPTION.createWithContext(stringReader);
			};
			if (enumSet.contains(axis)) {
				throw INVALID_SWIZZLE_EXCEPTION.createWithContext(stringReader);
			}

			enumSet.add(axis);
		}

		return enumSet;
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
