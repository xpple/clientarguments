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
import net.minecraft.util.ARGB;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CHexColorArgument implements ArgumentType<Integer> {
	private static final Collection<String> EXAMPLES = Arrays.asList("F00", "FF0000");
	public static final DynamicCommandExceptionType ERROR_INVALID_HEX = new DynamicCommandExceptionType(color -> Component.translatableEscape("argument.hexcolor.invalid", color));

	private CHexColorArgument() {
	}

	public static CHexColorArgument hexColor() {
		return new CHexColorArgument();
	}

	public static Integer getHexColor(final CommandContext<FabricClientCommandSource> context, final String argument) {
		return context.getArgument(argument, Integer.class);
	}

	public Integer parse(final StringReader reader) throws CommandSyntaxException {
		String hexString = reader.readUnquotedString();

		return switch (hexString.length()) {
			case 3 -> ARGB.color(
				Integer.valueOf(MessageFormat.format("{0}{0}", hexString.charAt(0)), 16),
				Integer.valueOf(MessageFormat.format("{0}{0}", hexString.charAt(1)), 16),
				Integer.valueOf(MessageFormat.format("{0}{0}", hexString.charAt(2)), 16)
			);
			case 6 -> ARGB.color(Integer.valueOf(hexString.substring(0, 2), 16), Integer.valueOf(hexString.substring(2, 4), 16), Integer.valueOf(hexString.substring(4, 6), 16));
			default -> throw ERROR_INVALID_HEX.createWithContext(reader, hexString);
		};
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
		return SharedSuggestionProvider.suggest(EXAMPLES, suggestionsBuilder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
