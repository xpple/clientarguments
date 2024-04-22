package dev.xpple.clientarguments.arguments;

import com.google.common.primitives.Doubles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.FabricEntitySelectorReader;
import net.minecraft.advancements.critereon.WrappedMinMaxBounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer; import java.util.function.BiFunction; import java.util.function.Consumer; import java.util.function.Predicate; import java.util.function.Function; import java.util.function.ToDoubleFunction; 

public class CEntitySelectorReader implements FabricEntitySelectorReader {
	public static final SimpleCommandExceptionType INVALID_ENTITY_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.invalid"));
	public static final DynamicCommandExceptionType UNKNOWN_SELECTOR_EXCEPTION = new DynamicCommandExceptionType(selectorType -> Component.translatableEscape("argument.entity.selector.unknown", selectorType));
	public static final SimpleCommandExceptionType NOT_ALLOWED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.selector.not_allowed"));
	public static final SimpleCommandExceptionType MISSING_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.selector.missing"));
	public static final SimpleCommandExceptionType UNTERMINATED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.options.unterminated"));
	public static final DynamicCommandExceptionType VALUELESS_EXCEPTION = new DynamicCommandExceptionType(option -> Component.translatableEscape("argument.entity.options.valueless", option));
	public static final BiConsumer<Vec3, List<? extends Entity>> NEAREST = (pos, entities) -> entities.sort((entity1, entity2) -> Doubles.compare(entity1.distanceToSqr(pos), entity2.distanceToSqr(pos)));
	public static final BiConsumer<Vec3, List<? extends Entity>> FURTHEST = (pos, entities) -> entities.sort((entity1, entity2) -> Doubles.compare(entity2.distanceToSqr(pos), entity1.distanceToSqr(pos)));
	public static final BiConsumer<Vec3, List<? extends Entity>> RANDOM = (pos, entities) -> Collections.shuffle(entities);
	public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> DEFAULT_SUGGESTION_PROVIDER = (builder, consumer) -> builder.buildFuture();
	private final StringReader reader;
	private final boolean atAllowed;
	private int limit;
	private boolean includesNonPlayers;
	private MinMaxBounds.Doubles distance = MinMaxBounds.Doubles.ANY;
	private MinMaxBounds.Ints levelRange = MinMaxBounds.Ints.ANY;
	@Nullable
	private Double x;
	@Nullable
	private Double y;
	@Nullable
	private Double z;
	@Nullable
	private Double dx;
	@Nullable
	private Double dy;
	@Nullable
	private Double dz;
	private WrappedMinMaxBounds pitchRange = WrappedMinMaxBounds.ANY;
	private WrappedMinMaxBounds yawRange = WrappedMinMaxBounds.ANY;
	private Predicate<Entity> predicate = entity -> true;
	private BiConsumer<Vec3, List<? extends Entity>> sorter = CEntitySelector.ARBITRARY;
	private boolean senderOnly;
	@Nullable
	private String playerName;
	private int startCursor;
	@Nullable
	private UUID uuid;
	private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestionProvider = DEFAULT_SUGGESTION_PROVIDER;
	private boolean selectsName;
	private boolean excludesName;
	private boolean hasLimit;
	private boolean hasSorter;
	private boolean selectsGameMode;
	private boolean excludesGameMode;
	private boolean selectsTeam;
	private boolean excludesTeam;
	@Nullable
	private EntityType<?> entityType;
	private boolean excludesEntityType;
	private boolean selectsScores;
	private boolean selectsAdvancements;
	private boolean usesAt;

	public CEntitySelectorReader(StringReader reader) {
		this(reader, true);
	}

	public CEntitySelectorReader(StringReader reader, boolean atAllowed) {
		this.reader = reader;
		this.atAllowed = atAllowed;
	}

	public CEntitySelector build() {
		AABB aabb;
		if (this.dx == null && this.dy == null && this.dz == null) {
			if (this.distance.max().isPresent()) {
				double d = this.distance.max().get();
				aabb = new AABB(-d, -d, -d, d + 1.0, d + 1.0, d + 1.0);
			} else {
				aabb = null;
			}
		} else {
			aabb = this.createBox(this.dx == null ? 0.0 : this.dx, this.dy == null ? 0.0 : this.dy, this.dz == null ? 0.0 : this.dz);
		}

		Function<Vec3, Vec3> function;
		if (this.x == null && this.y == null && this.z == null) {
			function = pos -> pos;
		} else {
			function = pos -> new Vec3(this.x == null ? pos.x : this.x, this.y == null ? pos.y : this.y, this.z == null ? pos.z : this.z);
		}

		return new CEntitySelector(this.limit, this.includesNonPlayers, this.predicate, this.distance, function, aabb, this.sorter, this.senderOnly, this.playerName, this.uuid, this.entityType, this.usesAt);
	}

