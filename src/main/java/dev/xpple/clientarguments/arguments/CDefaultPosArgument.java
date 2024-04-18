package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class CDefaultPosArgument implements CPosArgument {
	private final CoordinateArgument x;
	private final CoordinateArgument y;
	private final CoordinateArgument z;

	public CDefaultPosArgument(CoordinateArgument x, CoordinateArgument y, CoordinateArgument z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Vec3d toAbsolutePos(FabricClientCommandSource source) {
		Vec3d vec3d = source.getPosition();
		return new Vec3d(this.x.toAbsoluteCoordinate(vec3d.x), this.y.toAbsoluteCoordinate(vec3d.y), this.z.toAbsoluteCoordinate(vec3d.z));
	}

	@Override
	public Vec2f toAbsoluteRotation(FabricClientCommandSource source) {
		Vec2f vec2f = source.getRotation();
		return new Vec2f((float) this.x.toAbsoluteCoordinate(vec2f.x), (float) this.y.toAbsoluteCoordinate(vec2f.y));
	}

	@Override
	public boolean isXRelative() {
		return this.x.isRelative();
	}

	@Override
	public boolean isYRelative() {
		return this.y.isRelative();
	}

	@Override
	public boolean isZRelative() {
		return this.z.isRelative();
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CDefaultPosArgument defaultPosArgument)) {
			return false;
		}
        return this.x.equals(defaultPosArgument.x) && this.y.equals(defaultPosArgument.y) && this.z.equals(defaultPosArgument.z);
    }

	public static CDefaultPosArgument parse(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();
		CoordinateArgument coordinateArgument = CoordinateArgument.parse(reader);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			CoordinateArgument coordinateArgument2 = CoordinateArgument.parse(reader);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				CoordinateArgument coordinateArgument3 = CoordinateArgument.parse(reader);
				return new CDefaultPosArgument(coordinateArgument, coordinateArgument2, coordinateArgument3);
			}
        }
        reader.setCursor(cursor);
        throw CVec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
    }

	public static CDefaultPosArgument parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
		int i = reader.getCursor();
		CoordinateArgument coordinateArgument = CoordinateArgument.parse(reader, centerIntegers);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			CoordinateArgument coordinateArgument2 = CoordinateArgument.parse(reader, false);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				CoordinateArgument coordinateArgument3 = CoordinateArgument.parse(reader, centerIntegers);
				return new CDefaultPosArgument(coordinateArgument, coordinateArgument2, coordinateArgument3);
			}
        }
        reader.setCursor(i);
        throw CVec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
    }

	public static CDefaultPosArgument absolute(double x, double y, double z) {
		return new CDefaultPosArgument(new CoordinateArgument(false, x), new CoordinateArgument(false, y), new CoordinateArgument(false, z));
	}

	public static CDefaultPosArgument absolute(Vec2f vec) {
		return new CDefaultPosArgument(new CoordinateArgument(false, vec.x), new CoordinateArgument(false, vec.y), new CoordinateArgument(true, 0.0));
	}

	public static CDefaultPosArgument zero() {
		return new CDefaultPosArgument(new CoordinateArgument(true, 0.0), new CoordinateArgument(true, 0.0), new CoordinateArgument(true, 0.0));
	}

	@Override
	public int hashCode() {
		int i = this.x.hashCode();
		i = 31 * i + this.y.hashCode();
		return 31 * i + this.z.hashCode();
	}
}
