package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CScoreboardSlotArgumentType implements ArgumentType<ScoreboardDisplaySlot> {

	private static final Collection<String> EXAMPLES = Arrays.asList("sidebar", "foo.bar");
	public static final DynamicCommandExceptionType INVALID_SLOT_EXCEPTION = new DynamicCommandExceptionType(name -> Text.translatable("argument.scoreboardDisplaySlot.invalid", name));

	public static CScoreboardSlotArgumentType scoreboardSlot() {
		return new CScoreboardSlotArgumentType();
	}

	public static ScoreboardDisplaySlot getCScoreboardSlot(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ScoreboardDisplaySlot.class);
	}

	@Override
	public ScoreboardDisplaySlot parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		ScoreboardDisplaySlot slotId = ScoreboardDisplaySlot.CODEC.byId(string);
		if (slotId == null) {
			throw INVALID_SLOT_EXCEPTION.create(string);
		}
		return slotId;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(Arrays.stream(ScoreboardDisplaySlot.values()).map(ScoreboardDisplaySlot::asString), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