	private AABB createBox(double x, double y, double z) {
		boolean bl = x < 0.0;
		boolean bl2 = y < 0.0;
		boolean bl3 = z < 0.0;
		double d = bl ? x : 0.0;
		double e = bl2 ? y : 0.0;
		double f = bl3 ? z : 0.0;
		double g = (bl ? 0.0 : x) + 1.0;
		double h = (bl2 ? 0.0 : y) + 1.0;
		double i = (bl3 ? 0.0 : z) + 1.0;
		return new AABB(d, e, f, g, h, i);
	}

	private void buildPredicate() {
		if (this.pitchRange != WrappedMinMaxBounds.ANY) {
			this.predicate = this.predicate.and(this.rotationPredicate(this.pitchRange, Entity::getXRot));
		}

		if (this.yawRange != WrappedMinMaxBounds.ANY) {
			this.predicate = this.predicate.and(this.rotationPredicate(this.yawRange, Entity::getYRot));
		}

		if (!this.levelRange.isAny()) {
			this.predicate = this.predicate.and(entity -> entity instanceof ServerPlayer && this.levelRange.matches(((ServerPlayer) entity).experienceLevel));
		}
	}

	private Predicate<Entity> rotationPredicate(WrappedMinMaxBounds angleRange, ToDoubleFunction<Entity> entityToAngle) {
		double d = Mth.wrapDegrees(angleRange.min() == null ? 0.0F : angleRange.min());
		double e = Mth.wrapDegrees(angleRange.max() == null ? 359.0F : angleRange.max());
		return entity -> {
			double f = Mth.wrapDegrees(entityToAngle.applyAsDouble(entity));
			if (d > e) {
				return f >= d || f <= e;
			} else {
				return f >= d && f <= e;
			}
		};
	}

	protected void readAtVariable() throws CommandSyntaxException {
		this.usesAt = true;
		this.suggestionProvider = this::suggestSelectorRest;
		if (!this.reader.canRead()) {
			throw MISSING_EXCEPTION.createWithContext(this.reader);
		}
		int i = this.reader.getCursor();
		char c = this.reader.read();
		if (c == 'p') {
			this.limit = 1;
			this.includesNonPlayers = false;
			this.sorter = NEAREST;
			this.setEntityType(EntityType.PLAYER);
		} else if (c == 'a') {
			this.limit = Integer.MAX_VALUE;
			this.includesNonPlayers = false;
			this.sorter = CEntitySelector.ARBITRARY;
			this.setEntityType(EntityType.PLAYER);
		} else if (c == 'r') {
			this.limit = 1;
			this.includesNonPlayers = false;
			this.sorter = RANDOM;
			this.setEntityType(EntityType.PLAYER);
		} else if (c == 's') {
			this.limit = 1;
			this.includesNonPlayers = true;
			this.senderOnly = true;
		} else {
			if (c != 'e') {
				this.reader.setCursor(i);
				throw UNKNOWN_SELECTOR_EXCEPTION.createWithContext(this.reader, "@" + c);
			}

			this.limit = Integer.MAX_VALUE;
			this.includesNonPlayers = true;
			this.sorter = CEntitySelector.ARBITRARY;
			this.predicate = Entity::isAlive;
		}

		this.suggestionProvider = this::suggestOpen;
		if (this.reader.canRead() && this.reader.peek() == '[') {
			this.reader.skip();
			this.suggestionProvider = this::suggestOptionOrEnd;
			this.readArguments();
		}
	}

	protected void readRegular() throws CommandSyntaxException {
		if (this.reader.canRead()) {
			this.suggestionProvider = this::suggestNormal;
		}

		int cursor = this.reader.getCursor();
		String string = this.reader.readString();

		try {
			this.uuid = UUID.fromString(string);
			this.includesNonPlayers = true;
		} catch (IllegalArgumentException e) {
			if (string.isEmpty() || string.length() > 16) {
				this.reader.setCursor(cursor);
				throw INVALID_ENTITY_EXCEPTION.createWithContext(this.reader);
			}

			this.includesNonPlayers = false;
			this.playerName = string;
		}

		this.limit = 1;
	}

