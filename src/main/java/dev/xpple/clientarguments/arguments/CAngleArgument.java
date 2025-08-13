package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Collection;

public class CAngleArgument implements ArgumentType<CAngleArgument.Angle> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0", "~", "~-5");
	public static final SimpleCommandExceptionType INCOMPLETE_ANGLE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.angle.incomplete"));
	public static final SimpleCommandExceptionType INVALID_ANGLE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.angle.invalid"));

	public static CAngleArgument angle() {
		return new CAngleArgument();
	}

	public static float getAngle(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Angle.class).getAngle(context.getSource());
	}

	@Override
	public Angle parse(final StringReader stringReader) throws CommandSyntaxException {
		if (!stringReader.canRead()) {
			throw INCOMPLETE_ANGLE_EXCEPTION.createWithContext(stringReader);
		}
		boolean relative = WorldCoordinate.isRelative(stringReader);
		float angle = stringReader.canRead() && stringReader.peek() != ' ' ? stringReader.readFloat() : 0.0F;
        if (Float.isNaN(angle) || Float.isInfinite(angle)) {
            throw INVALID_ANGLE_EXCEPTION.createWithContext(stringReader);
        }
		return new Angle(angle, relative);
    }

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static final class Angle {
		private final float angle;
		private final boolean relative;

		Angle(float angle, boolean relative) {
			this.angle = angle;
			this.relative = relative;
		}

		public float getAngle(FabricClientCommandSource source) {
			return Mth.wrapDegrees(this.relative ? this.angle + source.getRotation().y : this.angle);
		}
	}
}
