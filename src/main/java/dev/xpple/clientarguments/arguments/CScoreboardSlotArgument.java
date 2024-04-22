package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CScoreboardSlotArgument implements ArgumentType<DisplaySlot> {
	private static final Collection<String> EXAMPLES = Arrays.asList("sidebar", "foo.bar");
	public static final DynamicCommandExceptionType INVALID_SLOT_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("argument.scoreboardDisplaySlot.invalid", name));

	private CScoreboardSlotArgument() {
	}

	public static CScoreboardSlotArgument scoreboardSlot() {
		return new CScoreboardSlotArgument();
	}

	public static DisplaySlot getScoreboardSlot(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, DisplaySlot.class);
	}

	@Override
	public DisplaySlot parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		DisplaySlot displaySlot = DisplaySlot.CODEC.byName(string);
		if (displaySlot == null) {
			throw INVALID_SLOT_EXCEPTION.createWithContext(stringReader, string);
		}
		return displaySlot;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Arrays.stream(DisplaySlot.values()).map(DisplaySlot::getSerializedName), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
