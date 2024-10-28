package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CGameProfileArgument implements ArgumentType<CGameProfileArgument.Result> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
	public static final SimpleCommandExceptionType UNKNOWN_PLAYER_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.player.unknown"));

	private final boolean singleTarget;

	private CGameProfileArgument(boolean singleTarget) {
		this.singleTarget = singleTarget;
	}

	public static CGameProfileArgument gameProfile() {
		return new CGameProfileArgument(false);
	}

	public static CGameProfileArgument gameProfile(boolean singleTarget) {
		return new CGameProfileArgument(singleTarget);
	}

	public static Collection<GameProfile> getProfileArgument(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, Result.class).getNames(context.getSource());
	}

	public static GameProfile getSingleProfileArgument(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Collection<GameProfile> profiles = context.getArgument(name, Result.class).getNames(context.getSource());
		if (profiles.size() > 1) {
			throw CEntityArgument.TOO_MANY_PLAYERS_EXCEPTION.create();
		}
		return profiles.iterator().next();
	}

	public <S> CGameProfileArgument.Result parse(final StringReader stringReader, final S source) throws CommandSyntaxException {
		return parse(stringReader, CEntitySelectorParser.allowSelectors(source));
	}

	@Override
	public Result parse(final StringReader stringReader) throws CommandSyntaxException {
		return parse(stringReader, true);
	}

	private CGameProfileArgument.Result parse(StringReader stringReader, boolean allowSelectors) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == CEntitySelectorParser.SYNTAX_SELECTOR_START) {
			CEntitySelectorParser entitySelectorParser = new CEntitySelectorParser(stringReader, allowSelectors);
			CEntitySelector entitySelector = entitySelectorParser.parse();
			if (entitySelector.includesEntities()) {
				throw CEntityArgument.PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(stringReader);
			}
			if (this.singleTarget && entitySelector.getMaxResults() > 1) {
				throw CEntityArgument.TOO_MANY_PLAYERS_EXCEPTION.create();
			}
			return new SelectorResult(entitySelector);
		}

		int cursor = stringReader.getCursor();
		while (stringReader.canRead() && stringReader.peek() != ' ') {
			stringReader.skip();
		}

		String playerName = stringReader.getString().substring(cursor, stringReader.getCursor());
		return source -> Collections.singleton(Minecraft.getInstance().getConnection().getOnlinePlayers().stream()
			.map(PlayerInfo::getProfile)
			.filter(profile -> profile.getName().equals(playerName))
			.findFirst().orElseThrow(UNKNOWN_PLAYER_EXCEPTION::create));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (context.getSource() instanceof SharedSuggestionProvider sharedSuggestionProvider) {
			StringReader stringReader = new StringReader(builder.getInput());
			stringReader.setCursor(builder.getStart());
			CEntitySelectorParser entitySelectorParser = new CEntitySelectorParser(stringReader, CEntitySelectorParser.allowSelectors(sharedSuggestionProvider));

			try {
				entitySelectorParser.parse();
			} catch (CommandSyntaxException ignored) {
			}

			return entitySelectorParser.fillSuggestions(builder, builderx -> SharedSuggestionProvider.suggest(((SharedSuggestionProvider) context.getSource()).getOnlinePlayerNames(), builderx));
		}
		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@FunctionalInterface
	public interface Result {
		Collection<GameProfile> getNames(FabricClientCommandSource source) throws CommandSyntaxException;
	}

	public static class SelectorResult implements Result {
		private final CEntitySelector selector;

		public SelectorResult(CEntitySelector selector) {
			this.selector = selector;
		}

		@Override
		public Collection<GameProfile> getNames(FabricClientCommandSource fabricClientCommandSource) throws CommandSyntaxException {
			List<AbstractClientPlayer> list = this.selector.findPlayers(fabricClientCommandSource);
			if (list.isEmpty()) {
				throw CEntityArgument.PLAYER_NOT_FOUND_EXCEPTION.create();
			}
			List<GameProfile> list2 = Lists.newArrayList();

			for (AbstractClientPlayer abstractClientPlayer : list) {
				list2.add(abstractClientPlayer.getGameProfile());
			}

			return list2;
		}
	}
}
