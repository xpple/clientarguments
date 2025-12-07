package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CResourceArgument<T> implements ArgumentType<Holder.Reference<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType NOT_SUMMONABLE_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("entity.not_summonable", id));
    public static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((element, type) -> Component.translatableEscape("argument.resource.not_found", element, type));
    public static final Dynamic3CommandExceptionType INVALID_TYPE_EXCEPTION = new Dynamic3CommandExceptionType((element, type, expectedType) -> Component.translatableEscape("argument.resource.invalid_type", element, type, expectedType));
    final ResourceKey<? extends Registry<T>> registryRef;
    private final HolderLookup<T> holderLookup;

    public CResourceArgument(CommandBuildContext buildContext, ResourceKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
        this.holderLookup = buildContext.lookupOrThrow(registryRef);
    }

    public static <T> CResourceArgument<T> registryEntry(CommandBuildContext buildContext, ResourceKey<? extends Registry<T>> registryRef) {
        return new CResourceArgument<>(buildContext, registryRef);
    }

    @SuppressWarnings("unchecked")
    public static <T> Holder.Reference<T> getRegistryEntry(final CommandContext<FabricClientCommandSource> context, final String name, ResourceKey<Registry<T>> registryRef) throws CommandSyntaxException {
        Holder.Reference<T> reference = (Holder.Reference<T>) context.getArgument(name, Holder.Reference.class);
        ResourceKey<?> resourceKey = reference.key();
        if (!resourceKey.isFor(registryRef)) {
            throw INVALID_TYPE_EXCEPTION.create(resourceKey.identifier(), resourceKey.registry(), registryRef.identifier());
        }
        return reference;
    }

    public static Holder.Reference<Attribute> getEntityAttribute(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.ATTRIBUTE);
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.CONFIGURED_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.STRUCTURE);
    }

    public static Holder.Reference<EntityType<?>> getEntityType(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.ENTITY_TYPE);
    }

    public static Holder.Reference<EntityType<?>> getSummonableEntityType(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        Holder.Reference<EntityType<?>> reference = getRegistryEntry(context, name, Registries.ENTITY_TYPE);
        if (!reference.value().canSummon()) {
            throw NOT_SUMMONABLE_EXCEPTION.create(reference.key().identifier().toString());
        }
        return reference;
    }

    public static Holder.Reference<MobEffect> getStatusEffect(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.MOB_EFFECT);
    }

    public static Holder.Reference<Enchantment> getEnchantment(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.ENCHANTMENT);
    }

    public Holder.Reference<T> parse(StringReader stringReader) throws CommandSyntaxException {
        Identifier id = Identifier.read(stringReader);
        ResourceKey<T> resourceKey = ResourceKey.create(this.registryRef, id);
        return this.holderLookup
            .get(resourceKey)
            .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(stringReader, id, this.registryRef.identifier()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(this.holderLookup.listElementIds().map(ResourceKey::identifier), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
