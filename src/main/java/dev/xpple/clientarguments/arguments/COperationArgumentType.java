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
import net.minecraft.scoreboard.ScoreboardPlayerScore;
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

	public static Operation getCOperation(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Operation.class);
	}

	@Override
	public Operation parse(final StringReader stringReader) throws CommandSyntaxException {
		if (!stringReader.canRead()) {
			throw INVALID_OPERATION.create();
		} else {
			int cursor = stringReader.getCursor();

			while(stringReader.canRead() && stringReader.peek() != ' ') {
				stringReader.skip();
			}

			return getOperator(stringReader.getString().substring(cursor, stringReader.getCursor()));
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static Operation getOperator(String operator) throws CommandSyntaxException {
		return operator.equals("><") ? (a, b) -> {
			int score = a.getScore();
			a.setScore(b.getScore());
			b.setScore(score);
		} : getIntOperator(operator);
	}

	private static IntOperator getIntOperator(String operator) throws CommandSyntaxException {
		byte code = -1;
		switch (operator.hashCode()) {
			case 60 -> {
				if (operator.equals("<")) {
					code = 6;
				}
			}
			case 61 -> {
				if (operator.equals("=")) {
					code = 0;
				}
			}
			case 62 -> {
				if (operator.equals(">")) {
					code = 7;
				}
			}
			case 1208 -> {
				if (operator.equals("%=")) {
					code = 5;
				}
			}
			case 1363 -> {
				if (operator.equals("*=")) {
					code = 3;
				}
			}
			case 1394 -> {
				if (operator.equals("+=")) {
					code = 1;
				}
			}
			case 1456 -> {
				if (operator.equals("-=")) {
					code = 2;
				}
			}
			case 1518 -> {
				if (operator.equals("/=")) {
					code = 4;
				}
			}
		}

		return switch (code) {
			case 0 -> (a, b) -> b;
			case 1 -> Integer::sum;
			case 2 -> (a, b) -> a - b;
			case 3 -> (a, b) -> a * b;
			case 4 -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}
				return MathHelper.floorDiv(a, b);
			};
			case 5 -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}
				return MathHelper.floorMod(a, b);
			};
			case 6 -> Math::min;
			case 7 -> Math::max;
			default -> throw INVALID_OPERATION.create();
		};
	}

	@FunctionalInterface
	public interface Operation {
		void apply(ScoreboardPlayerScore a, ScoreboardPlayerScore b) throws CommandSyntaxException;
	}

	@FunctionalInterface
	private interface IntOperator extends Operation {
		int apply(int a, int b) throws CommandSyntaxException;

		default void apply(ScoreboardPlayerScore scoreboardPlayerScore, ScoreboardPlayerScore scoreboardPlayerScore2) throws CommandSyntaxException {
			scoreboardPlayerScore.setScore(this.apply(scoreboardPlayerScore.getScore(), scoreboardPlayerScore2.getScore()));
		}
	}
}
