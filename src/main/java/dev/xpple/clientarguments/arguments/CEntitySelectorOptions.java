package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.function.Predicate;

public class CEntitySelectorOptions {

	private static final Map<String, SelectorOption> OPTIONS = new HashMap<>();
	public static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.translatable("cargument.entity.options.unknown", arg));
	public static final DynamicCommandExceptionType INAPPLICABLE_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.translatable("cargument.entity.options.inapplicable", arg));
	public static final SimpleCommandExceptionType NEGATIVE_DISTANCE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("cargument.entity.options.distance.negative"));
	public static final SimpleCommandExceptionType NEGATIVE_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("cargument.entity.options.level.negative"));
	public static final SimpleCommandExceptionType TOO_SMALL_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("cargument.entity.options.limit.toosmall"));
	public static final DynamicCommandExceptionType IRREVERSIBLE_SORT_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.translatable("cargument.entity.options.sort.irreversible", arg));
	public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.translatable("cargument.entity.options.mode.invalid", arg));
	public static final DynamicCommandExceptionType INVALID_TYPE_EXCEPTION = new DynamicCommandExceptionType(arg -> Text.translatable("cargument.entity.options.type.invalid", arg));

	private static void putOption(String id, SelectorHandler handler, Predicate<CEntitySelectorReader> condition, Text description) {
		OPTIONS.put(id, new SelectorOption(handler, condition, description));
	}

	public static void register() {
		if (!OPTIONS.isEmpty()) {
			return;
		}
		CEntitySelectorOptions.putOption("name", reader2 -> {
			int i = reader2.getReader().getCursor();
			boolean bl = reader2.readNegationCharacter();
			String string = reader2.getReader().readString();
			if (reader2.excludesName() && !bl) {
				reader2.getReader().setCursor(i);
				throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader2.getReader(), "name");
			}
			if (bl) {
				reader2.setExcludesName(true);
			} else {
				reader2.setSelectsName(true);
			}
			reader2.setPredicate(reader -> reader.getName().getString().equals(string) != bl);
		}, reader -> !reader.selectsName(), Text.translatable("cargument.entity.options.name.description"));
		CEntitySelectorOptions.putOption("distance", reader -> {
			int i = reader.getReader().getCursor();
			NumberRange.FloatRange floatRange = NumberRange.FloatRange.parse(reader.getReader());
			if (floatRange.getMin() != null && floatRange.getMin() < 0.0 || floatRange.getMax() != null && floatRange.getMax() < 0.0) {
				reader.getReader().setCursor(i);
				throw NEGATIVE_DISTANCE_EXCEPTION.createWithContext(reader.getReader());
			}
			reader.setDistance(floatRange);
		}, reader -> reader.getDistance().isDummy(), Text.translatable("cargument.entity.options.distance.description"));
		CEntitySelectorOptions.putOption("level", reader -> {
			int i = reader.getReader().getCursor();
			NumberRange.IntRange intRange = NumberRange.IntRange.parse(reader.getReader());
			if (intRange.getMin() != null && intRange.getMin() < 0 || intRange.getMax() != null && intRange.getMax() < 0) {
				reader.getReader().setCursor(i);
				throw NEGATIVE_LEVEL_EXCEPTION.createWithContext(reader.getReader());
			}
			reader.setLevelRange(intRange);
			reader.setIncludesNonPlayers(false);
		}, reader -> reader.getLevelRange().isDummy(), Text.translatable("cargument.entity.options.level.description"));
		CEntitySelectorOptions.putOption("x", reader -> reader.setX(reader.getReader().readDouble()), reader -> reader.getX() == null, Text.translatable("cargument.entity.options.x.description"));
		CEntitySelectorOptions.putOption("y", reader -> reader.setY(reader.getReader().readDouble()), reader -> reader.getY() == null, Text.translatable("cargument.entity.options.y.description"));
		CEntitySelectorOptions.putOption("z", reader -> reader.setZ(reader.getReader().readDouble()), reader -> reader.getZ() == null, Text.translatable("cargument.entity.options.z.description"));
		CEntitySelectorOptions.putOption("dx", reader -> reader.setDx(reader.getReader().readDouble()), reader -> reader.getDx() == null, Text.translatable("cargument.entity.options.dx.description"));
		CEntitySelectorOptions.putOption("dy", reader -> reader.setDy(reader.getReader().readDouble()), reader -> reader.getDy() == null, Text.translatable("cargument.entity.options.dy.description"));
		CEntitySelectorOptions.putOption("dz", reader -> reader.setDz(reader.getReader().readDouble()), reader -> reader.getDz() == null, Text.translatable("cargument.entity.options.dz.description"));
		CEntitySelectorOptions.putOption("x_rotation", reader -> reader.setPitchRange(FloatRangeArgument.parse(reader.getReader(), true, MathHelper::wrapDegrees)), reader -> reader.getPitchRange() == FloatRangeArgument.ANY, Text.translatable("cargument.entity.options.x_rotation.description"));
		CEntitySelectorOptions.putOption("y_rotation", reader -> reader.setYawRange(FloatRangeArgument.parse(reader.getReader(), true, MathHelper::wrapDegrees)), reader -> reader.getYawRange() == FloatRangeArgument.ANY, Text.translatable("cargument.entity.options.y_rotation.description"));
		CEntitySelectorOptions.putOption("limit", reader -> {
			int i = reader.getReader().getCursor();
			int j = reader.getReader().readInt();
			if (j < 1) {
				reader.getReader().setCursor(i);
				throw TOO_SMALL_LEVEL_EXCEPTION.createWithContext(reader.getReader());
			}
			reader.setLimit(j);
			reader.setHasLimit(true);
		}, reader -> !reader.isSenderOnly() && !reader.hasLimit(), Text.translatable("cargument.entity.options.limit.description"));
		CEntitySelectorOptions.putOption("sort", reader -> {
			int i = reader.getReader().getCursor();
			String string = reader.getReader().readUnquotedString();
			reader.setSuggestionProvider((builder, consumer) -> CommandSource.suggestMatching(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder));
			reader.setSorter(switch (string) {
				case "nearest" -> EntitySelectorReader.NEAREST;
				case "furthest" -> EntitySelectorReader.FURTHEST;
				case "random" -> EntitySelectorReader.RANDOM;
				case "arbitrary" -> EntitySelector.ARBITRARY;
				default -> {
					reader.getReader().setCursor(i);
					throw IRREVERSIBLE_SORT_EXCEPTION.createWithContext(reader.getReader(), string);
				}
			});
			reader.setHasSorter(true);
		}, reader -> !reader.isSenderOnly() && !reader.hasSorter(), Text.translatable("cargument.entity.options.sort.description"));
		CEntitySelectorOptions.putOption("gamemode", reader -> {
			reader.setSuggestionProvider((builder, consumer) -> {
				String string = builder.getRemaining().toLowerCase(Locale.ROOT);
				boolean bl = !reader.excludesGameMode();
				boolean bl2 = true;
				if (!string.isEmpty()) {
					if (string.charAt(0) == '!') {
						bl = false;
						string = string.substring(1);
					} else {
						bl2 = false;
					}
				}
				for (GameMode gameMode : GameMode.values()) {
					if (!gameMode.getName().toLowerCase(Locale.ROOT).startsWith(string)) continue;
					if (bl2) {
						builder.suggest("!" + gameMode.getName());
					}
					if (!bl) continue;
					builder.suggest(gameMode.getName());
				}
				return builder.buildFuture();
			});
			int i = reader.getReader().getCursor();
			boolean bl = reader.readNegationCharacter();
			if (reader.excludesGameMode() && !bl) {
				reader.getReader().setCursor(i);
				throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "gamemode");
			}
			String string = reader.getReader().readUnquotedString();
			GameMode gameMode = GameMode.byName(string, null);
			if (gameMode == null) {
				reader.getReader().setCursor(i);
				throw INVALID_MODE_EXCEPTION.createWithContext(reader.getReader(), string);
			}
			reader.setIncludesNonPlayers(false);
			reader.setPredicate(entity -> {
				if (!(entity instanceof AbstractClientPlayerEntity player)) {
					return false;
				}

				GameMode gameMode2 = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(player.getUuid()).getGameMode();
				return bl == (gameMode2 != gameMode);
			});
			if (bl) {
				reader.setExcludesGameMode(true);
			} else {
				reader.setSelectsGameMode(true);
			}
		}, reader -> !reader.selectsGameMode(), Text.translatable("cargument.entity.options.gamemode.description"));
		CEntitySelectorOptions.putOption("team", reader -> {
			boolean bl = reader.readNegationCharacter();
			String string = reader.getReader().readUnquotedString();
			reader.setPredicate(entity -> {
				if (!(entity instanceof LivingEntity)) {
					return false;
				}
				AbstractTeam abstractTeam = entity.getScoreboardTeam();
				String string2 = abstractTeam == null ? "" : abstractTeam.getName();
				return string2.equals(string) != bl;
			});
			if (bl) {
				reader.setExcludesTeam(true);
			} else {
				reader.setSelectsTeam(true);
			}
		}, reader -> !reader.selectsTeam(), Text.translatable("cargument.entity.options.team.description"));
		CEntitySelectorOptions.putOption("type", reader -> {
			reader.setSuggestionProvider((builder, consumer) -> {
				CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder, String.valueOf('!'));
				CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().map(TagKey::id), builder, "!#");
				if (!reader.excludesEntityType()) {
					CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder);
					CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().map(TagKey::id), builder, String.valueOf('#'));
				}
				return builder.buildFuture();
			});
			int i = reader.getReader().getCursor();
			boolean bl = reader.readNegationCharacter();
			if (reader.excludesEntityType() && !bl) {
				reader.getReader().setCursor(i);
				throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "type");
			}
			if (bl) {
				reader.setExcludesEntityType();
			}
			if (reader.readTagCharacter()) {
				TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.fromCommandInput(reader.getReader()));
				reader.setPredicate(entity -> entity.getType().isIn(tagKey) != bl);
			} else {
				Identifier tagKey = Identifier.fromCommandInput(reader.getReader());
				EntityType<?> entityType = Registries.ENTITY_TYPE.getOrEmpty(tagKey).orElseThrow(() -> {
					reader.getReader().setCursor(i);
					return INVALID_TYPE_EXCEPTION.createWithContext(reader.getReader(), tagKey.toString());
				});
				if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
					reader.setIncludesNonPlayers(false);
				}
				reader.setPredicate(entity -> Objects.equals(entityType, entity.getType()) != bl);
				if (!bl) {
					reader.setEntityType(entityType);
				}
			}
		}, reader -> !reader.selectsEntityType(), Text.translatable("cargument.entity.options.type.description"));
		CEntitySelectorOptions.putOption("tag", reader -> {
			boolean bl = reader.readNegationCharacter();
			String string = reader.getReader().readUnquotedString();
			reader.setPredicate(entity -> {
				if ("".equals(string)) {
					return entity.getScoreboardTags().isEmpty() != bl;
				}
				return entity.getScoreboardTags().contains(string) != bl;
			});
		}, reader -> true, Text.translatable("cargument.entity.options.tag.description"));
		CEntitySelectorOptions.putOption("nbt", reader -> {
			boolean bl = reader.readNegationCharacter();
			NbtCompound nbtCompound = new StringNbtReader(reader.getReader()).parseCompound();
			reader.setPredicate(entity -> {
				ItemStack itemStack;
				NbtCompound nbtCompound2 = entity.writeNbt(new NbtCompound());
				if (entity instanceof OtherClientPlayerEntity && !(itemStack = ((OtherClientPlayerEntity) entity).getInventory().getMainHandStack()).isEmpty()) {
					nbtCompound2.put("SelectedItem", itemStack.writeNbt(new NbtCompound()));
				}
				return NbtHelper.matches(nbtCompound, nbtCompound2, true) != bl;
			});
		}, reader -> true, Text.translatable("cargument.entity.options.nbt.description"));
		CEntitySelectorOptions.putOption("scores", reader -> {
			StringReader stringReader = reader.getReader();
			HashMap<String, NumberRange.IntRange> map = Maps.newHashMap();
			stringReader.expect('{');
			stringReader.skipWhitespace();
			while (stringReader.canRead() && stringReader.peek() != '}') {
				stringReader.skipWhitespace();
				String string = stringReader.readUnquotedString();
				stringReader.skipWhitespace();
				stringReader.expect('=');
				stringReader.skipWhitespace();
				NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
				map.put(string, intRange);
				stringReader.skipWhitespace();
				if (!stringReader.canRead() || stringReader.peek() != ',') continue;
				stringReader.skip();
			}
			stringReader.expect('}');
			if (!map.isEmpty()) {
				reader.setPredicate(entity -> {
					Scoreboard scoreboard = entity.getWorld().getScoreboard();
					String string = entity.getEntityName();
					for (Map.Entry<String, NumberRange.IntRange> entry : map.entrySet()) {
						ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(entry.getKey());
						if (scoreboardObjective == null) {
							return false;
						}
						if (!scoreboard.playerHasObjective(string, scoreboardObjective)) {
							return false;
						}
						ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(string, scoreboardObjective);
						int i = scoreboardPlayerScore.getScore();
						if (entry.getValue().test(i)) {
							continue;
						}
						return false;
					}
					return true;
				});
			}
			reader.setSelectsScores(true);
		}, reader -> !reader.selectsScores(), Text.translatable("cargument.entity.options.scores.description"));
		CEntitySelectorOptions.putOption("advancements", reader -> {
			StringReader stringReader = reader.getReader();
			Map<Identifier, Predicate<AdvancementProgress>> map = new HashMap<>();
			stringReader.expect('{');
			stringReader.skipWhitespace();
			while (stringReader.canRead() && stringReader.peek() != '}') {
				stringReader.skipWhitespace();
				Identifier identifier = Identifier.fromCommandInput(stringReader);
				stringReader.skipWhitespace();
				stringReader.expect('=');
				stringReader.skipWhitespace();
				if (stringReader.canRead() && stringReader.peek() == '{') {
					HashMap<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
					stringReader.skipWhitespace();
					stringReader.expect('{');
					stringReader.skipWhitespace();
					while (stringReader.canRead() && stringReader.peek() != '}') {
						stringReader.skipWhitespace();
						String string = stringReader.readUnquotedString();
						stringReader.skipWhitespace();
						stringReader.expect('=');
						stringReader.skipWhitespace();
						boolean bl = stringReader.readBoolean();
						map2.put(string, criterionProgress -> criterionProgress.isObtained() == bl);
						stringReader.skipWhitespace();
						if (!stringReader.canRead() || stringReader.peek() != ',') continue;
						stringReader.skip();
					}
					stringReader.skipWhitespace();
					stringReader.expect('}');
					stringReader.skipWhitespace();
					map.put(identifier, advancementProgress -> {
						for (Map.Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
							CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
							if (criterionProgress != null && entry.getValue().test(criterionProgress)) continue;
							return false;
						}
						return true;
					});
				} else {
					boolean map2 = stringReader.readBoolean();
					map.put(identifier, advancementProgress -> advancementProgress.isDone() == map2);
				}
				stringReader.skipWhitespace();
				if (!stringReader.canRead() || stringReader.peek() != ',') {
					continue;
				}
				stringReader.skip();
			}
			stringReader.expect('}');
			if (!map.isEmpty()) {
				reader.setPredicate(entity -> false);
				reader.setIncludesNonPlayers(false);
			}
			reader.setSelectsAdvancements(true);
		}, reader -> !reader.selectsAdvancements(), Text.translatable("cargument.entity.options.advancements.description"));
		CEntitySelectorOptions.putOption("predicate", reader -> reader.setPredicate(entity -> false), reader -> true, Text.translatable("cargument.entity.options.predicate.description"));
	}

	public static SelectorHandler getHandler(CEntitySelectorReader reader, String option, int restoreCursor) throws CommandSyntaxException {
		SelectorOption selectorOption = OPTIONS.get(option);
		if (selectorOption != null) {
			if (selectorOption.condition.test(reader)) {
				return selectorOption.handler;
			}
			throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
		}
		reader.getReader().setCursor(restoreCursor);
		throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
	}

	public static void suggestOptions(CEntitySelectorReader reader, SuggestionsBuilder suggestionBuilder) {
		String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);
		for (Map.Entry<String, SelectorOption> entry : OPTIONS.entrySet()) {
			if (!entry.getValue().condition.test(reader) || !entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
				continue;
			}
			suggestionBuilder.suggest(entry.getKey() + "=", entry.getValue().description);
		}
	}

	private static class SelectorOption {
		public final SelectorHandler handler;
		public final Predicate<CEntitySelectorReader> condition;
		public final Text description;

		SelectorOption(SelectorHandler handler, Predicate<CEntitySelectorReader> condition, Text description) {
			this.handler = handler;
			this.condition = condition;
			this.description = description;
		}
	}

	public interface SelectorHandler {
		void handle(CEntitySelectorReader reader) throws CommandSyntaxException;
	}
}
