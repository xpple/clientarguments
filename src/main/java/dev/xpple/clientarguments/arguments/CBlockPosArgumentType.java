package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CBlockPosArgumentType implements ArgumentType<CPosArgument> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
	public static final SimpleCommandExceptionType UNLOADED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.pos.unloaded"));
	public static final SimpleCommandExceptionType OUT_OF_WORLD_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.pos.outofworld"));
	public static final SimpleCommandExceptionType OUT_OF_BOUNDS_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.pos.outofbounds"));

	public static CBlockPosArgumentType blockPos() {
		return new CBlockPosArgumentType();
	}

	public static BlockPos getCLoadedBlockPos(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		BlockPos blockPos = (context.getArgument(name, CPosArgument.class)).toAbsoluteBlockPos(context.getSource());

		if (!context.getSource().getWorld().isChunkLoaded(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
			throw UNLOADED_EXCEPTION.create();
		} else if (!context.getSource().getWorld().isInBuildLimit(blockPos)) {
			throw OUT_OF_WORLD_EXCEPTION.create();
		}
		return blockPos;
	}

	public static BlockPos getCBlockPos(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		BlockPos blockPos = context.getArgument(name, CPosArgument.class).toAbsoluteBlockPos(context.getSource());
		if (World.isValid(blockPos)) {
			return blockPos;
		}
		throw OUT_OF_BOUNDS_EXCEPTION.create();
	}

	@Override
	public CPosArgument parse(final StringReader stringReader) throws CommandSyntaxException {
		return stringReader.canRead() && stringReader.peek() == '^' ? CLookingPosArgument.parse(stringReader) : CDefaultPosArgument.parse(stringReader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		String remaining = builder.getRemaining();
		if (!remaining.isEmpty() && remaining.charAt(0) == '^') {
			final Set<CommandSource.RelativePosition> singleton = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
			return CommandSource.suggestPositions(remaining, singleton, builder, CommandManager.getCommandValidator(this::parse));
		} else {
			final Collection<CommandSource.RelativePosition> blockPositionSuggestions = ((FabricClientCommandSource) context.getSource()).getBlockPositionSuggestions();
			return CommandSource.suggestPositions(remaining, blockPositionSuggestions, builder, CommandManager.getCommandValidator(this::parse));
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
