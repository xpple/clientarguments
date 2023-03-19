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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CRegistryEntryPredicateArgumentType<T> implements ArgumentType<CRegistryEntryPredicateArgumentType.EntryPredicate<T>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");

    private static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((tag, type) -> Text.translatable("argument.resource_tag.not_found", tag, type));
    private static final Dynamic3CommandExceptionType WRONG_TYPE_EXCEPTION = new Dynamic3CommandExceptionType((tag, type, expectedType) -> Text.translatable("argument.resource_tag.invalid_type", tag, type, expectedType));

    private final RegistryWrapper<T> registryWrapper;
    final RegistryKey<? extends Registry<T>> registryRef;

    public CRegistryEntryPredicateArgumentType(CommandRegistryAccess registryAccess, RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
        this.registryWrapper = registryAccess.createWrapper(registryRef);
    }

    public static <T> CRegistryEntryPredicateArgumentType<T> registryEntryPredicate(CommandRegistryAccess registryRef, RegistryKey<? extends Registry<T>> registryAccess) {
        return new CRegistryEntryPredicateArgumentType<>(registryRef, registryAccess);
    }

    public static <T> CRegistryEntryPredicateArgumentType.EntryPredicate<T> getRegistryEntryPredicate(final CommandContext<ServerCommandSource> context, final String name, RegistryKey<Registry<T>> registryRef) throws CommandSyntaxException {
        CRegistryEntryPredicateArgumentType.EntryPredicate<?> entryPredicate = context.getArgument(name, CRegistryEntryPredicateArgumentType.EntryPredicate.class);
        Optional<CRegistryEntryPredicateArgumentType.EntryPredicate<T>> optional = entryPredicate.tryCast(registryRef);
        return optional.orElseThrow(() -> entryPredicate.getEntry().map(entry -> {
            RegistryKey<?> registryKey = entry.registryKey();
            return RegistryEntryArgumentType.INVALID_TYPE_EXCEPTION.create(registryKey.getValue(), registryKey.getRegistry(), registryRef.getValue());
        }, entryList -> {
            TagKey<?> tagKey = entryList.getTag();
            return WRONG_TYPE_EXCEPTION.create(tagKey.id(), tagKey.registry(), registryRef.getValue());
        }));
    }

    @Override
    public CRegistryEntryPredicateArgumentType.EntryPredicate<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int cursor = stringReader.getCursor();

            try {
                stringReader.skip();
                Identifier identifier = Identifier.fromCommandInput(stringReader);
                TagKey<T> tagKey = TagKey.of(this.registryRef, identifier);
                RegistryEntryList.Named<T> named = this.registryWrapper.getOptional(tagKey).orElseThrow(() -> NOT_FOUND_EXCEPTION.create(identifier, this.registryRef.getValue()));
                return new CRegistryEntryPredicateArgumentType.TagBased<>(named);
            } catch (CommandSyntaxException e) {
                stringReader.setCursor(cursor);
                throw e;
            }
        } else {
            Identifier identifier = Identifier.fromCommandInput(stringReader);
            RegistryKey<T> registryKey = RegistryKey.of(this.registryRef, identifier);
            RegistryEntry.Reference<T> reference = this.registryWrapper.getOptional(registryKey).orElseThrow(() -> RegistryEntryArgumentType.NOT_FOUND_EXCEPTION.create(identifier, this.registryRef.getValue()));
            return new CRegistryEntryPredicateArgumentType.EntryBased<>(reference);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        CommandSource.suggestIdentifiers(this.registryWrapper.streamTagKeys().map(TagKey::id), builder, "#");
        return CommandSource.suggestIdentifiers(this.registryWrapper.streamKeys().map(RegistryKey::getValue), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public interface EntryPredicate<T> extends Predicate<RegistryEntry<T>> {
        Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry();

        <E> Optional<CRegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef);

        String asString();
    }

    private record TagBased<T>(RegistryEntryList.Named<T> tag) implements CRegistryEntryPredicateArgumentType.EntryPredicate<T> {

        @Override
        public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
            return Either.right(this.tag);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E> Optional<CRegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.tag.getTag().isOf(registryRef) ? Optional.of((EntryPredicate<E>) this) : Optional.empty();
        }

        @Override
        public boolean test(RegistryEntry<T> registryEntry) {
            return this.tag.contains(registryEntry);
        }

        @Override
        public String asString() {
            return "#" + this.tag.getTag().id();
        }
    }

    private record EntryBased<T>(RegistryEntry.Reference<T> value) implements CRegistryEntryPredicateArgumentType.EntryPredicate<T> {

        @Override
        public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
            return Either.left(this.value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E> Optional<CRegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.value.registryKey().isOf(registryRef) ? Optional.of((EntryPredicate<E>) this) : Optional.empty();
        }

        @Override
        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.equals(this.value);
        }

        @Override
        public String asString() {
            return this.value.registryKey().getValue().toString();
        }
    }
}
