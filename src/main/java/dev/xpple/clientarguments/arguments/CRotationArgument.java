package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;

public class CRotationArgument implements ArgumentType<CCoordinates> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~-5 ~5");
	public static final SimpleCommandExceptionType INCOMPLETE_ROTATION_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.rotation.incomplete"));

	public static CRotationArgument rotation() {
		return new CRotationArgument();
	}

	public static CCoordinates getRotation(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CCoordinates.class);
	}

	@Override
	public CCoordinates parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();
		if (!stringReader.canRead()) {
			throw INCOMPLETE_ROTATION_EXCEPTION.createWithContext(stringReader);
		}
		WorldCoordinate worldCoordinate = WorldCoordinate.parseDouble(stringReader, false);
        if (!stringReader.canRead() || stringReader.peek() != ' ') {
            stringReader.setCursor(cursor);
            throw INCOMPLETE_ROTATION_EXCEPTION.createWithContext(stringReader);
        }
		stringReader.skip();
		WorldCoordinate worldCoordinate2 = WorldCoordinate.parseDouble(stringReader, false);
		return new CWorldCoordinates(worldCoordinate2, worldCoordinate, new WorldCoordinate(true, 0.0));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
