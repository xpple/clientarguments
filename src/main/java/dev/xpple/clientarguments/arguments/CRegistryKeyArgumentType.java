package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CRegistryKeyArgumentType<T> implements ArgumentType<RegistryKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType INVALID_FEATURE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.place.feature.invalid", id));
    private static final DynamicCommandExceptionType INVALID_STRUCTURE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.place.structure.invalid", id));
    private static final DynamicCommandExceptionType INVALID_JIGSAW_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.place.jigsaw.invalid", id));
    final RegistryKey<? extends Registry<T>> registryRef;

    public CRegistryKeyArgumentType(RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static <T> CRegistryKeyArgumentType<T> registryKey(RegistryKey<? extends Registry<T>> registryRef) {
        return new CRegistryKeyArgumentType<>(registryRef);
    }

    public static <T> RegistryKey<T> getKey(final CommandContext<FabricClientCommandSource> context, final String name, final RegistryKey<Registry<T>> registryRef, final DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        RegistryKey<?> registryKey = context.getArgument(name, RegistryKey.class);
        Optional<RegistryKey<T>> optional = registryKey.tryCast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(registryKey));
    }

    public static <T> Registry<T> getRegistry(final CommandContext<FabricClientCommandSource> context, RegistryKey<? extends Registry<T>> registryRef) {
        return context.getSource().getRegistryManager().get(registryRef);
    }

    public static <T> RegistryEntry.Reference<T> getRegistryEntry(final CommandContext<FabricClientCommandSource> context, final String name, RegistryKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        RegistryKey<T> registryKey = getKey(context, name, registryRef, invalidException);
        return getRegistry(context, registryRef)
            .getEntry(registryKey)
            .orElseThrow(() -> invalidException.create(registryKey.getValue()));
    }

    public static RegistryEntry.Reference<ConfiguredFeature<?, ?>> getConfiguredFeatureEntry(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.CONFIGURED_FEATURE, INVALID_FEATURE_EXCEPTION);
    }

    public static RegistryEntry.Reference<Structure> getStructureEntry(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.STRUCTURE, INVALID_STRUCTURE_EXCEPTION);
    }

    public static RegistryEntry.Reference<StructurePool> getStructurePoolEntry(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.TEMPLATE_POOL, INVALID_JIGSAW_EXCEPTION);
    }

    @Override
    public RegistryKey<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        return RegistryKey.of(this.registryRef, identifier);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return context.getSource() instanceof CommandSource commandSource
            ? commandSource.listIdSuggestions(this.registryRef, CommandSource.SuggestedIdType.ELEMENTS, builder, context)
            : builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
