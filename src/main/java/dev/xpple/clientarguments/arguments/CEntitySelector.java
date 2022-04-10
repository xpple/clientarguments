package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
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
		@Override
		public Entity downcast(Entity entity) {
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

	public Entity getEntity(FabricClientCommandSource source) throws CommandSyntaxException {
		List<? extends Entity> list = this.getEntities(source);
		if (list.isEmpty()) {
			throw CEntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		if (list.size() > 1) {
			throw CEntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
		}
		return list.get(0);
	}

	public List<? extends Entity> getEntities(FabricClientCommandSource source) throws CommandSyntaxException {
		if (!this.includesNonPlayers) {
			return this.getPlayers(source);
		}
		if (this.playerName != null) {
			AbstractClientPlayerEntity abstractClientPlayerEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity instanceof AbstractClientPlayerEntity)
					.map(entity -> (AbstractClientPlayerEntity) entity)
					.filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
					.findAny().orElse(null);
			return abstractClientPlayerEntity == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayerEntity);
		}
		if (this.uuid != null) {
			Entity foundEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity.getUuid().equals(this.uuid))
					.findAny().orElse(null);
			return foundEntity == null ? Collections.emptyList() : Lists.newArrayList(foundEntity);
		}
		Vec3d pos = this.positionOffset.apply(source.getPosition());
		Predicate<Entity> predicate = this.getPositionPredicate(pos);
		if (this.senderOnly) {
			if (source.getEntity() != null && predicate.test(source.getEntity())) {
				return Lists.newArrayList(source.getEntity());
			}
			return Collections.emptyList();
		}
		ArrayList<Entity> entity = new ArrayList<>();
		this.appendEntitiesFromWorld(entity, source.getWorld(), pos, predicate);
		return this.getEntities(pos, entity);
	}

	private void appendEntitiesFromWorld(List<Entity> result, ClientWorld clientWorld, Vec3d pos, Predicate<Entity> predicate) {
		if (this.box != null) {
			result.addAll(clientWorld.getEntitiesByType(this.entityFilter, this.box.offset(pos), predicate));
		} else {
			clientWorld.getEntities().forEach(entity -> {
				if (predicate.test(entity)) {
					result.add(entity);
				}
				if (entity instanceof EnderDragonEntity enderDragon) {
					for (EnderDragonPart bodyPart : enderDragon.getBodyParts()) {
						Entity e = entityFilter.downcast(bodyPart);
						if (e == null || !predicate.test(e)) {
							continue;
						}
						result.add(e);
					}
				}
			});
		}
	}

	public AbstractClientPlayerEntity getPlayer(FabricClientCommandSource source) throws CommandSyntaxException {
		List<AbstractClientPlayerEntity> list = this.getPlayers(source);
		if (list.size() != 1) {
			throw CEntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
		}
		return list.get(0);
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
		}
		if (this.uuid != null) {
			abstractClientPlayerEntity = Streams.stream(source.getWorld().getEntities())
					.filter(entity -> entity instanceof AbstractClientPlayerEntity)
					.map(entity -> (AbstractClientPlayerEntity) entity)
					.filter(entity -> entity.getUuid().equals(this.uuid))
					.findAny().orElse(null);
			return abstractClientPlayerEntity == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayerEntity);
		}
		Vec3d pos = this.positionOffset.apply(source.getPosition());
		Predicate<Entity> predicate = this.getPositionPredicate(pos);
		if (this.senderOnly) {
			if (source.getEntity() instanceof AbstractClientPlayerEntity player && predicate.test(player)) {
				return Lists.newArrayList(player);
			}
			return Collections.emptyList();
		}
		List<AbstractClientPlayerEntity> entities = source.getWorld().getPlayers().stream()
				.filter(predicate)
				.collect(Collectors.toList());
		return this.getEntities(pos, entities);
	}

	private Predicate<Entity> getPositionPredicate(Vec3d pos) {
		Predicate<Entity> predicate = this.basePredicate;
		if (this.box != null) {
			Box box = this.box.offset(pos);
			predicate = predicate.and(entity -> box.intersects(entity.getBoundingBox()));
		}
		if (!this.distance.isDummy()) {
			predicate = predicate.and(entity -> this.distance.testSqrt(entity.squaredDistanceTo(pos)));
		}
		return predicate;
	}

	private <T extends Entity> List<T> getEntities(Vec3d pos, List<T> entities) {
		if (entities.size() > 1) {
			this.sorter.accept(pos, entities);
		}
		return entities.subList(0, Math.min(this.limit, entities.size()));
	}

	public static Text getNames(List<? extends Entity> entities) {
		return Texts.join(entities, Entity::getDisplayName);
	}
}
