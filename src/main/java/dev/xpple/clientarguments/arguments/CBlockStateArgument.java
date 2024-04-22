package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public class CBlockStateArgument implements ArgumentType<CBlockInput> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");
    private final HolderLookup<Block> registryWrapper;

    public CBlockStateArgument(CommandBuildContext buildContext) {
        this.registryWrapper = buildContext.lookupOrThrow(Registries.BLOCK);
    }

    public static CBlockStateArgument blockState(CommandBuildContext buildContext) {
        return new CBlockStateArgument(buildContext);
    }

    @Override
    public CBlockInput parse(final StringReader stringReader) throws CommandSyntaxException {
        BlockStateParser.BlockResult blockResult = BlockStateParser.parseForBlock(this.registryWrapper, stringReader, true);
        return new CBlockInput(blockResult.blockState(), blockResult.properties().keySet(), blockResult.nbt());
    }

    public static CBlockInput getBlockState(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, CBlockInput.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return BlockStateParser.fillSuggestions(this.registryWrapper, builder, false, true);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}