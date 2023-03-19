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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CRegistryEntryArgumentType<T> implements ArgumentType<RegistryEntry.Reference<T>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

    private static final DynamicCommandExceptionType NOT_SUMMONABLE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("entity.not_summonable", id));
    public static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((element, type) -> Text.translatable("argument.resource.not_found", element, type));
    public static final Dynamic3CommandExceptionType INVALID_TYPE_EXCEPTION = new Dynamic3CommandExceptionType((element, type, expectedType) -> Text.translatable("argument.resource.invalid_type", element, type, expectedType));

    final RegistryKey<? extends Registry<T>> registryRef;
    private final RegistryWrapper<T> registryWrapper;

    public CRegistryEntryArgumentType(CommandRegistryAccess arg, RegistryKey<? extends Registry<T>> arg2) {
        this.registryRef = arg2;
        this.registryWrapper = arg.createWrapper(arg2);
    }

    public static <T> CRegistryEntryArgumentType<T> registryEntry(CommandRegistryAccess registryAccess, RegistryKey<? extends Registry<T>> registryRef) {
        return new CRegistryEntryArgumentType<>(registryAccess, registryRef);
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryEntry.Reference<T> getCRegistryEntry(CommandContext<FabricClientCommandSource> context, String name, RegistryKey<Registry<T>> registryRef) throws CommandSyntaxException {
        RegistryEntry.Reference<T> reference = (RegistryEntry.Reference<T>) context.getArgument(name, RegistryEntry.Reference.class);
        RegistryKey<?> registryKey = reference.registryKey();
        if (registryKey.isOf(registryRef)) {
            return reference;
        }
        throw INVALID_TYPE_EXCEPTION.create(registryKey.getValue(), registryKey.getRegistry(), registryRef.getValue());
    }

    public static RegistryEntry.Reference<EntityAttribute> getCEntityAttribute(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return getCRegistryEntry(context, name, RegistryKeys.ATTRIBUTE);
    }

    public static RegistryEntry.Reference<ConfiguredFeature<?, ?>> getCConfiguredFeature(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return getCRegistryEntry(context, name, RegistryKeys.CONFIGURED_FEATURE);
    }

    public static RegistryEntry.Reference<Structure> getCStructure(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return getCRegistryEntry(context, name, RegistryKeys.STRUCTURE);
    }

    public static RegistryEntry.Reference<EntityType<?>> getCEntityType(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return getCRegistryEntry(context, name, RegistryKeys.ENTITY_TYPE);
    }

    public static RegistryEntry.Reference<EntityType<?>> getCSummonableEntityType(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        RegistryEntry.Reference<EntityType<?>> lv = getCRegistryEntry(context, name, RegistryKeys.ENTITY_TYPE);
        if (lv.value().isSummonable()) {
            return lv;
        }
        throw NOT_SUMMONABLE_EXCEPTION.create(lv.registryKey().getValue().toString());
    }

    public static RegistryEntry.Reference<StatusEffect> getCStatusEffect(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return getCRegistryEntry(context, name, RegistryKeys.STATUS_EFFECT);
    }

    public static RegistryEntry.Reference<Enchantment> getCEnchantment(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return getCRegistryEntry(context, name, RegistryKeys.ENCHANTMENT);
    }

    @Override
    public RegistryEntry.Reference<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        RegistryKey<T> registryKey = RegistryKey.of(this.registryRef, identifier);
        return this.registryWrapper.getOptional(registryKey).orElseThrow(() -> NOT_FOUND_EXCEPTION.create(identifier, this.registryRef.getValue()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> commandContext, final SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestIdentifiers(this.registryWrapper.streamKeys().map(RegistryKey::getValue), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
