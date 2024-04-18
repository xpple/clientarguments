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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CRegistryEntryReferenceArgumentType<T> implements ArgumentType<RegistryEntry.Reference<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType NOT_SUMMONABLE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("entity.not_summonable", id));
    public static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((element, type) -> Text.stringifiedTranslatable("argument.resource.not_found", element, type));
    public static final Dynamic3CommandExceptionType INVALID_TYPE_EXCEPTION = new Dynamic3CommandExceptionType((element, type, expectedType) -> Text.stringifiedTranslatable("argument.resource.invalid_type", element, type, expectedType));
    final RegistryKey<? extends Registry<T>> registryRef;
    private final RegistryWrapper<T> registryWrapper;

    public CRegistryEntryReferenceArgumentType(CommandRegistryAccess registryAccess, RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
        this.registryWrapper = registryAccess.getWrapperOrThrow(registryRef);
    }

    public static <T> CRegistryEntryReferenceArgumentType<T> registryEntry(CommandRegistryAccess registryAccess, RegistryKey<? extends Registry<T>> registryRef) {
        return new CRegistryEntryReferenceArgumentType<>(registryAccess, registryRef);
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryEntry.Reference<T> getRegistryEntry(final CommandContext<FabricClientCommandSource> context, final String name, RegistryKey<Registry<T>> registryRef) throws CommandSyntaxException {
        RegistryEntry.Reference<T> reference = (RegistryEntry.Reference<T>) context.getArgument(name, RegistryEntry.Reference.class);
        RegistryKey<?> registryKey = reference.registryKey();
        if (!registryKey.isOf(registryRef)) {
            throw INVALID_TYPE_EXCEPTION.create(registryKey.getValue(), registryKey.getRegistry(), registryRef.getValue());
        }
        return reference;
    }

    public static RegistryEntry.Reference<EntityAttribute> getEntityAttribute(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ATTRIBUTE);
    }

    public static RegistryEntry.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.CONFIGURED_FEATURE);
    }

    public static RegistryEntry.Reference<Structure> getStructure(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.STRUCTURE);
    }

    public static RegistryEntry.Reference<EntityType<?>> getEntityType(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ENTITY_TYPE);
    }

    public static RegistryEntry.Reference<EntityType<?>> getSummonableEntityType(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        RegistryEntry.Reference<EntityType<?>> reference = getRegistryEntry(context, name, RegistryKeys.ENTITY_TYPE);
        if (!reference.value().isSummonable()) {
            throw NOT_SUMMONABLE_EXCEPTION.create(reference.registryKey().getValue().toString());
        }
        return reference;
    }

    public static RegistryEntry.Reference<StatusEffect> getStatusEffect(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.STATUS_EFFECT);
    }

    public static RegistryEntry.Reference<Enchantment> getEnchantment(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ENCHANTMENT);
    }

    public RegistryEntry.Reference<T> parse(StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        RegistryKey<T> registryKey = RegistryKey.of(this.registryRef, identifier);
        return this.registryWrapper
            .getOptional(registryKey)
            .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier, this.registryRef.getValue()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(this.registryWrapper.streamKeys().map(RegistryKey::getValue), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
