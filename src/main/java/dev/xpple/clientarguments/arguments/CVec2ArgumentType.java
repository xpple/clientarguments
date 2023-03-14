package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CVec2ArgumentType implements ArgumentType<CPosArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "0.1 -0.5", "~1 ~-2");
	public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.pos2d.incomplete"));
	private final boolean centerIntegers;

	public CVec2ArgumentType(boolean centerIntegers) {
		this.centerIntegers = centerIntegers;
	}

	public static CVec2ArgumentType vec2() {
		return vec2(true);
	}

	public static CVec2ArgumentType vec2(boolean centerIntegers) {
		return new CVec2ArgumentType(centerIntegers);
	}

	public static Vec2f getCVec2(CommandContext<FabricClientCommandSource> context, String name) {
		Vec3d vec3d = context.getArgument(name, CPosArgument.class).toAbsolutePos(context.getSource());
		return new Vec2f((float)vec3d.x, (float)vec3d.z);
	}

	@Override
	public CPosArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();
		if (!stringReader.canRead()) {
			throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
		} else {
			CoordinateArgument coordinateArgument = CoordinateArgument.parse(stringReader, this.centerIntegers);
			if (stringReader.canRead() && stringReader.peek() == ' ') {
				stringReader.skip();
				CoordinateArgument coordinateArgument2 = CoordinateArgument.parse(stringReader, this.centerIntegers);
				return new CDefaultPosArgument(coordinateArgument, new CoordinateArgument(true, 0.0D), coordinateArgument2);
			} else {
				stringReader.setCursor(cursor);
				throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
			}
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof CommandSource)) {
			return Suggestions.empty();
		} else {
			String string = builder.getRemaining();
			if (!string.isEmpty() && string.charAt(0) == '^') {
				final Set<CommandSource.RelativePosition> singleton = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
				return CommandSource.suggestColumnPositions(string, singleton, builder, CommandManager.getCommandValidator(this::parse));
			} else {
				final Collection<CommandSource.RelativePosition> positionSuggestions = ((CommandSource) context.getSource()).getPositionSuggestions();
				return CommandSource.suggestColumnPositions(string, positionSuggestions, builder, CommandManager.getCommandValidator(this::parse));
			}
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
