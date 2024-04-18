package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class CLookingPosArgument implements CPosArgument {
	private final double x;
	private final double y;
	private final double z;

	public CLookingPosArgument(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Vec3d toAbsolutePos(FabricClientCommandSource source) {
		Vec2f vec2f = source.getRotation();
		Vec3d vec3d = CEntityAnchorArgumentType.EntityAnchor.FEET.positionAt(source);
		float f = MathHelper.cos((vec2f.y + 90.0F) * (float) (Math.PI / 180.0));
		float g = MathHelper.sin((vec2f.y + 90.0F) * (float) (Math.PI / 180.0));
		float h = MathHelper.cos(-vec2f.x * (float) (Math.PI / 180.0));
		float i = MathHelper.sin(-vec2f.x * (float) (Math.PI / 180.0));
		float j = MathHelper.cos((-vec2f.x + 90.0F) * (float) (Math.PI / 180.0));
		float k = MathHelper.sin((-vec2f.x + 90.0F) * (float) (Math.PI / 180.0));
		Vec3d vec3d2 = new Vec3d(f * h, i, g * h);
		Vec3d vec3d3 = new Vec3d(f * j, k, g * j);
		Vec3d vec3d4 = vec3d2.crossProduct(vec3d3).multiply(-1.0);
		double d = vec3d2.x * this.z + vec3d3.x * this.y + vec3d4.x * this.x;
		double e = vec3d2.y * this.z + vec3d3.y * this.y + vec3d4.y * this.x;
		double l = vec3d2.z * this.z + vec3d3.z * this.y + vec3d4.z * this.x;
		return new Vec3d(vec3d.x + d, vec3d.y + e, vec3d.z + l);
	}

	@Override
	public Vec2f toAbsoluteRotation(FabricClientCommandSource source) {
		return Vec2f.ZERO;
	}

	@Override
	public boolean isXRelative() {
		return true;
	}

	@Override
	public boolean isYRelative() {
		return true;
	}

	@Override
	public boolean isZRelative() {
		return true;
	}

	public static CLookingPosArgument parse(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();
		double d = readCoordinate(reader, cursor);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }
		reader.skip();
		double e = readCoordinate(reader, cursor);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }
		reader.skip();
		double f = readCoordinate(reader, cursor);
		return new CLookingPosArgument(d, e, f);
    }

	private static double readCoordinate(StringReader reader, int startingCursorPos) throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
		}
		if (reader.peek() != '^') {
			reader.setCursor(startingCursorPos);
			throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
		}
		reader.skip();
		return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CLookingPosArgument lookingPosArgument)) {
			return false;
		}
		return this.x == lookingPosArgument.x && this.y == lookingPosArgument.y && this.z == lookingPosArgument.z;
	}

	public int hashCode() {
		return Objects.hash(this.x, this.y, this.z);
	}
}
