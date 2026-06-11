package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TeamColorArgument;
import net.minecraft.world.scores.TeamColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTeamColorArgument implements ArgumentType<TeamColor> {
    private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");

    private CTeamColorArgument() {
    }

    public static CTeamColorArgument teamColor() {
        return new CTeamColorArgument();
    }

    public static TeamColor getTeamColor(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, TeamColor.class);
    }

    public TeamColor parse(final StringReader reader) throws CommandSyntaxException {
        String id = reader.readUnquotedString();
        TeamColor result = TeamColor.byName(id);
        if (result == null) {
            throw TeamColorArgument.ERROR_INVALID_VALUE.createWithContext(reader, id);
        } else {
            return result;
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(TeamColor.VALUES.stream().map(TeamColor::getSerializedName), builder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
