package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CObjectiveArgument implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "*", "012");
	private static final DynamicCommandExceptionType UNKNOWN_OBJECTIVE_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("arguments.objective.notFound", name));
	private static final DynamicCommandExceptionType READONLY_OBJECTIVE_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("arguments.objective.readonly", name));

	public static CObjectiveArgument objective() {
		return new CObjectiveArgument();
	}

	public static Objective getObjective(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		String string = context.getArgument(name, String.class);
		Scoreboard scoreboard = context.getSource().getWorld().getScoreboard();
		Objective objective = scoreboard.getObjective(string);
		if (objective == null) {
			throw UNKNOWN_OBJECTIVE_EXCEPTION.create(string);
		}
		return objective;
	}

	public static Objective getWritableObjective(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Objective scoreboardObjective = getObjective(context, name);
		if (scoreboardObjective.getCriteria().isReadOnly()) {
			throw READONLY_OBJECTIVE_EXCEPTION.create(scoreboardObjective.getName());
		}
		return scoreboardObjective;
	}

	@Override
	public String parse(final StringReader stringReader) throws CommandSyntaxException {
		return stringReader.readUnquotedString();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (context.getSource() instanceof FabricClientCommandSource fabricClientCommandSource) {
			return SharedSuggestionProvider.suggest(fabricClientCommandSource.getWorld().getScoreboard().getObjectiveNames(), builder);
		} else {
			return context.getSource() instanceof SharedSuggestionProvider commandSource ? commandSource.customSuggestion(context) : Suggestions.empty();
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
