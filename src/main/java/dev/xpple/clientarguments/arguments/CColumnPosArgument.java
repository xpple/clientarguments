package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CColumnPosArgument implements ArgumentType<CCordinates> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~1 ~-2", "^ ^", "^-1 ^0");
	public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.pos2d.incomplete"));

	public static CColumnPosArgument columnPos() {
		return new CColumnPosArgument();
	}

	public static ColumnPos getColumnPos(final CommandContext<FabricClientCommandSource> context, final String name) {
		BlockPos blockPos = context.getArgument(name, CCordinates.class).getBlockPos(context.getSource());
		return new ColumnPos(blockPos.getX(), blockPos.getZ());
	}

	@Override
	public CCordinates parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();
		if (!stringReader.canRead()) {
			throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
		}
		WorldCoordinate worldCoord = WorldCoordinate.parseInt(stringReader);
        if (!stringReader.canRead() || stringReader.peek() != ' ') {
            stringReader.setCursor(cursor);
            throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
        }
		stringReader.skip();
		WorldCoordinate worldCoord2 = WorldCoordinate.parseInt(stringReader);
		return new CWorldCoordinates(worldCoord, new WorldCoordinate(true, 0.0), worldCoord2);
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
			collection = ((SharedSuggestionProvider)context.getSource()).getRelevantCoordinates();
		}

		return SharedSuggestionProvider.suggest2DCoordinates(string, collection, builder, Commands.createValidator(this::parse));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
