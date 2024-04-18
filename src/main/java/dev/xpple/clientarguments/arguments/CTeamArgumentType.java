package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTeamArgumentType implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "123");
	private static final DynamicCommandExceptionType UNKNOWN_TEAM_EXCEPTION = new DynamicCommandExceptionType(name -> Text.stringifiedTranslatable("team.notFound", name));

	public static CTeamArgumentType team() {
		return new CTeamArgumentType();
	}

	public static Team getTeam(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		String string = context.getArgument(name, String.class);
		Scoreboard scoreboard = context.getSource().getWorld().getScoreboard();
		Team team = scoreboard.getTeam(string);
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
		return context.getSource() instanceof CommandSource ? CommandSource.suggestMatching(((CommandSource)context.getSource()).getTeamNames(), builder) : Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
