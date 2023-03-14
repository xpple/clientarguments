package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;

import java.util.Arrays;
import java.util.Locale;

public class CHeightmapArgumentType extends EnumArgumentType<Type> {

    private static final Codec<Heightmap.Type> HEIGHTMAP_CODEC = StringIdentifiable.createCodec(CHeightmapArgumentType::getHeightmapTypes, (name) -> name.toLowerCase(Locale.ROOT));

    private static Heightmap.Type[] getHeightmapTypes() {
        return Arrays.stream(Type.values()).filter(Type::isStoredServerSide).toArray(Type[]::new);
    }

    private CHeightmapArgumentType() {
        super(HEIGHTMAP_CODEC, CHeightmapArgumentType::getHeightmapTypes);
    }

    public static CHeightmapArgumentType heightmap() {
        return new CHeightmapArgumentType();
    }

    public static Heightmap.Type getCHeightmap(final CommandContext<FabricClientCommandSource> context, final String id) {
        return context.getArgument(id, Type.class);
    }

    protected String transformValueName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
