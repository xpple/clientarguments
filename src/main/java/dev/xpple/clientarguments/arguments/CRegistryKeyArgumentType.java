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
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CRegistryKeyArgumentType<T> implements ArgumentType<RegistryKey<T>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType UNKNOWN_ATTRIBUTE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("cattribute.unknown", id));
    private static final DynamicCommandExceptionType INVALID_CONFIGURED_FEATURE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("ccommands.placefeature.invalid", id));
    final RegistryKey<? extends Registry<T>> registryRef;

    public CRegistryKeyArgumentType(RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static <T> CRegistryKeyArgumentType<T> registryKey(RegistryKey<? extends Registry<T>> registryRef) {
        return new CRegistryKeyArgumentType<T>(registryRef);
    }

    private static <T> RegistryKey<T> getKey(CommandContext<FabricClientCommandSource> context, String name, RegistryKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        RegistryKey<?> registryKey = context.getArgument(name, RegistryKey.class);
        Optional<RegistryKey<T>> optional = registryKey.tryCast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(registryKey));
    }

    private static <T> Registry<T> getRegistry(CommandContext<FabricClientCommandSource> context, RegistryKey<? extends Registry<T>> registryRef) {
        return context.getSource().getRegistryManager().get(registryRef);
    }

    public static EntityAttribute getCAttribute(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        RegistryKey<EntityAttribute> registryKey = CRegistryKeyArgumentType.getKey(context, name, RegistryKeys.ATTRIBUTE, UNKNOWN_ATTRIBUTE_EXCEPTION);
        return CRegistryKeyArgumentType.getRegistry(context, RegistryKeys.ATTRIBUTE).getOrEmpty(registryKey).orElseThrow(() -> UNKNOWN_ATTRIBUTE_EXCEPTION.create(registryKey.getValue()));
    }

    public static RegistryEntry<ConfiguredFeature<?, ?>> getCConfiguredFeatureEntry(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        RegistryKey<ConfiguredFeature<?, ?>> registryKey = CRegistryKeyArgumentType.getKey(context, name, RegistryKeys.CONFIGURED_FEATURE, INVALID_CONFIGURED_FEATURE_EXCEPTION);
        return CRegistryKeyArgumentType.getRegistry(context, RegistryKeys.CONFIGURED_FEATURE).getEntry(registryKey).orElseThrow(() -> INVALID_CONFIGURED_FEATURE_EXCEPTION.create(registryKey.getValue()));
    }

    @Override
    public RegistryKey<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        return RegistryKey.of(this.registryRef, identifier);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        S s = context.getSource();
        if (s instanceof CommandSource commandSource) {
            return commandSource.listIdSuggestions(this.registryRef, CommandSource.SuggestedIdType.ELEMENTS, builder, context);
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
