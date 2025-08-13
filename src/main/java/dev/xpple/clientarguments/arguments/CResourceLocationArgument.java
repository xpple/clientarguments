package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;

public class CResourceLocationArgument implements ArgumentType<ResourceLocation> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

	public static CResourceLocationArgument id() {
		return new CResourceLocationArgument();
	}

	public static ResourceLocation getId(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ResourceLocation.class);
	}

	@Override
	public ResourceLocation parse(final StringReader stringReader) throws CommandSyntaxException {
		return ResourceLocation.read(stringReader);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
