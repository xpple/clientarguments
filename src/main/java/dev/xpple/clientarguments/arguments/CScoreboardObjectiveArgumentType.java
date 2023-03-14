package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CScoreboardObjectiveArgumentType implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "*", "012");
	private static final DynamicCommandExceptionType UNKNOWN_OBJECTIVE_EXCEPTION = new DynamicCommandExceptionType(name -> Text.translatable("arguments.objective.notFound", name));
	private static final DynamicCommandExceptionType READONLY_OBJECTIVE_EXCEPTION = new DynamicCommandExceptionType(name -> Text.translatable("arguments.objective.readonly", name));

	public static CScoreboardObjectiveArgumentType scoreboardObjective() {
		return new CScoreboardObjectiveArgumentType();
	}

	public static ScoreboardObjective getCObjective(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		String string = context.getArgument(name, String.class);
		Scoreboard scoreboard = context.getSource().getWorld().getScoreboard();
		ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(string);
		if (scoreboardObjective == null) {
			throw UNKNOWN_OBJECTIVE_EXCEPTION.create(string);
		}
		return scoreboardObjective;
	}

	public static ScoreboardObjective getCWritableObjective(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		ScoreboardObjective scoreboardObjective = getCObjective(context, name);
		if (scoreboardObjective.getCriterion().isReadOnly()) {
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
		S source = context.getSource();
		if (source instanceof FabricClientCommandSource fabricSource) {
			return CommandSource.suggestMatching(fabricSource.getWorld().getScoreboard().getObjectiveNames(), builder);
		}
		if (source instanceof CommandSource commandSource) {
			return commandSource.getCompletions(context);
		}
		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
