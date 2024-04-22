package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CResourceOrTagKeyArgument<T> implements ArgumentType<CResourceOrTagKeyArgument.Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    final ResourceKey<? extends Registry<T>> registryRef;

    public CResourceOrTagKeyArgument(ResourceKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static <T> CResourceOrTagKeyArgument<T> registryPredicate(ResourceKey<? extends Registry<T>> registryRef) {
        return new CResourceOrTagKeyArgument<>(registryRef);
    }

    public static <T> Result<T> getPredicate(final CommandContext<FabricClientCommandSource> context, final String name, final ResourceKey<Registry<T>> registryRef, final DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        Result<?> result = context.getArgument(name, Result.class);
        Optional<Result<T>> optional = result.tryCast(registryRef);
        return optional.orElseThrow(() -> invalidException.create(result));
    }

    @Override
    public Result<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int i = stringReader.getCursor();

            try {
                stringReader.skip();
                ResourceLocation id = ResourceLocation.read(stringReader);
                return new TagResult<>(TagKey.create(this.registryRef, id));
            } catch (CommandSyntaxException var4) {
                stringReader.setCursor(i);
                throw var4;
            }
        } else {
            ResourceLocation id = ResourceLocation.read(stringReader);
            return new ResourceResult<>(ResourceKey.create(this.registryRef, id));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider commandSource
            ? commandSource.suggestRegistryElements(this.registryRef, SharedSuggestionProvider.ElementSuggestionType.ALL, builder, context)
            : builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    record ResourceResult<T>(ResourceKey<T> key) implements Result<T> {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> getKey() {
            return Either.left(this.key);
        }

        @Override
        public <E> Optional<Result<E>> tryCast(ResourceKey<? extends Registry<E>> registryRef) {
            return this.key.cast(registryRef).map(ResourceResult::new);
        }

        public boolean test(Holder<T> registryEntry) {
            return registryEntry.is(this.key);
        }

        @Override
        public String asString() {
            return this.key.location().toString();
        }
    }

    public interface Result<T> extends Predicate<Holder<T>> {
        Either<ResourceKey<T>, TagKey<T>> getKey();

        <E> Optional<Result<E>> tryCast(ResourceKey<? extends Registry<E>> registryRef);

        String asString();
    }

    record TagResult<T>(TagKey<T> key) implements Result<T> {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> getKey() {
            return Either.right(this.key);
        }

        @Override
        public <E> Optional<Result<E>> tryCast(ResourceKey<? extends Registry<E>> registryRef) {
            return this.key.cast(registryRef).map(TagResult::new);
        }

        public boolean test(Holder<T> registryEntry) {
            return registryEntry.is(this.key);
        }

        @Override
        public String asString() {
            return "#" + this.key.location();
        }
    }
}
