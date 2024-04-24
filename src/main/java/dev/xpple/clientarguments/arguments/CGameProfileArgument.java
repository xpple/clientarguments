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

public class CGameProfileArgument implements ArgumentType<CGameProfileArgument.GameProfileArgument> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
	public static final SimpleCommandExceptionType UNKNOWN_PLAYER_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.player.unknown"));

	public static CGameProfileArgument gameProfile() {
		return new CGameProfileArgument();
	}

	public static Collection<GameProfile> getProfileArgument(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, GameProfileArgument.class).getNames(context.getSource());
	}

	@Override
	public GameProfileArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == '@') {
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
			CEntitySelector entitySelector = entitySelectorReader.read();
			if (entitySelector.includesNonPlayers()) {
				throw CEntityArgument.PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(stringReader);
			}
			return new CGameProfileArgument.SelectorBacked(entitySelector);
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
		if (context.getSource() instanceof SharedSuggestionProvider) {
			StringReader stringReader = new StringReader(builder.getInput());
			stringReader.setCursor(builder.getStart());
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);

			try {
				entitySelectorReader.read();
			} catch (CommandSyntaxException ignored) {
			}

			return entitySelectorReader.listSuggestions(builder, builderx -> SharedSuggestionProvider.suggest(((SharedSuggestionProvider) context.getSource()).getOnlinePlayerNames(), builderx));
		}
		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@FunctionalInterface
	public interface GameProfileArgument {
		Collection<GameProfile> getNames(FabricClientCommandSource source) throws CommandSyntaxException;
	}

	public static class SelectorBacked implements GameProfileArgument {
		private final CEntitySelector selector;

		public SelectorBacked(CEntitySelector selector) {
			this.selector = selector;
		}

		@Override
		public Collection<GameProfile> getNames(FabricClientCommandSource fabricClientCommandSource) throws CommandSyntaxException {
			List<AbstractClientPlayer> list = this.selector.getPlayers(fabricClientCommandSource);
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
