package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CEntitySelector {

	private static final TypeFilter<Entity, ?> PASSTHROUGH_FILTER = new TypeFilter<>() {
		public Entity downcast(Entity entity) {
			return entity;
		}

		public Class<? extends Entity> getBaseClass() {
			return Entity.class;
		}
	};
	private final int limit;
	private final boolean includesNonPlayers;
	private final Predicate<Entity> basePredicate;
	private final NumberRange.FloatRange distance;
	private final Function<Vec3d, Vec3d> positionOffset;
	@Nullable
	private final Box box;
	private final BiConsumer<Vec3d, List<? extends Entity>> sorter;
	private final boolean senderOnly;
	@Nullable
	private final String playerName;
	@Nullable
	private final UUID uuid;
	private final TypeFilter<Entity, ?> entityFilter;
	private final boolean usesAt;

	public CEntitySelector(int count, boolean includesNonPlayers, Predicate<Entity> basePredicate, NumberRange.FloatRange distance, Function<Vec3d, Vec3d> positionOffset, @Nullable Box box, BiConsumer<Vec3d, List<? extends Entity>> sorter, boolean senderOnly, @Nullable String playerName, @Nullable UUID uuid, @Nullable EntityType<?> type, boolean usesAt) {
		this.limit = count;
		this.includesNonPlayers = includesNonPlayers;
		this.basePredicate = basePredicate;
		this.distance = distance;
		this.positionOffset = positionOffset;
		this.box = box;
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

	public Entity getEntity(FabricClientCommandSource fabricClientCommandSource) throws CommandSyntaxException {
		List<? extends Entity> list = this.getEntities(fabricClientCommandSource);
		if (list.isEmpty()) {
			throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		} else if (list.size() > 1) {
			throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
		}
		return list.get(0);
	}

	public List<? extends Entity> getEntities(FabricClientCommandSource source) throws CommandSyntaxException {
		if (!this.includesNonPlayers) {
			return this.getPlayers(source);
		} else if (this.playerName != null) {
			AbstractClientPlayerEntity abstractClientPlayerEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity instanceof AbstractClientPlayerEntity)
					.map(entity -> (AbstractClientPlayerEntity) entity)
					.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
					.findAny().orElse(null);
			return abstractClientPlayerEntity == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayerEntity);
		} else if (this.uuid != null) {
			Entity foundEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity.getUuid().equals(this.uuid))
					.findAny().orElse(null);

			return foundEntity == null ? Collections.emptyList() : Lists.newArrayList(foundEntity);
		} else {
			Vec3d vec3d = this.positionOffset.apply(source.getPosition());
			Predicate<Entity> predicate = this.getPositionPredicate(vec3d);
			if (this.senderOnly) {
				return source.getEntity() != null && predicate.test(source.getEntity()) ? Lists.newArrayList(source.getEntity()) : Collections.emptyList();
			} else {
				List<Entity> list = new ArrayList<>();
				this.appendEntitiesFromWorld(list, source.getWorld(), vec3d, predicate);
				return this.getEntities(vec3d, list);
			}
		}
	}

	private void appendEntitiesFromWorld(List<Entity> list, ClientWorld clientWorld, Vec3d vec3d, Predicate<Entity> predicate) {
		if (this.box != null) {
			list.addAll(clientWorld.getEntitiesByType(this.entityFilter, this.box.offset(vec3d), predicate));
		} else {
			final int border = 30_000_000;
			list.addAll(clientWorld.getEntitiesByType(this.entityFilter, new Box(-border, 0, -border, border, 255, border), predicate));
		}
	}

	public AbstractClientPlayerEntity getPlayer(FabricClientCommandSource source) throws CommandSyntaxException {
		List<AbstractClientPlayerEntity> list = this.getPlayers(source);
		if (list.size() != 1) {
			throw CEntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
		} else {
			return list.get(0);
		}
	}

	public List<AbstractClientPlayerEntity> getPlayers(FabricClientCommandSource source) throws CommandSyntaxException {
		AbstractClientPlayerEntity abstractClientPlayerEntity;
		if (this.playerName != null) {
			abstractClientPlayerEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity instanceof AbstractClientPlayerEntity)
					.map(entity -> (AbstractClientPlayerEntity) entity)
					.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
					.findAny().orElse(null);
			return abstractClientPlayerEntity == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayerEntity);
		} else if (this.uuid != null) {
			abstractClientPlayerEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity.getUuid().equals(this.uuid))
					.filter(entity -> entity instanceof AbstractClientPlayerEntity)
					.map(entity -> (AbstractClientPlayerEntity) entity)
					.findAny().orElse(null);
			return abstractClientPlayerEntity == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayerEntity);
		} else {
			Vec3d vec3d = this.positionOffset.apply(source.getPosition());
			Predicate<Entity> predicate = this.getPositionPredicate(vec3d);
			if (this.senderOnly) {
				if (source.getEntity() instanceof AbstractClientPlayerEntity player) {
					if (predicate.test(player)) {
						return Lists.newArrayList(player);
					}
				}
				return Collections.emptyList();
			} else {
				List<AbstractClientPlayerEntity> entities = source.getWorld().getPlayers().stream()
							.filter(predicate)
							.collect(Collectors.toList());

				return this.getEntities(vec3d, entities);
			}
		}
	}

	private Predicate<Entity> getPositionPredicate(Vec3d vec3d) {
		Predicate<Entity> predicate = this.basePredicate;
		if (this.box != null) {
			Box box = this.box.offset(vec3d);
			predicate = predicate.and((entity) -> box.intersects(entity.getBoundingBox()));
		}

		if (!this.distance.isDummy()) {
			predicate = predicate.and((entity) -> this.distance.testSqrt(entity.squaredDistanceTo(vec3d)));
		}

		return predicate;
	}

	private <T extends Entity> List<T> getEntities(Vec3d vec3d, List<T> list) {
		if (list.size() > 1) {
			this.sorter.accept(vec3d, list);
		}

		return list.subList(0, Math.min(this.limit, list.size()));
	}

	public static Text getNames(List<? extends Entity> list) {
		return Texts.join(list, Entity::getDisplayName);
	}
}
