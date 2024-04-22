package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class CColorArgument implements ArgumentType<ChatFormatting> {
	private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");
	public static final DynamicCommandExceptionType INVALID_COLOR_EXCEPTION = new DynamicCommandExceptionType(color -> Component.translatableEscape("argument.color.invalid", color));

	private CColorArgument() {
	}

	public static CColorArgument color() {
		return new CColorArgument();
	}

	public static ChatFormatting getColor(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ChatFormatting.class);
	}

	@Override
	public ChatFormatting parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		ChatFormatting formatting = ChatFormatting.getByName(string);
        if (formatting == null || formatting.isFormat()) {
			throw INVALID_COLOR_EXCEPTION.createWithContext(stringReader, string);
		}
		return formatting;
    }

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(ChatFormatting.getNames(true, false), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
