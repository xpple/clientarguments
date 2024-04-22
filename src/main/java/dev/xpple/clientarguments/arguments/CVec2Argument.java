package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CVec2Argument implements ArgumentType<CCordinates> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "0.1 -0.5", "~1 ~-2");
	public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos2d.incomplete"));
	private final boolean centerIntegers;

	public CVec2Argument(boolean centerIntegers) {
		this.centerIntegers = centerIntegers;
	}

	public static CVec2Argument vec2() {
		return new CVec2Argument(true);
	}

	public static CVec2Argument vec2(boolean centerIntegers) {
		return new CVec2Argument(centerIntegers);
	}

	public static Vec2 getVec2(final CommandContext<FabricClientCommandSource> context, final String name) {
		Vec3 vec3d = context.getArgument(name, CCordinates.class).getPosition(context.getSource());
		return new Vec2((float) vec3d.x, (float) vec3d.z);
	}

	@Override
	public CCordinates parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();
		if (!stringReader.canRead()) {
			throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
		}
		WorldCoordinate worldCoordinate = WorldCoordinate.parseDouble(stringReader, this.centerIntegers);
        if (!stringReader.canRead() || stringReader.peek() != ' ') {
            stringReader.setCursor(cursor);
            throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
        }
		stringReader.skip();
		WorldCoordinate worldCoordinate2 = WorldCoordinate.parseDouble(stringReader, this.centerIntegers);
		return new CWorldCoordinates(worldCoordinate, new WorldCoordinate(true, 0.0), worldCoordinate2);
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

		return SharedSuggestionProvider.suggest2DCoordinates(string, collection, builder, Commands.createValidator(this::parse));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
