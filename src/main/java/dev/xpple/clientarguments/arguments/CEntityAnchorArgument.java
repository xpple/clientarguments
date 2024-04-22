package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class CEntityAnchorArgument implements ArgumentType<CEntityAnchorArgument.EntityAnchor> {
	private static final Collection<String> EXAMPLES = Arrays.asList("eyes", "feet");
	private static final DynamicCommandExceptionType INVALID_ANCHOR_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("argument.anchor.invalid", name));

	public static CEntityAnchorArgument entityAnchor() {
		return new CEntityAnchorArgument();
	}

	public static EntityAnchor getEntityAnchor(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, EntityAnchor.class);
	}

	@Override
	public EntityAnchor parse(final StringReader stringReader) throws CommandSyntaxException {
		int cursor = stringReader.getCursor();
		String string = stringReader.readUnquotedString();
		EntityAnchor entityAnchor = EntityAnchor.fromId(string);
		if (entityAnchor == null) {
			stringReader.setCursor(cursor);
			throw INVALID_ANCHOR_EXCEPTION.createWithContext(stringReader, string);
		}
		return entityAnchor;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(EntityAnchor.ANCHORS.keySet(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public enum EntityAnchor {
		FEET("feet", (pos, entity) -> pos),
		EYES("eyes", (pos, entity) -> new Vec3(pos.x, pos.y + (double) entity.getEyeHeight(), pos.z));

		static final Map<String, EntityAnchor> ANCHORS = Util.make(Maps.newHashMap(), map -> {
			for (EntityAnchor entityAnchor : values()) {
				map.put(entityAnchor.id, entityAnchor);
			}
		});
		private final String id;
		private final BiFunction<Vec3, Entity, Vec3> offset;

		EntityAnchor(final String id, final BiFunction<Vec3, Entity, Vec3> offset) {
			this.id = id;
			this.offset = offset;
		}

		@Nullable
		public static CEntityAnchorArgument.EntityAnchor fromId(String id) {
			return ANCHORS.get(id);
		}

		public Vec3 positionAt(Entity entity) {
			return this.offset.apply(entity.position(), entity);
		}

		public Vec3 positionAt(FabricClientCommandSource source) {
			Entity entity = source.getEntity();
			return entity == null ? source.getPosition() : this.offset.apply(source.getPosition(), entity);
		}
	}
}
