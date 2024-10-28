package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class CLocalCoordinates implements CCoordinates {
	private final double x;
	private final double y;
	private final double z;

	public CLocalCoordinates(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Vec3 getPosition(FabricClientCommandSource source) {
		Vec2 vec2 = source.getRotation();
		Vec3 vec3 = CEntityAnchorArgument.EntityAnchor.FEET.positionAt(source);
		float f = Mth.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
		float g = Mth.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
		float h = Mth.cos(-vec2.x * (float) (Math.PI / 180.0));
		float i = Mth.sin(-vec2.x * (float) (Math.PI / 180.0));
		float j = Mth.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
		float k = Mth.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
		Vec3 vec32 = new Vec3(f * h, i, g * h);
		Vec3 vec33 = new Vec3(f * j, k, g * j);
		Vec3 vec34 = vec32.cross(vec33).scale(-1.0);
		double d = vec32.x * this.z + vec33.x * this.y + vec34.x * this.x;
		double e = vec32.y * this.z + vec33.y * this.y + vec34.y * this.x;
		double l = vec32.z * this.z + vec33.z * this.y + vec34.z * this.x;
		return new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
	}

	@Override
	public Vec2 getRotation(FabricClientCommandSource source) {
		return Vec2.ZERO;
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

	public static CLocalCoordinates parse(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();
		double d = readCoordinate(reader, cursor);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw CVec3Argument.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }
		reader.skip();
		double e = readCoordinate(reader, cursor);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw CVec3Argument.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }
		reader.skip();
		double f = readCoordinate(reader, cursor);
		return new CLocalCoordinates(d, e, f);
    }

	private static double readCoordinate(StringReader reader, int startingCursorPos) throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
		}
		if (reader.peek() != '^') {
			reader.setCursor(startingCursorPos);
			throw CVec3Argument.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
		}
		reader.skip();
		return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CLocalCoordinates localCoordinates)) {
			return false;
		}
		return this.x == localCoordinates.x && this.y == localCoordinates.y && this.z == localCoordinates.z;
	}

	public int hashCode() {
		return Objects.hash(this.x, this.y, this.z);
	}
}
