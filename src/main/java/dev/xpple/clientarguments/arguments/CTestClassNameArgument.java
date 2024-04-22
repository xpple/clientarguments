package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTestClassNameArgument implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("techtests", "mobtests");

	public static CTestClassNameArgument testClassName() {
		return new CTestClassNameArgument();
	}

	public static String getTestClassName(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		if (!GameTestRegistry.isTestClass(string)) {
			Message message = Component.literal("No such test class: " + string);
			throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
		}
		return string;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(GameTestRegistry.getAllTestClassNames().stream(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
