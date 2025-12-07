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
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CObjectiveCriteriaArgument implements ArgumentType<ObjectiveCriteria> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar.baz", "minecraft:foo");
	public static final DynamicCommandExceptionType INVALID_CRITERION_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("argument.criteria.invalid", name));

	private CObjectiveCriteriaArgument() {
	}

	public static CObjectiveCriteriaArgument criteria() {
		return new CObjectiveCriteriaArgument();
	}

	public static ObjectiveCriteria getCriteria(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ObjectiveCriteria.class);
	}

	@Override
	public ObjectiveCriteria parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			stringReader.skip();
		}

		String string = stringReader.getString().substring(cursor, stringReader.getCursor());
		return ObjectiveCriteria.byName(string).orElseThrow(() -> {
			stringReader.setCursor(cursor);
			return INVALID_CRITERION_EXCEPTION.createWithContext(stringReader, string);
		});
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		List<String> list = Lists.newArrayList(ObjectiveCriteria.getCustomCriteriaNames());

		for (StatType<?> statType : BuiltInRegistries.STAT_TYPE) {
			for (var object : statType.getRegistry()) {
				String string = this.getStatName(statType, object);
				list.add(string);
			}
		}

		return SharedSuggestionProvider.suggest(list, builder);
	}

	@SuppressWarnings("unchecked")
    public <T> String getStatName(StatType<T> stat, Object value) {
		return Stat.buildName(stat, (T) value);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
