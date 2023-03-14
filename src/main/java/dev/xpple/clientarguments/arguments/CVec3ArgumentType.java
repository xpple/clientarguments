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
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CVec3ArgumentType implements ArgumentType<CPosArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
	public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.pos3d.incomplete"));
	public static final SimpleCommandExceptionType MIXED_COORDINATE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.pos.mixed"));
	private final boolean centerIntegers;

	public CVec3ArgumentType(boolean centerIntegers) {
		this.centerIntegers = centerIntegers;
	}

	public static CVec3ArgumentType vec3() {
		return new CVec3ArgumentType(true);
	}

	public static CVec3ArgumentType vec3(boolean centerIntegers) {
		return new CVec3ArgumentType(centerIntegers);
	}

	public static Vec3d getCVec3(CommandContext<FabricClientCommandSource> context, String name) {
		return context.getArgument(name, CPosArgument.class).toAbsolutePos(context.getSource());
	}

	public static CPosArgument getCPosArgument(CommandContext<FabricClientCommandSource> context, String name) {
		return context.getArgument(name, CPosArgument.class);
	}

	@Override
	public CPosArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		return stringReader.canRead() && stringReader.peek() == '^' ? CLookingPosArgument.parse(stringReader) : CDefaultPosArgument.parse(stringReader, this.centerIntegers);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof CommandSource)) {
			return Suggestions.empty();
		} else {
			String string = builder.getRemaining();
			if (!string.isEmpty() && string.charAt(0) == '^') {
				final Set<CommandSource.RelativePosition> singleton = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
				return CommandSource.suggestPositions(string, singleton, builder, CommandManager.getCommandValidator(this::parse));
			} else {
				final Collection<CommandSource.RelativePosition> positionSuggestions = ((CommandSource) context.getSource()).getPositionSuggestions();
				return CommandSource.suggestPositions(string, positionSuggestions, builder, CommandManager.getCommandValidator(this::parse));
			}
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
