package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CEntitySelector {
	public static final BiConsumer<Vec3, List<? extends Entity>> ARBITRARY = (pos, entities) -> {
	};
	private static final EntityTypeTest<Entity, ?> PASSTHROUGH_FILTER = new EntityTypeTest<>() {
		@Override
        public Entity tryCast(Entity entity) {
            return entity;
        }

        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };
	private final int limit;
	private final boolean includesNonPlayers;
	private final Predicate<Entity> basePredicate;
	private final MinMaxBounds.Doubles distance;
	private final Function<Vec3, Vec3> positionOffset;
	@Nullable
	private final AABB aabb;
	private final BiConsumer<Vec3, List<? extends Entity>> sorter;
	private final boolean senderOnly;
	@Nullable
	private final String playerName;
	@Nullable
	private final UUID uuid;
	private final EntityTypeTest<Entity, ?> entityFilter;
	private final boolean usesAt;

	public CEntitySelector(int count, boolean includesNonPlayers, Predicate<Entity> basePredicate, MinMaxBounds.Doubles distance, Function<Vec3, Vec3> positionOffset, @Nullable AABB aabb, BiConsumer<Vec3, List<? extends Entity>> sorter, boolean senderOnly, @Nullable String playerName, @Nullable UUID uuid, @Nullable EntityType<?> type, boolean usesAt) {
		this.limit = count;
		this.includesNonPlayers = includesNonPlayers;
		this.basePredicate = basePredicate;
		this.distance = distance;
		this.positionOffset = positionOffset;
		this.aabb = aabb;
		this.sorter = sorter;
		this.senderOnly = senderOnly;
		this.playerName = playerName;
		this.uuid = uuid;
		this.entityFilter = type == null ? PASSTHROUGH_FILTER : type;
		this.usesAt = usesAt;
	}

	public int getLimit() {
		return this.limit;
	}

	public boolean includesNonPlayers() {
		return this.includesNonPlayers;
	}

	public boolean isSenderOnly() {
		return this.senderOnly;
	}

	public boolean usesAt() {
		return this.usesAt;
	}

	public Entity getEntity(FabricClientCommandSource source) throws CommandSyntaxException {
		List<? extends Entity> list = this.getEntities(source);
		if (list.isEmpty()) {
			throw CEntityArgument.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		if (list.size() > 1) {
			throw CEntityArgument.TOO_MANY_ENTITIES_EXCEPTION.create();
		}
		return list.getFirst();
	}

	public List<? extends Entity> getEntities(FabricClientCommandSource source) throws CommandSyntaxException {
		return this.getUnfilteredEntities(source).stream().filter(entity -> entity.getType().isEnabled(source.enabledFeatures())).toList();
	}

	public List<? extends Entity> getUnfilteredEntities(FabricClientCommandSource source) throws CommandSyntaxException {
		if (!this.includesNonPlayers) {
			return this.getPlayers(source);
		}
		if (this.playerName != null) {
			AbstractClientPlayer abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity instanceof AbstractClientPlayer)
				.map(entity -> (AbstractClientPlayer) entity)
				.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
				.findAny().orElse(null);
			return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
		}
		if (this.uuid != null) {
			Entity foundEntity = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity.getUUID().equals(this.uuid))
				.findAny().orElse(null);
			return foundEntity == null ? Collections.emptyList() : Lists.newArrayList(foundEntity);
		}

		Vec3 vec3 = this.positionOffset.apply(source.getPosition());
		Predicate<Entity> predicate = this.getPositionPredicate(vec3);
		if (this.senderOnly) {
			return source.getEntity() != null && predicate.test(source.getEntity())
				? Lists.newArrayList(source.getEntity())
				: Collections.emptyList();
		}
		List<Entity> list = Lists.newArrayList();
		this.appendEntitiesFromWorld(list, source.getWorld(), vec3, predicate);
		return this.getEntities(vec3, list);
	}

	private void appendEntitiesFromWorld(List<Entity> entities, ClientLevel world, Vec3 pos, Predicate<Entity> predicate) {
		int appendLimit = this.getAppendLimit();
		if (entities.size() < appendLimit) {
			if (this.aabb != null) {
				world.getEntities(this.entityFilter, this.aabb.move(pos), predicate, entities, appendLimit);
			} else {
				world.getEntities().get(this.entityFilter, entity -> {
					if (predicate.test(entity)) {
						entities.add(entity);
						if (entities.size() >= appendLimit) {
							return AbortableIterationConsumer.Continuation.ABORT;
						}
					}

					return AbortableIterationConsumer.Continuation.CONTINUE;
				});
			}
		}
	}

	private int getAppendLimit() {
		return this.sorter == ARBITRARY ? this.limit : Integer.MAX_VALUE;
	}

	public AbstractClientPlayer getPlayer(FabricClientCommandSource source) throws CommandSyntaxException {
		List<AbstractClientPlayer> list = this.getPlayers(source);
		if (list.size() != 1) {
			throw EntityArgument.NO_PLAYERS_FOUND.create();
		}
		return list.getFirst();
	}

	public List<AbstractClientPlayer> getPlayers(FabricClientCommandSource source) throws CommandSyntaxException {
		AbstractClientPlayer abstractClientPlayer;
		if (this.playerName != null) {
			abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity instanceof AbstractClientPlayer)
				.map(entity -> (AbstractClientPlayer) entity)
				.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
				.findAny().orElse(null);
			return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
		}
		if (this.uuid != null) {
			abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity instanceof AbstractClientPlayer)
				.map(entity -> (AbstractClientPlayer) entity)
				.filter(entity -> entity.getUUID().equals(this.uuid))
				.findAny().orElse(null);
			return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
		}
		Vec3 vec3d = this.positionOffset.apply(source.getPosition());
		Predicate<Entity> predicate = this.getPositionPredicate(vec3d);
		if (this.senderOnly) {
			if (source.getEntity() instanceof AbstractClientPlayer player && predicate.test(player)) {
				return Lists.newArrayList(player);
			}

			return Collections.emptyList();
		}
		List<AbstractClientPlayer> entities = source.getWorld().players().stream()
			.filter(predicate)
			.limit(this.getAppendLimit())
			.collect(Collectors.toList());

		return this.getEntities(vec3d, entities);
	}

	private Predicate<Entity> getPositionPredicate(Vec3 pos) {
		Predicate<Entity> predicate = this.basePredicate;
		if (this.aabb != null) {
			AABB aabb = this.aabb.move(pos);
			predicate = predicate.and(entity -> aabb.intersects(entity.getBoundingBox()));
		}

		if (!this.distance.isAny()) {
			predicate = predicate.and(entity -> this.distance.matchesSqr(entity.distanceToSqr(pos)));
		}

		return predicate;
	}

	private <T extends Entity> List<T> getEntities(Vec3 pos, List<T> entities) {
		if (entities.size() > 1) {
			this.sorter.accept(pos, entities);
		}

		return entities.subList(0, Math.min(this.limit, entities.size()));
	}

	public static Component getNames(List<? extends Entity> entities) {
		return ComponentUtils.formatList(entities, Entity::getDisplayName);
	}
}
