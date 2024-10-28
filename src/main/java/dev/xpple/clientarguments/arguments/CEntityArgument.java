package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CEntityArgument implements ArgumentType<CEntitySelector> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
	public static final SimpleCommandExceptionType TOO_MANY_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.toomany"));
	public static final SimpleCommandExceptionType TOO_MANY_PLAYERS_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
	public static final SimpleCommandExceptionType PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.player.entities"));
	public static final SimpleCommandExceptionType ENTITY_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.entity"));
	public static final SimpleCommandExceptionType PLAYER_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.player"));
	public static final SimpleCommandExceptionType NOT_ALLOWED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.selector.not_allowed"));
	final boolean singleTarget;
	final boolean playersOnly;

	protected CEntityArgument(boolean singleTarget, boolean playersOnly) {
		this.singleTarget = singleTarget;
		this.playersOnly = playersOnly;
	}

	public static CEntityArgument entity() {
		return new CEntityArgument(true, false);
	}

	public static Entity getEntity(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).findSingleEntity(context.getSource());
	}

	public static CEntityArgument entities() {
		return new CEntityArgument(false, false);
	}

	public static Collection<? extends Entity> getEntities(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Collection<? extends Entity> collection = getOptionalEntities(context, name);
		if (collection.isEmpty()) {
			throw ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		return collection;
	}

	public static Collection<? extends Entity> getOptionalEntities(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).findEntities(context.getSource());
	}

	public static Collection<AbstractClientPlayer> getOptionalPlayers(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).findPlayers(context.getSource());
	}

	public static CEntityArgument player() {
		return new CEntityArgument(true, true);
	}

	public static AbstractClientPlayer getPlayer(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).findSinglePlayer(context.getSource());
	}

	public static CEntityArgument players() {
		return new CEntityArgument(false, true);
	}

	public static Collection<AbstractClientPlayer> getPlayers(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		List<AbstractClientPlayer> list = context.getArgument(name, CEntitySelector.class).findPlayers(context.getSource());
		if (list.isEmpty()) {
			throw PLAYER_NOT_FOUND_EXCEPTION.create();
		}
		return list;
	}

	@Override
	public CEntitySelector parse(final StringReader stringReader) throws CommandSyntaxException {
		return this.parse(stringReader, true);
	}

	public <S> CEntitySelector parse(final StringReader stringReader, final S source) throws CommandSyntaxException {
		return this.parse(stringReader, CEntitySelectorParser.allowSelectors(source));
	}

	private CEntitySelector parse(StringReader stringReader, boolean allowSelectors) throws CommandSyntaxException {
		CEntitySelectorParser entitySelectorParser = new CEntitySelectorParser(stringReader, allowSelectors);
		CEntitySelector entitySelector = entitySelectorParser.parse();
		if (entitySelector.getMaxResults() > 1 && this.singleTarget) {
			if (this.playersOnly) {
				stringReader.setCursor(0);
				throw TOO_MANY_PLAYERS_EXCEPTION.createWithContext(stringReader);
			} else {
				stringReader.setCursor(0);
				throw TOO_MANY_ENTITIES_EXCEPTION.createWithContext(stringReader);
			}
		}
		if (entitySelector.includesEntities() && this.playersOnly && !entitySelector.isSelfSelector()) {
			stringReader.setCursor(0);
			throw PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(stringReader);
		}
		return entitySelector;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (context.getSource() instanceof SharedSuggestionProvider commandSource) {
			StringReader stringReader = new StringReader(builder.getInput());
			stringReader.setCursor(builder.getStart());
			CEntitySelectorParser entitySelectorParser = new CEntitySelectorParser(stringReader, CEntitySelectorParser.allowSelectors(commandSource));

			try {
				entitySelectorParser.parse();
			} catch (CommandSyntaxException ignored) {
			}

			return entitySelectorParser.fillSuggestions(builder, builderx -> {
				Collection<String> collection = commandSource.getOnlinePlayerNames();
				Iterable<String> iterable = this.playersOnly ? collection : Iterables.concat(collection, commandSource.getSelectedEntities());
				SharedSuggestionProvider.suggest(iterable, builderx);
			});
		}
		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
