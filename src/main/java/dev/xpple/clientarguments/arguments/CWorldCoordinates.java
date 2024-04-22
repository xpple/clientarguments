package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CWorldCoordinates implements CCordinates {
	private final WorldCoordinate x;
	private final WorldCoordinate y;
	private final WorldCoordinate z;

	public CWorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Vec3 getPosition(FabricClientCommandSource source) {
		Vec3 vec3 = source.getPosition();
		return new Vec3(this.x.get(vec3.x), this.y.get(vec3.y), this.z.get(vec3.z));
	}

	@Override
	public Vec2 getRotation(FabricClientCommandSource source) {
		Vec2 vec2 = source.getRotation();
		return new Vec2((float) this.x.get(vec2.x), (float) this.y.get(vec2.y));
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
		if (!(o instanceof CWorldCoordinates defaultPosArgument)) {
			return false;
		}
        return this.x.equals(defaultPosArgument.x) && this.y.equals(defaultPosArgument.y) && this.z.equals(defaultPosArgument.z);
    }

	public static CWorldCoordinates parse(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();
		WorldCoordinate worldCoordinate = WorldCoordinate.parseInt(reader);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			WorldCoordinate worldCoordinate2 = WorldCoordinate.parseInt(reader);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				WorldCoordinate worldCoordinate3 = WorldCoordinate.parseInt(reader);
				return new CWorldCoordinates(worldCoordinate, worldCoordinate2, worldCoordinate3);
			}
        }
        reader.setCursor(cursor);
        throw CVec3Argument.INCOMPLETE_EXCEPTION.createWithContext(reader);
    }

	public static CWorldCoordinates parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
		int i = reader.getCursor();
		WorldCoordinate worldCoordinate = WorldCoordinate.parseDouble(reader, centerIntegers);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			WorldCoordinate worldCoordinate2 = WorldCoordinate.parseDouble(reader, false);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				WorldCoordinate worldCoordinate3 = WorldCoordinate.parseDouble(reader, centerIntegers);
				return new CWorldCoordinates(worldCoordinate, worldCoordinate2, worldCoordinate3);
			}
        }
        reader.setCursor(i);
        throw CVec3Argument.INCOMPLETE_EXCEPTION.createWithContext(reader);
    }

	public static CWorldCoordinates absolute(double x, double y, double z) {
		return new CWorldCoordinates(new WorldCoordinate(false, x), new WorldCoordinate(false, y), new WorldCoordinate(false, z));
	}

	public static CWorldCoordinates absolute(Vec2 vec) {
		return new CWorldCoordinates(new WorldCoordinate(false, vec.x), new WorldCoordinate(false, vec.y), new WorldCoordinate(true, 0.0));
	}

	public static CWorldCoordinates current() {
		return new CWorldCoordinates(new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0));
	}

	@Override
	public int hashCode() {
		int i = this.x.hashCode();
		i = 31 * i + this.y.hashCode();
		return 31 * i + this.z.hashCode();
	}
}
