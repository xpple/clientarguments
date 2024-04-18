package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CScoreboardCriterionArgumentType implements ArgumentType<ScoreboardCriterion> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar.baz", "minecraft:foo");
	public static final DynamicCommandExceptionType INVALID_CRITERION_EXCEPTION = new DynamicCommandExceptionType(name -> Text.stringifiedTranslatable("argument.criteria.invalid", name));

	private CScoreboardCriterionArgumentType() {
	}

	public static CScoreboardCriterionArgumentType scoreboardCriterion() {
		return new CScoreboardCriterionArgumentType();
	}

	public static ScoreboardCriterion getCriterion(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ScoreboardCriterion.class);
	}

	@Override
	public ScoreboardCriterion parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			stringReader.skip();
		}

		String string = stringReader.getString().substring(cursor, stringReader.getCursor());
		return ScoreboardCriterion.getOrCreateStatCriterion(string).orElseThrow(() -> {
			stringReader.setCursor(cursor);
			return INVALID_CRITERION_EXCEPTION.createWithContext(stringReader, string);
		});
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		List<String> list = Lists.newArrayList(ScoreboardCriterion.getAllSimpleCriteria());

		for (StatType<?> statType : Registries.STAT_TYPE) {
			for (var object : statType.getRegistry()) {
				String string = this.getStatName(statType, object);
				list.add(string);
			}
		}

		return CommandSource.suggestMatching(list, builder);
	}

	public <T> String getStatName(StatType<T> stat, Object value) {
		return Stat.getName(stat, (T) value);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
