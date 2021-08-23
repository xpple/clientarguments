package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CScoreboardCriterionArgumentType implements ArgumentType<ScoreboardCriterion> {

	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar.baz", "minecraft:foo");
	public static final DynamicCommandExceptionType INVALID_CRITERION_EXCEPTION = new DynamicCommandExceptionType(name -> new TranslatableText("cargument.criteria.invalid", name));

	public static CScoreboardCriterionArgumentType scoreboardCriterion() {
		return new CScoreboardCriterionArgumentType();
	}

	public static ScoreboardCriterion getCriterion(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ScoreboardCriterion.class);
	}

	@Override
	public ScoreboardCriterion parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();

		while(stringReader.canRead() && stringReader.peek() != ' ') {
			stringReader.skip();
		}

		String string = stringReader.getString().substring(cursor, stringReader.getCursor());
		return ScoreboardCriterion.getOrCreateStatCriterion(string).orElseThrow(() -> {
			stringReader.setCursor(cursor);
			return INVALID_CRITERION_EXCEPTION.create(string);
		});
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		List<String> list = new ArrayList<>(ScoreboardCriterion.method_37271());

		Registry.STAT_TYPE.stream().forEach(type -> type.forEach(value -> {
			list.add(getName(Registry.STAT_TYPE.getId(type)) + ":" + value.getName());
		}));

		return CommandSource.suggestMatching(list, builder);
	}

	private static <T> String getName(Identifier id) {
		return id.toString().replace(':', '.');
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
