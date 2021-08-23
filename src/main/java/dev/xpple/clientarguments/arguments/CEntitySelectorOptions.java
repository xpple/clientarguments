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
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class CEntitySelectorOptions {

	private static final Map<String, CEntitySelectorOptions.SelectorOption> OPTIONS = new HashMap<>();
	public static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("cargument.entity.options.unknown", arg));
	public static final DynamicCommandExceptionType INAPPLICABLE_OPTION_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("cargument.entity.options.inapplicable", arg));
	public static final SimpleCommandExceptionType NEGATIVE_DISTANCE_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.options.distance.negative"));
	public static final SimpleCommandExceptionType NEGATIVE_LEVEL_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.options.level.negative"));
	public static final SimpleCommandExceptionType TOO_SMALL_LEVEL_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.options.limit.toosmall"));
	public static final DynamicCommandExceptionType IRREVERSIBLE_SORT_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("cargument.entity.options.sort.irreversible", arg));
	public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("cargument.entity.options.mode.invalid", arg));
	public static final DynamicCommandExceptionType INVALID_TYPE_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("cargument.entity.options.type.invalid", arg));

	private static final MinecraftClient client = MinecraftClient.getInstance();

	private static void putOption(String id, CEntitySelectorOptions.SelectorHandler handler, Predicate<CEntitySelectorReader> condition, Text description) {
		OPTIONS.put(id, new CEntitySelectorOptions.SelectorOption(handler, condition, description));
	}

	public static void register() {
		if (OPTIONS.isEmpty()) {
			putOption("name", entitySelectorReader -> {
				int cursor = entitySelectorReader.getReader().getCursor();
				boolean bl = entitySelectorReader.readNegationCharacter();
				String string = entitySelectorReader.getReader().readString();
				if (entitySelectorReader.excludesName() && !bl) {
					entitySelectorReader.getReader().setCursor(cursor);
					throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(entitySelectorReader.getReader(), "name");
				} else {
					if (bl) {
						entitySelectorReader.setExcludesName(true);
					} else {
						entitySelectorReader.setSelectsName(true);
					}

					entitySelectorReader.setPredicate(entity -> entity.getName().getString().equals(string) != bl);
				}
			}, entitySelectorReader -> !entitySelectorReader.selectsName(), new TranslatableText("cargument.entity.options.name.description"));
			putOption("distance", entitySelectorReader -> {
				int cursor = entitySelectorReader.getReader().getCursor();
				NumberRange.FloatRange floatRange = NumberRange.FloatRange.parse(entitySelectorReader.getReader());
				if ((floatRange.getMin() == null || !(floatRange.getMin() < 0.0D)) && (floatRange.getMax() == null || !(floatRange.getMax() < 0.0D))) {
					entitySelectorReader.setDistance(floatRange);
				} else {
					entitySelectorReader.getReader().setCursor(cursor);
					throw NEGATIVE_DISTANCE_EXCEPTION.createWithContext(entitySelectorReader.getReader());
				}
			}, entitySelectorReader -> entitySelectorReader.getDistance().isDummy(), new TranslatableText("cargument.entity.options.distance.description"));
			putOption("level", entitySelectorReader -> {
				int cursor = entitySelectorReader.getReader().getCursor();
				NumberRange.IntRange intRange = NumberRange.IntRange.parse(entitySelectorReader.getReader());
				if ((intRange.getMin() == null || intRange.getMin() >= 0) && (intRange.getMax() == null || intRange.getMax() >= 0)) {
					entitySelectorReader.setLevelRange(intRange);
					entitySelectorReader.setIncludesNonPlayers(false);
				} else {
					entitySelectorReader.getReader().setCursor(cursor);
					throw NEGATIVE_LEVEL_EXCEPTION.createWithContext(entitySelectorReader.getReader());
				}
			}, entitySelectorReader -> entitySelectorReader.getLevelRange().isDummy(), new TranslatableText("cargument.entity.options.level.description"));
			putOption("x", entitySelectorReader -> entitySelectorReader.setX(entitySelectorReader.getReader().readDouble()), entitySelectorReader -> entitySelectorReader.getX() == null, new TranslatableText("cargument.entity.options.x.description"));
			putOption("y", entitySelectorReader -> entitySelectorReader.setY(entitySelectorReader.getReader().readDouble()), entitySelectorReader -> entitySelectorReader.getY() == null, new TranslatableText("cargument.entity.options.y.description"));
			putOption("z", entitySelectorReader -> entitySelectorReader.setZ(entitySelectorReader.getReader().readDouble()), entitySelectorReader -> entitySelectorReader.getZ() == null, new TranslatableText("cargument.entity.options.z.description"));
			putOption("dx", entitySelectorReader -> entitySelectorReader.setDx(entitySelectorReader.getReader().readDouble()), entitySelectorReader -> entitySelectorReader.getDx() == null, new TranslatableText("cargument.entity.options.dx.description"));
			putOption("dy", entitySelectorReader -> entitySelectorReader.setDy(entitySelectorReader.getReader().readDouble()), entitySelectorReader -> entitySelectorReader.getDy() == null, new TranslatableText("cargument.entity.options.dy.description"));
			putOption("dz", entitySelectorReader -> entitySelectorReader.setDz(entitySelectorReader.getReader().readDouble()), entitySelectorReader -> entitySelectorReader.getDz() == null, new TranslatableText("cargument.entity.options.dz.description"));
			putOption("x_rotation", entitySelectorReader -> entitySelectorReader.setPitchRange(FloatRangeArgument.parse(entitySelectorReader.getReader(), true, MathHelper::wrapDegrees)), entitySelectorReader -> entitySelectorReader.getPitchRange() == FloatRangeArgument.ANY, new TranslatableText("cargument.entity.options.x_rotation.description"));
			putOption("y_rotation", entitySelectorReader -> entitySelectorReader.setYawRange(FloatRangeArgument.parse(entitySelectorReader.getReader(), true, MathHelper::wrapDegrees)), entitySelectorReader -> entitySelectorReader.getYawRange() == FloatRangeArgument.ANY, new TranslatableText("cargument.entity.options.y_rotation.description"));
			putOption("limit", entitySelectorReader -> {
				int cursor = entitySelectorReader.getReader().getCursor();
				int j = entitySelectorReader.getReader().readInt();
				if (j < 1) {
					entitySelectorReader.getReader().setCursor(cursor);
					throw TOO_SMALL_LEVEL_EXCEPTION.createWithContext(entitySelectorReader.getReader());
				} else {
					entitySelectorReader.setLimit(j);
					entitySelectorReader.setHasLimit(true);
				}
			}, entitySelectorReader -> !entitySelectorReader.isSenderOnly() && !entitySelectorReader.hasLimit(), new TranslatableText("cargument.entity.options.limit.description"));
			putOption("sort", entitySelectorReader -> {
				int cursor = entitySelectorReader.getReader().getCursor();
				String string = entitySelectorReader.getReader().readUnquotedString();
				entitySelectorReader.setSuggestionProvider((suggestionsBuilder, consumer) -> CommandSource.suggestMatching(Arrays.asList("nearest", "furthest", "random", "arbitrary"), suggestionsBuilder));
				byte var5 = -1;
				switch(string.hashCode()) {
				case -938285885:
					if (string.equals("random")) {
						var5 = 2;
					}
					break;
				case 1510793967:
					if (string.equals("furthest")) {
						var5 = 1;
					}
					break;
				case 1780188658:
					if (string.equals("arbitrary")) {
						var5 = 3;
					}
					break;
				case 1825779806:
					if (string.equals("nearest")) {
						var5 = 0;
					}
				}

				BiConsumer<Vec3d, List<? extends Entity>> biConsumer5;
				switch (var5) {
					case 0 -> biConsumer5 = CEntitySelectorReader.NEAREST;
					case 1 -> biConsumer5 = CEntitySelectorReader.FURTHEST;
					case 2 -> biConsumer5 = CEntitySelectorReader.RANDOM;
					case 3 -> biConsumer5 = CEntitySelectorReader.ARBITRARY;
					default -> {
						entitySelectorReader.getReader().setCursor(cursor);
						throw IRREVERSIBLE_SORT_EXCEPTION.createWithContext(entitySelectorReader.getReader(), string);
					}
				}

				entitySelectorReader.setSorter(biConsumer5);
				entitySelectorReader.setHasSorter(true);
			}, entitySelectorReader -> !entitySelectorReader.isSenderOnly() && !entitySelectorReader.hasSorter(), new TranslatableText("cargument.entity.options.sort.description"));
			putOption("gamemode", entitySelectorReader -> {
				entitySelectorReader.setSuggestionProvider((suggestionsBuilder, consumer) -> {
					String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
					boolean bl = !entitySelectorReader.excludesGameMode();
					boolean bl2 = true;
					if (!remaining.isEmpty()) {
						if (remaining.charAt(0) == '!') {
							bl = false;
							remaining = remaining.substring(1);
						} else {
							bl2 = false;
						}
					}

					for (GameMode gameMode : GameMode.values()) {
						if (gameMode.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
							if (bl2) {
								suggestionsBuilder.suggest("!" + gameMode.getName());
							}

							if (bl) {
								suggestionsBuilder.suggest(gameMode.getName());
							}
						}
					}

					return suggestionsBuilder.buildFuture();
				});
				int cursor = entitySelectorReader.getReader().getCursor();
				boolean bl = entitySelectorReader.readNegationCharacter();
				if (entitySelectorReader.excludesGameMode() && !bl) {
					entitySelectorReader.getReader().setCursor(cursor);
					throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(entitySelectorReader.getReader(), "gamemode");
				} else {
					String string = entitySelectorReader.getReader().readUnquotedString();
					GameMode gameMode = GameMode.byName(string, null);
					if (gameMode == null) {
						entitySelectorReader.getReader().setCursor(cursor);
						throw INVALID_MODE_EXCEPTION.createWithContext(entitySelectorReader.getReader(), string);
					} else {
						entitySelectorReader.setIncludesNonPlayers(false);
						entitySelectorReader.setPredicate((entity) -> {
							if (!(entity instanceof AbstractClientPlayerEntity abstractPlayer)) {
								return false;
							} else {
								GameMode playerGameMode = client.getNetworkHandler().getPlayerList().stream()
										.filter(playerEntry -> playerEntry.getProfile().getName().equals(abstractPlayer.getName().getString()))
										.map(PlayerListEntry::getGameMode)
										.findAny().orElse(null);
								if (playerGameMode == null) {
									return false;
								}

								return bl == (playerGameMode != gameMode);
							}
						});
						if (bl) {
							entitySelectorReader.setHasNegatedGameMode(true);
						} else {
							entitySelectorReader.setSelectsGameMode(true);
						}

					}
				}
			}, entitySelectorReader -> !entitySelectorReader.selectsGameMode(), new TranslatableText("cargument.entity.options.gamemode.description"));
			putOption("team", entitySelectorReader -> {
				boolean bl = entitySelectorReader.readNegationCharacter();
				String string = entitySelectorReader.getReader().readUnquotedString();
				entitySelectorReader.setPredicate((entity) -> {
					if (!(entity instanceof LivingEntity)) {
						return false;
					} else {
						AbstractTeam abstractTeam = entity.getScoreboardTeam();
						String string2 = abstractTeam == null ? "" : abstractTeam.getName();
						return string2.equals(string) != bl;
					}
				});
				if (bl) {
					entitySelectorReader.setExcludesTeam(true);
				} else {
					entitySelectorReader.setSelectsTeam(true);
				}

			}, entitySelectorReader -> !entitySelectorReader.selectsTeam(), new TranslatableText("cargument.entity.options.team.description"));
			putOption("type", entitySelectorReader -> {
				entitySelectorReader.setSuggestionProvider((suggestionsBuilder, consumer) -> {
					CommandSource.suggestIdentifiers(Registry.ENTITY_TYPE.getIds(), suggestionsBuilder, String.valueOf('!'));
					CommandSource.suggestIdentifiers(EntityTypeTags.getTagGroup().getTagIds(), suggestionsBuilder, "!#");
					if (!entitySelectorReader.excludesEntityType()) {
						CommandSource.suggestIdentifiers(Registry.ENTITY_TYPE.getIds(), suggestionsBuilder);
						CommandSource.suggestIdentifiers(EntityTypeTags.getTagGroup().getTagIds(), suggestionsBuilder, String.valueOf('#'));
					}

					return suggestionsBuilder.buildFuture();
				});
				int cursor = entitySelectorReader.getReader().getCursor();
				boolean bl = entitySelectorReader.readNegationCharacter();
				if (entitySelectorReader.excludesEntityType() && !bl) {
					entitySelectorReader.getReader().setCursor(cursor);
					throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(entitySelectorReader.getReader(), "type");
				} else {
					if (bl) {
						entitySelectorReader.setExcludesEntityType();
					}

					Identifier identifier;
					if (entitySelectorReader.readTagCharacter()) {
						identifier = Identifier.fromCommandInput(entitySelectorReader.getReader());
						entitySelectorReader.setPredicate(entity -> entity.getType().isIn(entity.getEntityWorld().getTagManager().getOrCreateTagGroup(Registry.ENTITY_TYPE_KEY).getTagOrEmpty(identifier)) != bl);
					} else {
						identifier = Identifier.fromCommandInput(entitySelectorReader.getReader());
						EntityType<?> entityType = Registry.ENTITY_TYPE.getOrEmpty(identifier).orElseThrow(() -> {
							entitySelectorReader.getReader().setCursor(cursor);
							return INVALID_TYPE_EXCEPTION.createWithContext(entitySelectorReader.getReader(), identifier.toString());
						});
						if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
							entitySelectorReader.setIncludesNonPlayers(false);
						}

						entitySelectorReader.setPredicate(entity -> Objects.equals(entityType, entity.getType()) != bl);
						if (!bl) {
							entitySelectorReader.setEntityType(entityType);
						}
					}

				}
			}, entitySelectorReader -> !entitySelectorReader.selectsEntityType(), new TranslatableText("cargument.entity.options.type.description"));
			putOption("tag", entitySelectorReader -> {
				boolean bl = entitySelectorReader.readNegationCharacter();
				String string = entitySelectorReader.getReader().readUnquotedString();
				entitySelectorReader.setPredicate((entity) -> {
					if ("".equals(string)) {
						return entity.getScoreboardTags().isEmpty() != bl;
					} else {
						return entity.getScoreboardTags().contains(string) != bl;
					}
				});
			}, entitySelectorReader -> true, new TranslatableText("cargument.entity.options.tag.description"));
			putOption("nbt", entitySelectorReader -> {
				boolean bl = entitySelectorReader.readNegationCharacter();
				NbtCompound nbtCompound = (new StringNbtReader(entitySelectorReader.getReader())).parseCompound();
				entitySelectorReader.setPredicate(entity -> {
					NbtCompound nbtCompound2 = entity.writeNbt(new NbtCompound());
					if (entity instanceof AbstractClientPlayerEntity abstractPlayer) {
						ItemStack itemStack = abstractPlayer.getInventory().getMainHandStack();
						if (!itemStack.isEmpty()) {
							nbtCompound2.put("SelectedItem", itemStack.writeNbt(new NbtCompound()));
						}
					}

					return NbtHelper.matches(nbtCompound, nbtCompound2, true) != bl;
				});
			}, entitySelectorReader -> true, new TranslatableText("cargument.entity.options.nbt.description"));
			putOption("scores", entitySelectorReader -> {
				StringReader stringReader = entitySelectorReader.getReader();
				Map<String, NumberRange.IntRange> map = Maps.newHashMap();
				stringReader.expect('{');
				stringReader.skipWhitespace();

				while(stringReader.canRead() && stringReader.peek() != '}') {
					stringReader.skipWhitespace();
					String string = stringReader.readUnquotedString();
					stringReader.skipWhitespace();
					stringReader.expect('=');
					stringReader.skipWhitespace();
					NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
					map.put(string, intRange);
					stringReader.skipWhitespace();
					if (stringReader.canRead() && stringReader.peek() == ',') {
						stringReader.skip();
					}
				}

				stringReader.expect('}');
				if (!map.isEmpty()) {
					entitySelectorReader.setPredicate((entity) -> {
						Scoreboard scoreboard = entity.getEntityWorld().getScoreboard();
						String string = entity.getEntityName();
						Iterator<Entry<String, NumberRange.IntRange>> var4 = map.entrySet().iterator();

						Entry<String, NumberRange.IntRange> entry;
						int i;
						do {
							if (!var4.hasNext()) {
								return true;
							}

							entry = var4.next();
							ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(entry.getKey());
							if (scoreboardObjective == null) {
								return false;
							}

							if (!scoreboard.playerHasObjective(string, scoreboardObjective)) {
								return false;
							}

							ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(string, scoreboardObjective);
							i = scoreboardPlayerScore.getScore();
						} while(entry.getValue().test(i));

						return false;
					});
				}

				entitySelectorReader.setSelectsScores(true);
			}, entitySelectorReader -> !entitySelectorReader.selectsScores(), new TranslatableText("cargument.entity.options.scores.description"));
			putOption("advancements", entitySelectorReader -> {
				StringReader stringReader = entitySelectorReader.getReader();
				Map<Identifier, Predicate<AdvancementProgress>> map = Maps.newHashMap();
				stringReader.expect('{');
				stringReader.skipWhitespace();

				while (stringReader.canRead() && stringReader.peek() != '}') {
					stringReader.skipWhitespace();
					Identifier identifier = Identifier.fromCommandInput(stringReader);
					stringReader.skipWhitespace();
					stringReader.expect('=');
					stringReader.skipWhitespace();
					if (stringReader.canRead() && stringReader.peek() == '{') {
						Map<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
						stringReader.skipWhitespace();
						stringReader.expect('{');
						stringReader.skipWhitespace();

						while(stringReader.canRead() && stringReader.peek() != '}') {
							stringReader.skipWhitespace();
							String string = stringReader.readUnquotedString();
							stringReader.skipWhitespace();
							stringReader.expect('=');
							stringReader.skipWhitespace();
							boolean bl = stringReader.readBoolean();
							map2.put(string, (criterionProgress) -> criterionProgress.isObtained() == bl);
							stringReader.skipWhitespace();
							if (stringReader.canRead() && stringReader.peek() == ',') {
								stringReader.skip();
							}
						}

						stringReader.skipWhitespace();
						stringReader.expect('}');
						stringReader.skipWhitespace();
						map.put(identifier, (advancementProgress) -> {
							Iterator<Entry<String, Predicate<CriterionProgress>>> var2 = map2.entrySet().iterator();

							Entry<String, Predicate<CriterionProgress>> entry;
							CriterionProgress criterionProgress;
							do {
								if (!var2.hasNext()) {
									return true;
								}

								entry = var2.next();
								criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
							} while(criterionProgress != null && (entry.getValue()).test(criterionProgress));

							return false;
						});
					} else {
						boolean bl2 = stringReader.readBoolean();
						map.put(identifier, (advancementProgress) -> advancementProgress.isDone() == bl2);
					}

					stringReader.skipWhitespace();
					if (stringReader.canRead() && stringReader.peek() == ',') {
						stringReader.skip();
					}
				}

				stringReader.expect('}');
				if (!map.isEmpty()) {
					entitySelectorReader.setPredicate(entity -> false);
					entitySelectorReader.setIncludesNonPlayers(false);
				}

				entitySelectorReader.setSelectsAdvancements(true);
			}, entitySelectorReader -> !entitySelectorReader.selectsAdvancements(), new TranslatableText("cargument.entity.options.advancements.description"));
			putOption("predicate", entitySelectorReader -> entitySelectorReader.setPredicate(entity -> false), entitySelectorReader -> true, new TranslatableText("cargument.entity.options.predicate.description"));
		}
	}

	public static CEntitySelectorOptions.SelectorHandler getHandler(CEntitySelectorReader reader, String option, int restoreCursor) throws CommandSyntaxException {
		CEntitySelectorOptions.SelectorOption selectorOption = OPTIONS.get(option);
		if (selectorOption != null) {
			if (selectorOption.condition.test(reader)) {
				return selectorOption.handler;
			} else {
				throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
			}
		} else {
			reader.getReader().setCursor(restoreCursor);
			throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
		}
	}

	public static void suggestOptions(CEntitySelectorReader reader, SuggestionsBuilder suggestionBuilder) {
		String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

		for (Entry<String, SelectorOption> entry : OPTIONS.entrySet()) {
			if (entry.getValue().condition.test(reader) && (entry.getKey()).toLowerCase(Locale.ROOT).startsWith(string)) {
				suggestionBuilder.suggest(entry.getKey() + "=", entry.getValue().description);
			}
		}
	}

	private static class SelectorOption {
		public final CEntitySelectorOptions.SelectorHandler handler;
		public final Predicate<CEntitySelectorReader> condition;
		public final Text description;

		SelectorOption(CEntitySelectorOptions.SelectorHandler selectorHandler, Predicate<CEntitySelectorReader> predicate, Text text) {
			this.handler = selectorHandler;
			this.condition = predicate;
			this.description = text;
		}
	}

	public interface SelectorHandler {
		void handle(CEntitySelectorReader reader) throws CommandSyntaxException;
	}
}
