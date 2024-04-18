package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class COperationArgumentType implements ArgumentType<COperationArgumentType.Operation> {
	private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
	private static final SimpleCommandExceptionType INVALID_OPERATION = new SimpleCommandExceptionType(Text.translatable("arguments.operation.invalid"));
	private static final SimpleCommandExceptionType DIVISION_ZERO_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("arguments.operation.div0"));

	public static COperationArgumentType operation() {
		return new COperationArgumentType();
	}

	public static COperationArgumentType.Operation getOperation(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, COperationArgumentType.Operation.class);
	}

	@Override
	public COperationArgumentType.Operation parse(final StringReader stringReader) throws CommandSyntaxException {
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
		return CommandSource.suggestMatching(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static COperationArgumentType.Operation getOperator(String operator) throws CommandSyntaxException {
		return operator.equals("><") ? (a, b) -> {
			int i = a.getScore();
			a.setScore(b.getScore());
			b.setScore(i);
		} : getIntOperator(operator);
	}

	private static COperationArgumentType.IntOperator getIntOperator(String operator) throws CommandSyntaxException {
		return switch(operator) {
			case "=" -> (a, b) -> b;
			case "+=" -> Integer::sum;
			case "-=" -> (a, b) -> a - b;
			case "*=" -> (a, b) -> a * b;
			case "/=" -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}
				return MathHelper.floorDiv(a, b);
			};
			case "%=" -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}
				return MathHelper.floorMod(a, b);
			};
			case "<" -> Math::min;
			case ">" -> Math::max;
			default -> throw INVALID_OPERATION.create();
		};
	}

	@FunctionalInterface
	interface IntOperator extends COperationArgumentType.Operation {
		int apply(int a, int b) throws CommandSyntaxException;

		@Override
		default void apply(ScoreAccess scoreAccess, ScoreAccess scoreAccess2) throws CommandSyntaxException {
			scoreAccess.setScore(this.apply(scoreAccess.getScore(), scoreAccess2.getScore()));
		}
	}

	@FunctionalInterface
	public interface Operation {
		void apply(ScoreAccess a, ScoreAccess b) throws CommandSyntaxException;
	}
}
