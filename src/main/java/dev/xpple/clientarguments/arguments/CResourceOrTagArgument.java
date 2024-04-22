package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CResourceOrTagArgument<T> implements ArgumentType<CResourceOrTagArgument.Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((tag, type) -> Component.translatableEscape("argument.resource_tag.not_found", tag, type));
    private static final Dynamic3CommandExceptionType WRONG_TYPE_EXCEPTION = new Dynamic3CommandExceptionType((tag, type, expectedType) -> Component.translatableEscape("argument.resource_tag.invalid_type", tag, type, expectedType));
    private final HolderLookup<T> holderLookup;
    final ResourceKey<? extends Registry<T>> registryRef;

    public CResourceOrTagArgument(CommandBuildContext buildContext, ResourceKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
        this.holderLookup = buildContext.lookupOrThrow(registryRef);
    }

    public static <T> CResourceOrTagArgument<T> resourceOrTag(CommandBuildContext buildContext, ResourceKey<? extends Registry<T>> registryRef) {
        return new CResourceOrTagArgument<>(buildContext, registryRef);
    }

    public static <T> Result<T> getResourceOrTag(final CommandContext<FabricClientCommandSource> context, final String name, final ResourceKey<Registry<T>> registryRef) throws CommandSyntaxException {
        Result<?> result = context.getArgument(name, Result.class);
        Optional<Result<T>> optional = result.tryCast(registryRef);
        return optional.orElseThrow(() -> result.getEntry().map(entry -> {
            ResourceKey<?> resourceKey2 = entry.key();
            return ResourceArgument.ERROR_INVALID_RESOURCE_TYPE.create(resourceKey2.location(), resourceKey2.registry(), registryRef.location());
        }, entryList -> {
            TagKey<?> tagKey = entryList.key();
            return WRONG_TYPE_EXCEPTION.create(tagKey.location(), tagKey.registry(), registryRef.location());
        }));
    }

    @Override
    public Result<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int i = stringReader.getCursor();

            try {
                stringReader.skip();
                ResourceLocation id = ResourceLocation.read(stringReader);
                TagKey<T> tagKey = TagKey.create(this.registryRef, id);
                HolderSet.Named<T> named = this.holderLookup
                    .get(tagKey)
                    .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(stringReader, id, this.registryRef.location()));
                return new TagBased<>(named);
            } catch (CommandSyntaxException var6) {
                stringReader.setCursor(i);
                throw var6;
            }
        } else {
            ResourceLocation id = ResourceLocation.read(stringReader);
            ResourceKey<T> resourceKey = ResourceKey.create(this.registryRef, id);
            Holder.Reference<T> reference = this.holderLookup
                .get(resourceKey)
                .orElseThrow(() -> ResourceArgument.ERROR_UNKNOWN_RESOURCE.createWithContext(stringReader, id, this.registryRef.location()));
            return new EntryBased<>(reference);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        SharedSuggestionProvider.suggestResource(this.holderLookup.listTagIds().map(TagKey::location), builder, "#");
        return SharedSuggestionProvider.suggestResource(this.holderLookup.listElementIds().map(ResourceKey::location), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    record EntryBased<T>(Holder.Reference<T> value) implements Result<T> {
        @Override
        public Either<Holder.Reference<T>, HolderSet.Named<T>> getEntry() {
            return Either.left(this.value);
        }

        @Override
        public <E> Optional<Result<E>> tryCast(ResourceKey<? extends Registry<E>> registryRef) {
            return this.value.key().isFor(registryRef) ? Optional.of((Result<E>) this) : Optional.empty();
        }

        public boolean test(Holder<T> registryEntry) {
            return registryEntry.equals(this.value);
        }

        @Override
        public String asString() {
            return this.value.key().location().toString();
        }
    }

    public interface Result<T> extends Predicate<Holder<T>> {
        Either<Holder.Reference<T>, HolderSet.Named<T>> getEntry();

        <E> Optional<Result<E>> tryCast(ResourceKey<? extends Registry<E>> registryRef);

        String asString();
    }

    record TagBased<T>(HolderSet.Named<T> tag) implements Result<T> {
        @Override
        public Either<Holder.Reference<T>, HolderSet.Named<T>> getEntry() {
            return Either.right(this.tag);
        }

        @Override
        public <E> Optional<Result<E>> tryCast(ResourceKey<? extends Registry<E>> registryRef) {
            return this.tag.key().isFor(registryRef) ? Optional.of((Result<E>) this) : Optional.empty();
        }

        public boolean test(Holder<T> registryEntry) {
            return this.tag.contains(registryEntry);
        }

        @Override
        public String asString() {
            return "#" + this.tag.key().location();
        }
    }
}
