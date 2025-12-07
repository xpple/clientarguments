package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType INVALID_FEATURE_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.place.feature.invalid", id));
    private static final DynamicCommandExceptionType INVALID_STRUCTURE_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.place.structure.invalid", id));
    private static final DynamicCommandExceptionType INVALID_JIGSAW_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.place.jigsaw.invalid", id));
    private static final DynamicCommandExceptionType ERROR_INVALID_RECIPE = new DynamicCommandExceptionType(object -> Component.translatableEscape("recipe.notFound", object));
    private static final DynamicCommandExceptionType ERROR_INVALID_ADVANCEMENT = new DynamicCommandExceptionType(object -> Component.translatableEscape("advancement.advancementNotFound", object));
    final ResourceKey<? extends Registry<T>> registryRef;

    public CResourceKeyArgument(ResourceKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static <T> CResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> registryRef) {
        return new CResourceKeyArgument<>(registryRef);
    }

    public static <T> ResourceKey<T> getKey(final CommandContext<FabricClientCommandSource> context, final String name, final ResourceKey<Registry<T>> registryRef, final DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        ResourceKey<?> registryKey = context.getArgument(name, ResourceKey.class);
        Optional<ResourceKey<T>> optional = registryKey.cast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(registryKey.identifier()));
    }

    public static <T> Registry<T> getRegistry(final CommandContext<FabricClientCommandSource> context, ResourceKey<? extends Registry<T>> registryRef) {
        return context.getSource().registryAccess().lookupOrThrow(registryRef);
    }

    public static <T> Holder.Reference<T> getRegistryEntry(final CommandContext<FabricClientCommandSource> context, final String name, ResourceKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        ResourceKey<T> registryKey = getKey(context, name, registryRef, invalidException);
        return getRegistry(context, registryRef)
            .get(registryKey)
            .orElseThrow(() -> invalidException.create(registryKey.identifier()));
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.CONFIGURED_FEATURE, INVALID_FEATURE_EXCEPTION);
    }

    public static Holder.Reference<Structure> getStructure(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.STRUCTURE, INVALID_STRUCTURE_EXCEPTION);
    }

    public static Holder.Reference<StructureTemplatePool> getStructurePool(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.TEMPLATE_POOL, INVALID_JIGSAW_EXCEPTION);
    }

    public static AdvancementHolder getAdvancement(final CommandContext<FabricClientCommandSource> context, final String string) throws CommandSyntaxException {
        ResourceKey<Advancement> resourceKey = getKey(context, string, Registries.ADVANCEMENT, ERROR_INVALID_ADVANCEMENT);
        AdvancementHolder advancementHolder = context.getSource().getPlayer().connection.getAdvancements().get(resourceKey.identifier());
        if (advancementHolder == null) {
            throw ERROR_INVALID_ADVANCEMENT.create(resourceKey.identifier());
        }
        return advancementHolder;
    }

    @Override
    public ResourceKey<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.read(stringReader);
        return ResourceKey.create(this.registryRef, identifier);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider commandSource
            ? commandSource.suggestRegistryElements(this.registryRef, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, builder, context)
            : builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
