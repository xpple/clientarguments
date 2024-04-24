package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CScoreHolderArgument implements ArgumentType<CScoreHolderArgument.Result> {
	public static final SuggestionProvider<FabricClientCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);

		try {
			entitySelectorReader.read();
		} catch (CommandSyntaxException ignored) {
		}

		return entitySelectorReader.listSuggestions(builder, builderx -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builderx));
	};
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
	private static final SimpleCommandExceptionType EMPTY_SCORE_HOLDER_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
	final boolean multiple;

	public CScoreHolderArgument(boolean multiple) {
		this.multiple = multiple;
	}

	public static ScoreHolder getScoreHolder(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name).iterator().next();
	}

	public static Collection<ScoreHolder> getScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name, Collections::emptyList);
	}

	public static Collection<ScoreHolder> getScoreboardScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name, context.getSource().getWorld().getScoreboard()::getTrackedPlayers);
	}

	public static Collection<ScoreHolder> getScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name, final Supplier<Collection<ScoreHolder>> players) throws CommandSyntaxException {
		Collection<ScoreHolder> collection = context.getArgument(name, Result.class).getNames(context.getSource(), players);
		if (collection.isEmpty()) {
			throw EntityArgument.NO_ENTITIES_FOUND.create();
		}
		return collection;
	}

	public static CScoreHolderArgument scoreHolder() {
		return new CScoreHolderArgument(false);
	}

	public static CScoreHolderArgument scoreHolders() {
		return new CScoreHolderArgument(true);
	}

	@Override
	public Result parse(final StringReader stringReader) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == '@') {
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
			CEntitySelector entitySelector = entitySelectorReader.read();
			if (!this.multiple && entitySelector.getLimit() > 1) {
				throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(stringReader);
			}
			return new SelectorResult(entitySelector);
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
		List<ScoreHolder> list = List.of(ScoreHolder.forNameOnly(string));
		if (string.startsWith("#")) {
			return (source, players) -> list;
		}
		try {
			UUID uuid = UUID.fromString(string);
			return (source, holders) -> {
				ScoreHolder scoreHolder = Streams.stream(source.getWorld().entitiesForRendering())
					.filter(e -> e.getUUID().equals(uuid))
					.findAny().orElse(null);

				return scoreHolder != null ? List.of(scoreHolder) : list;
			};
		} catch (IllegalArgumentException var6) {
			return (source, holders) -> {
				AbstractClientPlayer abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
					.filter(entity -> entity instanceof AbstractClientPlayer)
					.map(entity -> (AbstractClientPlayer) entity)
					.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(string))
					.findAny().orElse(null);
				return abstractClientPlayer != null ? List.of(abstractClientPlayer) : list;
			};
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@FunctionalInterface
	public interface Result {
		Collection<ScoreHolder> getNames(FabricClientCommandSource source, Supplier<Collection<ScoreHolder>> holders) throws CommandSyntaxException;
	}

	public static class SelectorResult implements Result {
		private final CEntitySelector selector;

		public SelectorResult(CEntitySelector selector) {
			this.selector = selector;
		}

		@Override
		public Collection<ScoreHolder> getNames(FabricClientCommandSource fabricClientCommandSource, Supplier<Collection<ScoreHolder>> supplier) throws CommandSyntaxException {
			List<? extends Entity> list = this.selector.getEntities(fabricClientCommandSource);
			if (list.isEmpty()) {
				throw EntityArgument.NO_ENTITIES_FOUND.create();
			}
			return List.copyOf(list);
		}
	}
}
