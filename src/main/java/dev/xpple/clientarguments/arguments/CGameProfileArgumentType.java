package dev.xpple.clientarguments.arguments;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CGameProfileArgumentType implements ArgumentType<CGameProfileArgumentType.GameProfileArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
	public static final SimpleCommandExceptionType UNKNOWN_PLAYER_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.player.unknown"));

	public static CGameProfileArgumentType gameProfile() {
		return new CGameProfileArgumentType();
	}

	public static Collection<GameProfile> getCProfileArgument(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, GameProfileArgument.class).getNames(context.getSource());
	}

	@Override
	public GameProfileArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == '@') {
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
			CEntitySelector entitySelector = entitySelectorReader.read();
			if (entitySelector.includesNonPlayers()) {
				throw CEntityArgumentType.PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.create();
			} else {
				return new SelectorBacked(entitySelector);
			}
		} else {
			int cursor = stringReader.getCursor();

			while (stringReader.canRead() && stringReader.peek() != ' ') {
				stringReader.skip();
			}

			String string = stringReader.getString().substring(cursor, stringReader.getCursor());
			return source -> Collections.singleton(MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream()
					.map(PlayerListEntry::getProfile)
					.filter(profile -> profile.getName().equals(string))
					.findFirst().orElseThrow(UNKNOWN_PLAYER_EXCEPTION::create));
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (context.getSource() instanceof CommandSource) {
			StringReader stringReader = new StringReader(builder.getInput());
			stringReader.setCursor(builder.getStart());
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);

			try {
				entitySelectorReader.read();
			} catch (CommandSyntaxException ignored) {
			}

			return entitySelectorReader.listSuggestions(builder, builderx -> CommandSource.suggestMatching(((CommandSource) context.getSource()).getPlayerNames(), builderx));
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

		public Collection<GameProfile> getNames(FabricClientCommandSource clientCommandSource) throws CommandSyntaxException {
			List<AbstractClientPlayerEntity> list = this.selector.getPlayers(clientCommandSource);
			if (list.isEmpty()) {
				throw CEntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
			} else {
				List<GameProfile> profiles = new ArrayList<>();

				for (AbstractClientPlayerEntity abstractPlayer : list) {
					profiles.add(abstractPlayer.getGameProfile());
				}

				return profiles;
			}
		}
	}
}