	protected void readArguments() throws CommandSyntaxException {
		this.suggestionProvider = this::suggestOption;
		this.reader.skipWhitespace();

		while (this.reader.canRead() && this.reader.peek() != ']') {
			this.reader.skipWhitespace();
			int i = this.reader.getCursor();
			String string = this.reader.readString();
			CEntitySelectorOptions.SelectorHandler selectorHandler = CEntitySelectorOptions.getHandler(this, string, i);
			this.reader.skipWhitespace();
			if (!this.reader.canRead() || this.reader.peek() != '=') {
				this.reader.setCursor(i);
				throw VALUELESS_EXCEPTION.createWithContext(this.reader, string);
			}

			this.reader.skip();
			this.reader.skipWhitespace();
			this.suggestionProvider = DEFAULT_SUGGESTION_PROVIDER;
			selectorHandler.handle(this);
			this.reader.skipWhitespace();
			this.suggestionProvider = this::suggestEndNext;
			if (this.reader.canRead()) {
				if (this.reader.peek() != ',') {
					if (this.reader.peek() != ']') {
						throw UNTERMINATED_EXCEPTION.createWithContext(this.reader);
					}
					break;
				}

				this.reader.skip();
				this.suggestionProvider = this::suggestOption;
			}
		}

        if (!this.reader.canRead()) {
            throw UNTERMINATED_EXCEPTION.createWithContext(this.reader);
        }
		this.reader.skip();
		this.suggestionProvider = DEFAULT_SUGGESTION_PROVIDER;
    }

	public boolean readNegationCharacter() {
		this.reader.skipWhitespace();
		if (this.reader.canRead() && this.reader.peek() == '!') {
			this.reader.skip();
			this.reader.skipWhitespace();
			return true;
		}
		return false;
	}

	public boolean readTagCharacter() {
		this.reader.skipWhitespace();
		if (this.reader.canRead() && this.reader.peek() == '#') {
			this.reader.skip();
			this.reader.skipWhitespace();
			return true;
		}
		return false;
	}

	public StringReader getReader() {
		return this.reader;
	}

	public void setPredicate(Predicate<Entity> predicate) {
		this.predicate = this.predicate.and(predicate);
	}

	public MinMaxBounds.Doubles getDistance() {
		return this.distance;
	}

	public void setDistance(MinMaxBounds.Doubles distance) {
		this.distance = distance;
	}

	public MinMaxBounds.Ints getLevelRange() {
		return this.levelRange;
	}

	public void setLevelRange(MinMaxBounds.Ints levelRange) {
		this.levelRange = levelRange;
	}

	public WrappedMinMaxBounds getPitchRange() {
		return this.pitchRange;
	}

	public void setPitchRange(WrappedMinMaxBounds pitchRange) {
		this.pitchRange = pitchRange;
	}

	public WrappedMinMaxBounds getYawRange() {
		return this.yawRange;
	}

	public void setYawRange(WrappedMinMaxBounds yawRange) {
		this.yawRange = yawRange;
	}

	@Nullable
	public Double getX() {
		return this.x;
	}

	@Nullable
	public Double getY() {
		return this.y;
	}

