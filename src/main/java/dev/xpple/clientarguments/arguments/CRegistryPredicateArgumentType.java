package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CRegistryPredicateArgumentType<T> implements ArgumentType<CRegistryPredicateArgumentType.RegistryPredicate<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    final RegistryKey<? extends Registry<T>> registryRef;

    public CRegistryPredicateArgumentType(RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static <T> RegistryPredicateArgumentType<T> registryPredicate(RegistryKey<? extends Registry<T>> registryRef) {
        return new RegistryPredicateArgumentType<>(registryRef);
    }

    public static <T> RegistryPredicate<T> getCPredicate(
            CommandContext<FabricClientCommandSource> context,
            String name,
            RegistryKey<Registry<T>> registryRef,
            DynamicCommandExceptionType invalidException
    ) throws CommandSyntaxException {
        RegistryPredicate<?> registryPredicate = context.getArgument(name, RegistryPredicate.class);
        Optional<RegistryPredicate<T>> optional = registryPredicate.tryCast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(registryPredicate));
    }

    public RegistryPredicate<T> parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int i = stringReader.getCursor();

            try {
                stringReader.skip();
                Identifier identifier = Identifier.fromCommandInput(stringReader);
                return new TagBased<>(TagKey.of(this.registryRef, identifier));
            } catch (CommandSyntaxException var4) {
                stringReader.setCursor(i);
                throw var4;
            }
        } else {
            Identifier identifier2 = Identifier.fromCommandInput(stringReader);
            return new RegistryKeyBased<>(RegistryKey.of(this.registryRef, identifier2));
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Object var4 = context.getSource();
        return var4 instanceof CommandSource commandSource
                ? commandSource.listIdSuggestions(this.registryRef, CommandSource.SuggestedIdType.ALL, builder, context)
                : builder.buildFuture();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
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

        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.matchesKey(this.key);
        }

        @Override
        public String asString() {
            return this.key.getValue().toString();
        }
    }

    public interface RegistryPredicate<T> extends Predicate<RegistryEntry<T>> {
        Either<RegistryKey<T>, TagKey<T>> getKey();

        <E> Optional<RegistryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef);

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

        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.isIn(this.key);
        }

        @Override
        public String asString() {
            return "#" + this.key.id();
        }
    }
}
