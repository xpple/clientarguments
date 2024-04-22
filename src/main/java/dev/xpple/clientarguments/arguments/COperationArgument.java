package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class COperationArgument implements ArgumentType<COperationArgument.Operation> {
	private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
	private static final SimpleCommandExceptionType INVALID_OPERATION = new SimpleCommandExceptionType(Component.translatable("arguments.operation.invalid"));
	private static final SimpleCommandExceptionType DIVISION_ZERO_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("arguments.operation.div0"));

	public static COperationArgument operation() {
		return new COperationArgument();
	}

	public static COperationArgument.Operation getOperation(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, COperationArgument.Operation.class);
	}

	@Override
	public COperationArgument.Operation parse(final StringReader stringReader) throws CommandSyntaxException {
		if (!stringReader.canRead()) {
			throw INVALID_OPERATION.createWithContext(stringReader);
		}
		int i = stringReader.getCursor();

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			stringReader.skip();
		}

		return getOperator(stringReader.getString().substring(i, stringReader.getCursor()));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static COperationArgument.Operation getOperator(String operator) throws CommandSyntaxException {
		return operator.equals("><") ? (a, b) -> {
			int i = a.get();
			a.set(b.get());
			b.set(i);
		} : getIntOperator(operator);
	}

	private static COperationArgument.IntOperator getIntOperator(String operator) throws CommandSyntaxException {
		return switch(operator) {
			case "=" -> (a, b) -> b;
			case "+=" -> Integer::sum;
			case "-=" -> (a, b) -> a - b;
			case "*=" -> (a, b) -> a * b;
			case "/=" -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}
				return Mth.floorDiv(a, b);
			};
			case "%=" -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}
				return Mth.positiveModulo(a, b);
			};
			case "<" -> Math::min;
			case ">" -> Math::max;
			default -> throw INVALID_OPERATION.create();
		};
	}

	@FunctionalInterface
	interface IntOperator extends COperationArgument.Operation {
		int apply(int a, int b) throws CommandSyntaxException;

		@Override
		default void apply(ScoreAccess scoreAccess, ScoreAccess scoreAccess2) throws CommandSyntaxException {
			scoreAccess.set(this.apply(scoreAccess.get(), scoreAccess2.get()));
		}
	}

	@FunctionalInterface
	public interface Operation {
		void apply(ScoreAccess a, ScoreAccess b) throws CommandSyntaxException;
	}
}
