package dev.xpple.clientarguments.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.tag.TagKey;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CRegistryPredicateArgumentType<T> implements ArgumentType<CRegistryPredicateArgumentType.RegistryPredicate<T>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private static final DynamicCommandExceptionType INVALID_BIOME_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("ccommands.locatebiome.invalid", id));
    private static final DynamicCommandExceptionType INVALID_CONFIGURED_STRUCTURE_FEATURE_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("ccommands.locate.invalid", id));
    final RegistryKey<? extends Registry<T>> registryRef;

    public CRegistryPredicateArgumentType(RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static <T> CRegistryPredicateArgumentType<T> registryPredicate(RegistryKey<? extends Registry<T>> registryRef) {
        return new CRegistryPredicateArgumentType<T>(registryRef);
    }

    private static <T> RegistryPredicate<T> getPredicate(CommandContext<FabricClientCommandSource> context, String name, RegistryKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        RegistryPredicate<?> registryPredicate = context.getArgument(name, RegistryPredicate.class);
        Optional<RegistryPredicate<T>> optional = registryPredicate.tryCast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(registryPredicate));
    }

    public static RegistryPredicate<Biome> getCBiomePredicate(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return CRegistryPredicateArgumentType.getPredicate(context, name, Registry.BIOME_KEY, INVALID_BIOME_EXCEPTION);
    }

    public static RegistryPredicate<ConfiguredStructureFeature<?, ?>> getCConfiguredStructureFeaturePredicate(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return CRegistryPredicateArgumentType.getPredicate(context, name, Registry.CONFIGURED_STRUCTURE_FEATURE_KEY, INVALID_CONFIGURED_STRUCTURE_FEATURE_EXCEPTION);
    }

    @Override
    public RegistryPredicate<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int cursor = stringReader.getCursor();
            try {
                stringReader.skip();
                Identifier identifier = Identifier.fromCommandInput(stringReader);
                return new TagBased<>(TagKey.of(this.registryRef, identifier));
            } catch (CommandSyntaxException e) {
                stringReader.setCursor(cursor);
                throw e;
            }
        }
        Identifier i = Identifier.fromCommandInput(stringReader);
        return new RegistryKeyBased<>(RegistryKey.of(this.registryRef, i));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        S s = context.getSource();
        if (s instanceof CommandSource commandSource) {
            return commandSource.listIdSuggestions(this.registryRef, CommandSource.SuggestedIdType.ALL, builder, context);
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public interface RegistryPredicate<T> extends Predicate<RegistryEntry<T>> {
        Either<RegistryKey<T>, TagKey<T>> getKey();

        <E> Optional<RegistryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> var1);

        String asString();
    }

    record TagBased<T>(TagKey<T> key) implements RegistryPredicate<T> {
        @Override
        public Either<RegistryKey<T>, TagKey<T>> getKey() {
            return Either.right(this.key);
        }

        @Override
        public <E> Optional<RegistryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.key.tryCast(registryRef).map(TagBased::new);
        }

        @Override
        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.isIn(this.key);
        }

        @Override
        public String asString() {
            return "#" + this.key.id();
        }
    }

    record RegistryKeyBased<T>(RegistryKey<T> key) implements RegistryPredicate<T> {
        @Override
        public Either<RegistryKey<T>, TagKey<T>> getKey() {
            return Either.left(this.key);
        }

        @Override
        public <E> Optional<RegistryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.key.tryCast(registryRef).map(RegistryKeyBased::new);
        }

        @Override
        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.matchesKey(this.key);
        }

        @Override
        public String asString() {
            return this.key.getValue().toString();
        }
    }

    public static class Serializer implements ArgumentSerializer<CRegistryPredicateArgumentType<?>> {
        @Override
        public void toPacket(CRegistryPredicateArgumentType<?> registryPredicateArgumentType, PacketByteBuf packetByteBuf) {
            packetByteBuf.writeIdentifier(registryPredicateArgumentType.registryRef.getValue());
        }

        @Override
        public CRegistryPredicateArgumentType<?> fromPacket(PacketByteBuf packetByteBuf) {
            Identifier identifier = packetByteBuf.readIdentifier();
            return new CRegistryPredicateArgumentType<>(RegistryKey.ofRegistry(identifier));
        }

        @Override
        public void toJson(CRegistryPredicateArgumentType<?> registryPredicateArgumentType, JsonObject jsonObject) {
            jsonObject.addProperty("registry", registryPredicateArgumentType.registryRef.getValue().toString());
        }
    }
}
