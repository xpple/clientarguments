package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CColumnPosArgumentType implements ArgumentType<CPosArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~1 ~-2", "^ ^", "^-1 ^0");
	public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.pos2d.incomplete"));

	public static CColumnPosArgumentType columnPos() {
		return new CColumnPosArgumentType();
	}

	public static ColumnPos getCColumnPos(final CommandContext<FabricClientCommandSource> context, final String name) {
		BlockPos blockPos = (context.getArgument(name, CPosArgument.class)).toAbsoluteBlockPos(context.getSource());
		return new ColumnPos(blockPos.getX(), blockPos.getZ());
	}

	@Override
	public CPosArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();
		if (stringReader.canRead()) {
			CoordinateArgument coordinateArgument = CoordinateArgument.parse(stringReader);
			if (stringReader.canRead() && stringReader.peek() == ' ') {
				stringReader.skip();
				CoordinateArgument coordinateArgument2 = CoordinateArgument.parse(stringReader);
				return new CDefaultPosArgument(coordinateArgument, new CoordinateArgument(true, 0.0D), coordinateArgument2);
			}
			stringReader.setCursor(cursor);
		}
		throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof CommandSource)) {
			return Suggestions.empty();
		} else {
			String string = builder.getRemaining();
			if (!string.isEmpty() && string.charAt(0) == '^') {
				Set<CommandSource.RelativePosition> singleton = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
				return CommandSource.suggestColumnPositions(string, singleton, builder, CommandManager.getCommandValidator(this::parse));
			}
			Collection<CommandSource.RelativePosition> blockPositionSuggestions = ((CommandSource) context.getSource()).getBlockPositionSuggestions();
			return CommandSource.suggestColumnPositions(string, blockPositionSuggestions, builder, CommandManager.getCommandValidator(this::parse));
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