	@Nullable
	public Double getZ() {
		return this.z;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public void setDx(double dx) {
		this.dx = dx;
	}

	public void setDy(double dy) {
		this.dy = dy;
	}

	public void setDz(double dz) {
		this.dz = dz;
	}

	@Nullable
	public Double getDx() {
		return this.dx;
	}

	@Nullable
	public Double getDy() {
		return this.dy;
	}

	@Nullable
	public Double getDz() {
		return this.dz;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setIncludesNonPlayers(boolean includesNonPlayers) {
		this.includesNonPlayers = includesNonPlayers;
	}

	public BiConsumer<Vec3, List<? extends Entity>> getSorter() {
		return this.sorter;
	}

	public void setSorter(BiConsumer<Vec3, List<? extends Entity>> sorter) {
		this.sorter = sorter;
	}

	public CEntitySelector read() throws CommandSyntaxException {
		this.startCursor = this.reader.getCursor();
		this.suggestionProvider = this::suggestSelector;
		if (this.reader.canRead() && this.reader.peek() == '@') {
			if (!this.atAllowed) {
				throw NOT_ALLOWED_EXCEPTION.createWithContext(this.reader);
			}

			this.reader.skip();
			this.readAtVariable();
		} else {
			this.readRegular();
		}

		this.buildPredicate();
		return this.build();
	}

	private static void suggestSelector(SuggestionsBuilder builder) {
		builder.suggest("@p", Component.translatable("argument.entity.selector.nearestPlayer"));
		builder.suggest("@a", Component.translatable("argument.entity.selector.allPlayers"));
		builder.suggest("@r", Component.translatable("argument.entity.selector.randomPlayer"));
		builder.suggest("@s", Component.translatable("argument.entity.selector.self"));
		builder.suggest("@e", Component.translatable("argument.entity.selector.allEntities"));
	}

	private CompletableFuture<Suggestions> suggestSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		consumer.accept(builder);
		if (this.atAllowed) {
			suggestSelector(builder);
		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestNormal(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		SuggestionsBuilder suggestionsBuilder = builder.createOffset(this.startCursor);
		consumer.accept(suggestionsBuilder);
		return builder.add(suggestionsBuilder).buildFuture();
	}

	private CompletableFuture<Suggestions> suggestSelectorRest(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		SuggestionsBuilder suggestionsBuilder = builder.createOffset(builder.getStart() - 1);
		suggestSelector(suggestionsBuilder);
		builder.add(suggestionsBuilder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOpen(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		builder.suggest(String.valueOf('['));
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOptionOrEnd(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		builder.suggest(String.valueOf(']'));
		CEntitySelectorOptions.suggestOptions(this, builder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOption(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		CEntitySelectorOptions.suggestOptions(this, builder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestEndNext(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		builder.suggest(String.valueOf(','));
		builder.suggest(String.valueOf(']'));
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestDefinerNext(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		builder.suggest(String.valueOf('='));
		return builder.buildFuture();
	}

	public boolean isSenderOnly() {
		return this.senderOnly;
	}

	public void setSuggestionProvider(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestionProvider) {
		this.suggestionProvider = suggestionProvider;
	}

	public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
		return this.suggestionProvider.apply(builder.createOffset(this.reader.getCursor()), consumer);
	}

	public boolean selectsName() {
		return this.selectsName;
	}

	public void setSelectsName(boolean selectsName) {
		this.selectsName = selectsName;
	}

	public boolean excludesName() {
		return this.excludesName;
	}

	public void setExcludesName(boolean excludesName) {
		this.excludesName = excludesName;
	}

	public boolean hasLimit() {
		return this.hasLimit;
	}

	public void setHasLimit(boolean hasLimit) {
		this.hasLimit = hasLimit;
	}

	public boolean hasSorter() {
		return this.hasSorter;
	}

	public void setHasSorter(boolean hasSorter) {
		this.hasSorter = hasSorter;
	}

	public boolean selectsGameMode() {
		return this.selectsGameMode;
	}

	public void setSelectsGameMode(boolean selectsGameMode) {
		this.selectsGameMode = selectsGameMode;
	}

	public boolean excludesGameMode() {
		return this.excludesGameMode;
	}

	public void setExcludesGameMode(boolean excludesGameMode) {
		this.excludesGameMode = excludesGameMode;
	}

	public boolean selectsTeam() {
		return this.selectsTeam;
	}

	public void setSelectsTeam(boolean selectsTeam) {
		this.selectsTeam = selectsTeam;
	}

	public boolean excludesTeam() {
		return this.excludesTeam;
	}

	public void setExcludesTeam(boolean excludesTeam) {
		this.excludesTeam = excludesTeam;
	}

	public void setEntityType(@Nullable EntityType<?> entityType) {
		this.entityType = entityType;
	}

	public void setExcludesEntityType() {
		this.excludesEntityType = true;
	}

	public boolean selectsEntityType() {
		return this.entityType != null;
	}

	public boolean excludesEntityType() {
		return this.excludesEntityType;
	}

	public boolean selectsScores() {
		return this.selectsScores;
	}

	public void setSelectsScores(boolean selectsScores) {
		this.selectsScores = selectsScores;
	}

	public boolean selectsAdvancements() {
		return this.selectsAdvancements;
	}

	public void setSelectsAdvancements(boolean selectsAdvancements) {
		this.selectsAdvancements = selectsAdvancements;
	}
}
