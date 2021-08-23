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
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CScoreboardSlotArgumentType implements ArgumentType<Integer> {

	private static final Collection<String> EXAMPLES = Arrays.asList("sidebar", "foo.bar");
	public static final DynamicCommandExceptionType INVALID_SLOT_EXCEPTION = new DynamicCommandExceptionType(name -> new TranslatableText("cargument.scoreboardDisplaySlot.invalid", name));

	public static CScoreboardSlotArgumentType scoreboardSlot() {
		return new CScoreboardSlotArgumentType();
	}

	public static int getScoreboardSlot(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Integer.class);
	}

	@Override
	public Integer parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		int slotId = Scoreboard.getDisplaySlotId(string);
		if (slotId == -1) {
			throw INVALID_SLOT_EXCEPTION.create(string);
		}
		return slotId;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(Scoreboard.getDisplaySlotNames(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
