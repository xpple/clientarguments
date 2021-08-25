package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.argument.AngleArgumentType;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Collection;

public class CAngleArgumentType implements ArgumentType<CAngleArgumentType.Angle> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0", "~", "~-5");
	public static final SimpleCommandExceptionType INCOMPLETE_ANGLE_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.angle.incomplete"));
	public static final SimpleCommandExceptionType INVALID_ANGLE_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.angle.invalid"));

	public static CAngleArgumentType angle() {
		return new CAngleArgumentType();
	}

	public static float getCAngle(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Angle.class).getAngle(context.getSource());
	}

	@Override
	public Angle parse(final StringReader stringReader) throws CommandSyntaxException {
		if (!stringReader.canRead()) {
			throw INCOMPLETE_ANGLE_EXCEPTION.createWithContext(stringReader);
		}
		boolean relative = CoordinateArgument.isRelative(stringReader);
		float angle = stringReader.canRead() && stringReader.peek() != ' ' ? stringReader.readFloat() : 0.0F;
		if (!Float.isNaN(angle) && !Float.isInfinite(angle)) {
			return new Angle(angle, relative);
		}
		throw INVALID_ANGLE_EXCEPTION.createWithContext(stringReader);
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
			return MathHelper.wrapDegrees(this.relative ? this.angle + source.getRotation().y : this.angle);
		}
	}
}
