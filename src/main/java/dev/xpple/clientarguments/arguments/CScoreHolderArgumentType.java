package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Supplier;

public class CScoreHolderArgumentType implements ArgumentType<CScoreHolderArgumentType.ScoreHolders> {
	public static final SuggestionProvider<FabricClientCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);

		try {
			entitySelectorReader.read();
		} catch (CommandSyntaxException ignored) {
		}

		return entitySelectorReader.listSuggestions(builder, builderx -> CommandSource.suggestMatching(context.getSource().getPlayerNames(), builderx));
	};
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
	private static final SimpleCommandExceptionType EMPTY_SCORE_HOLDER_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.scoreHolder.empty"));
	final boolean multiple;

	public CScoreHolderArgumentType(boolean multiple) {
		this.multiple = multiple;
	}

	public static ScoreHolder getScoreHolder(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name).iterator().next();
	}

	public static Collection<ScoreHolder> getScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name, Collections::emptyList);
	}

	public static Collection<ScoreHolder> getScoreboardScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name, context.getSource().getWorld().getScoreboard()::getKnownScoreHolders);
	}

	public static Collection<ScoreHolder> getScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name, final Supplier<Collection<ScoreHolder>> players) throws CommandSyntaxException {
		Collection<ScoreHolder> collection = context.getArgument(name, CScoreHolderArgumentType.ScoreHolders.class).getNames(context.getSource(), players);
		if (collection.isEmpty()) {
			throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		return collection;
	}

	public static CScoreHolderArgumentType scoreHolder() {
		return new CScoreHolderArgumentType(false);
	}

	public static CScoreHolderArgumentType scoreHolders() {
		return new CScoreHolderArgumentType(true);
	}

	@Override
	public CScoreHolderArgumentType.ScoreHolders parse(final StringReader stringReader) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == '@') {
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
			CEntitySelector entitySelector = entitySelectorReader.read();
			if (!this.multiple && entitySelector.getLimit() > 1) {
				throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.createWithContext(stringReader);
			}
			return new CScoreHolderArgumentType.SelectorScoreHolders(entitySelector);
		}
		int cursor = stringReader.getCursor();

		while (stringReader.canRead() && stringReader.peek() != ' ') {
			stringReader.skip();
		}

		String string = stringReader.getString().substring(cursor, stringReader.getCursor());
		if (string.equals("*")) {
			return (source, players) -> {
				Collection<ScoreHolder> collection = players.get();
				if (collection.isEmpty()) {
					throw EMPTY_SCORE_HOLDER_EXCEPTION.create();
				}
				return collection;
			};
		}
		List<ScoreHolder> list = List.of(ScoreHolder.fromName(string));
		if (string.startsWith("#")) {
			return (source, players) -> list;
		}
		try {
			UUID uuid = UUID.fromString(string);
			return (source, holders) -> {
				ScoreHolder scoreHolder = Streams.stream(source.getWorld().getEntities())
					.filter(e -> e.getUuid().equals(uuid))
					.findAny().orElse(null);

				return scoreHolder != null ? List.of(scoreHolder) : list;
			};
		} catch (IllegalArgumentException var6) {
			return (source, holders) -> {
				AbstractClientPlayerEntity abstractClientPlayerEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity instanceof AbstractClientPlayerEntity)
					.map(entity -> (AbstractClientPlayerEntity) entity)
					.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(string))
					.findAny().orElse(null);
				return abstractClientPlayerEntity != null ? List.of(abstractClientPlayerEntity) : list;
			};
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@FunctionalInterface
	public interface ScoreHolders {
		Collection<ScoreHolder> getNames(FabricClientCommandSource source, Supplier<Collection<ScoreHolder>> holders) throws CommandSyntaxException;
	}

	public static class SelectorScoreHolders implements CScoreHolderArgumentType.ScoreHolders {
		private final CEntitySelector selector;

		public SelectorScoreHolders(CEntitySelector selector) {
			this.selector = selector;
		}

		@Override
		public Collection<ScoreHolder> getNames(FabricClientCommandSource fabricClientCommandSource, Supplier<Collection<ScoreHolder>> supplier) throws CommandSyntaxException {
			List<? extends Entity> list = this.selector.getEntities(fabricClientCommandSource);
			if (list.isEmpty()) {
				throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
			}
			return List.copyOf(list);
		}
	}
}
