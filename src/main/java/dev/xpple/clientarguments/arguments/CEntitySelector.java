package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.entity.EntityTypeTest;
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
	public static final int INFINITE = Integer.MAX_VALUE;
	public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_ARBITRARY = (center, entityList) -> {};
	private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<>() {
        public Entity tryCast(Entity entity) {
            return entity;
        }

        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };
	private final int maxResults;
	private final boolean includesEntities;
	private final boolean worldLimited;
	private final List<Predicate<Entity>> contextFreePredicates;
	private final MinMaxBounds.Doubles range;
	private final Function<Vec3, Vec3> position;
	@Nullable
	private final AABB aabb;
	private final BiConsumer<Vec3, List<? extends Entity>> order;
	private final boolean currentEntity;
	@Nullable
	private final String playerName;
	@Nullable
	private final UUID entityUUID;
	private final EntityTypeTest<Entity, ?> type;
	private final boolean usesSelector;

	public CEntitySelector(int maxResults, boolean includesEntities, boolean worldLimited, List<Predicate<Entity>> contextFreePredicates, MinMaxBounds.Doubles range, Function<Vec3, Vec3> position, @Nullable AABB aabb, BiConsumer<Vec3, List<? extends Entity>> order, boolean currentEntity, @Nullable String playerName, @Nullable UUID entityUUID, @Nullable EntityType<?> type, boolean usesSelector) {
		this.maxResults = maxResults;
		this.includesEntities = includesEntities;
		this.worldLimited = worldLimited;
		this.contextFreePredicates = contextFreePredicates;
		this.range = range;
		this.position = position;
		this.aabb = aabb;
		this.order = order;
		this.currentEntity = currentEntity;
		this.playerName = playerName;
		this.entityUUID = entityUUID;
		this.type = type == null ? ANY_TYPE : type;
		this.usesSelector = usesSelector;
	}

	public int getMaxResults() {
		return this.maxResults;
	}

	public boolean includesEntities() {
		return this.includesEntities;
	}

	public boolean isSelfSelector() {
		return this.currentEntity;
	}

	public boolean isWorldLimited() {
		return this.worldLimited;
	}

	public boolean usesSelector() {
		return this.usesSelector;
	}

	public Entity findSingleEntity(FabricClientCommandSource source) throws CommandSyntaxException {
		List<? extends Entity> list = this.findEntities(source);
		if (list.isEmpty()) {
			throw CEntityArgument.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		if (list.size() > 1) {
			throw CEntityArgument.TOO_MANY_ENTITIES_EXCEPTION.create();
		}
		return list.getFirst();
	}

	public List<? extends Entity> findEntities(FabricClientCommandSource source) throws CommandSyntaxException {
		if (!this.includesEntities) {
			return this.findPlayers(source);
		}
		if (this.playerName != null) {
			AbstractClientPlayer abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity instanceof AbstractClientPlayer)
				.map(entity -> (AbstractClientPlayer) entity)
				.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
				.findAny().orElse(null);
			return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
		}
		if (this.entityUUID != null) {
			Entity foundEntity = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity.getUUID().equals(this.entityUUID))
				.findAny().orElse(null);
			return foundEntity == null ? Collections.emptyList() : Lists.newArrayList(foundEntity);
		}

		Vec3 vec3 = this.position.apply(source.getPosition());
		AABB aabb = this.getAbsoluteAabb(vec3);
		Predicate<Entity> predicate = this.getPredicate(vec3, aabb, null);
		if (this.currentEntity) {
			return source.getEntity() != null && predicate.test(source.getEntity())
				? Lists.newArrayList(source.getEntity())
				: Collections.emptyList();
		}
		List<Entity> list = Lists.newArrayList();
		this.addEntities(list, source.getWorld(), aabb, predicate);
		return list;
	}

	private void addEntities(List<Entity> entities, ClientLevel level, @Nullable AABB box, Predicate<Entity> predicate) {
		int resultLimit = this.getResultLimit();
		if (entities.size() < resultLimit) {
			if (box != null) {
				level.getEntities(this.type, box, predicate, entities, resultLimit);
			} else {
				level.getEntities().get(this.type, entity -> {
					if (predicate.test(entity)) {
						entities.add(entity);
						if (entities.size() >= maxResults) {
							return AbortableIterationConsumer.Continuation.ABORT;
						}
					}

					return AbortableIterationConsumer.Continuation.CONTINUE;
				});
			}
		}
	}

	private int getResultLimit() {
		return this.order == ORDER_ARBITRARY ? this.maxResults : INFINITE;
	}

	public AbstractClientPlayer findSinglePlayer(FabricClientCommandSource source) throws CommandSyntaxException {
		List<AbstractClientPlayer> list = this.findPlayers(source);
		if (list.size() != 1) {
			throw CEntityArgument.PLAYER_NOT_FOUND_EXCEPTION.create();
		}
		return list.getFirst();
	}

	public List<AbstractClientPlayer> findPlayers(FabricClientCommandSource source) throws CommandSyntaxException {
		AbstractClientPlayer abstractClientPlayer;
		if (this.playerName != null) {
			abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity instanceof AbstractClientPlayer)
				.map(entity -> (AbstractClientPlayer) entity)
				.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
				.findAny().orElse(null);
			return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
		}
		if (this.entityUUID != null) {
			abstractClientPlayer = Streams.stream(source.getWorld().entitiesForRendering())
				.filter(entity -> entity instanceof AbstractClientPlayer)
				.map(entity -> (AbstractClientPlayer) entity)
				.filter(entity -> entity.getUUID().equals(this.entityUUID))
				.findAny().orElse(null);
			return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
		}
		Vec3 vec3d = this.position.apply(source.getPosition());
		Predicate<Entity> predicate = this.getPredicate(vec3d, this.getAbsoluteAabb(vec3d), null);
		if (this.currentEntity) {
			if (source.getEntity() instanceof AbstractClientPlayer player && predicate.test(player)) {
				return Lists.newArrayList(player);
			}

			return Collections.emptyList();
		}
		List<AbstractClientPlayer> entities = source.getWorld().players().stream()
			.filter(predicate)
			.limit(this.getResultLimit())
			.collect(Collectors.toList());

		return this.sortAndLimit(vec3d, entities);
	}

	@Nullable
	private AABB getAbsoluteAabb(Vec3 pos) {
		return this.aabb != null ? this.aabb.move(pos) : null;
	}

	private Predicate<Entity> getPredicate(Vec3 pos, @Nullable AABB box, @Nullable FeatureFlagSet enabledFeatures) {
		boolean bl = enabledFeatures != null;
		boolean bl2 = box != null;
		boolean bl3 = !this.range.isAny();
		int i = (bl ? 1 : 0) + (bl2 ? 1 : 0) + (bl3 ? 1 : 0);
		List<Predicate<Entity>> list;
		if (i == 0) {
			list = this.contextFreePredicates;
		} else {
			List<Predicate<Entity>> list2 = new ObjectArrayList<>(this.contextFreePredicates.size() + i);
			list2.addAll(this.contextFreePredicates);
			if (bl) {
				list2.add(entity -> entity.getType().isEnabled(enabledFeatures));
			}

			if (bl2) {
				list2.add(entity -> box.intersects(entity.getBoundingBox()));
			}

			if (bl3) {
				list2.add(entity -> this.range.matchesSqr(entity.distanceToSqr(pos)));
			}

			list = list2;
		}

		return Util.allOf(list);
	}

	private <T extends Entity> List<T> sortAndLimit(Vec3 pos, List<T> entities) {
		if (entities.size() > 1) {
			this.order.accept(pos, entities);
		}

		return entities.subList(0, Math.min(this.maxResults, entities.size()));
	}

	public static Component joinNames(List<? extends Entity> names) {
		return ComponentUtils.formatList(names, Entity::getDisplayName);
	}
}
