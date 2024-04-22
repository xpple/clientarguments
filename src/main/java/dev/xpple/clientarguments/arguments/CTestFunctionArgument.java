package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CTestFunctionArgument implements ArgumentType<TestFunction> {
	private static final Collection<String> EXAMPLES = Arrays.asList("techtests.piston", "techtests");

	public static CTestFunctionArgument testFunction() {
		return new CTestFunctionArgument();
	}

	public static TestFunction getFunction(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, TestFunction.class);
	}

	@Override
	public TestFunction parse(StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		Optional<TestFunction> optional = GameTestRegistry.findTestFunction(string);
        if (optional.isEmpty()) {
            Message message = Component.literal("No such test: " + string);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }
		return optional.get();
    }

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return suggestTestNames(context, builder);
	}

	public static <S> CompletableFuture<Suggestions> suggestTestNames(final CommandContext<S> context, final SuggestionsBuilder builder) {
		Stream<String> stream = GameTestRegistry.getAllTestFunctions().stream().map(TestFunction::testName);
		return SharedSuggestionProvider.suggest(stream, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
