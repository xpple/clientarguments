package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.test.TestFunctions;
import net.minecraft.text.LiteralText;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTestClassArgumentType implements ArgumentType<String> {

	private static final Collection<String> EXAMPLES = Arrays.asList("techtests", "mobtests");

	@Override
	public String parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		if (TestFunctions.testClassExists(string)) {
			return string;
		} else {
			Message message = new LiteralText("No such test class: " + string);
			throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
		}
	}

	public static CTestClassArgumentType testClass() {
		return new CTestClassArgumentType();
	}

	public static String getTestClass(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(TestFunctions.getTestClasses().stream(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
