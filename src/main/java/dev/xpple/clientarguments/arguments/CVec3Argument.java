package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CVec3Argument implements ArgumentType<CCoordinates> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
	public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos3d.incomplete"));
	public static final SimpleCommandExceptionType MIXED_COORDINATE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos.mixed"));
	private final boolean centerIntegers;

	public CVec3Argument(boolean centerIntegers) {
		this.centerIntegers = centerIntegers;
	}

	public static CVec3Argument vec3() {
		return new CVec3Argument(true);
	}

	public static CVec3Argument vec3(boolean centerIntegers) {
		return new CVec3Argument(centerIntegers);
	}

	public static Vec3 getVec3(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CCoordinates.class).getPosition(context.getSource());
	}

	public static CCoordinates getPosArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CCoordinates.class);
	}

	@Override
	public CCoordinates parse(final StringReader stringReader) throws CommandSyntaxException {
		return stringReader.canRead() && stringReader.peek() == '^' ? CLocalCoordinates.parse(stringReader) : CWorldCoordinates.parse(stringReader, this.centerIntegers);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof SharedSuggestionProvider)) {
			return Suggestions.empty();
		}
		String string = builder.getRemaining();
		Collection<SharedSuggestionProvider.TextCoordinates> collection;
		if (!string.isEmpty() && string.charAt(0) == '^') {
			collection = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
		} else {
			collection = ((SharedSuggestionProvider) context.getSource()).getAbsoluteCoordinates();
		}

		return SharedSuggestionProvider.suggestCoordinates(string, collection, builder, Commands.createValidator(this::parse));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
