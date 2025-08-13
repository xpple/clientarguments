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
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTeamArgument implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "123");
	private static final DynamicCommandExceptionType UNKNOWN_TEAM_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("team.notFound", name));

	public static CTeamArgument team() {
		return new CTeamArgument();
	}

	public static PlayerTeam getTeam(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		String string = context.getArgument(name, String.class);
		Scoreboard scoreboard = context.getSource().getWorld().getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam(string);
		if (team == null) {
			throw UNKNOWN_TEAM_EXCEPTION.create(string);
		}
		return team;
	}

	@Override
	public String parse(final StringReader stringReader) throws CommandSyntaxException {
		return stringReader.readUnquotedString();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return context.getSource() instanceof SharedSuggestionProvider ? SharedSuggestionProvider.suggest(((SharedSuggestionProvider)context.getSource()).getAllTeams(), builder) : Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
