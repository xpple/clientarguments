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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CRegistryEntryPredicateArgumentType<T> implements ArgumentType<CRegistryEntryPredicateArgumentType.EntryPredicate<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((tag, type) -> Text.stringifiedTranslatable("argument.resource_tag.not_found", tag, type));
    private static final Dynamic3CommandExceptionType WRONG_TYPE_EXCEPTION = new Dynamic3CommandExceptionType((tag, type, expectedType) -> Text.stringifiedTranslatable("argument.resource_tag.invalid_type", tag, type, expectedType));
    private final RegistryWrapper<T> registryWrapper;
    final RegistryKey<? extends Registry<T>> registryRef;

    public CRegistryEntryPredicateArgumentType(CommandRegistryAccess registryAccess, RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
        this.registryWrapper = registryAccess.getWrapperOrThrow(registryRef);
    }

    public static <T> CRegistryEntryPredicateArgumentType<T> registryEntryPredicate(CommandRegistryAccess registryAccess, RegistryKey<? extends Registry<T>> registryRef) {
        return new CRegistryEntryPredicateArgumentType<>(registryAccess, registryRef);
    }

    public static <T> EntryPredicate<T> getRegistryEntryPredicate(final CommandContext<FabricClientCommandSource> context, final String name, final RegistryKey<Registry<T>> registryRef) throws CommandSyntaxException {
        EntryPredicate<?> entryPredicate = context.getArgument(name, EntryPredicate.class);
        Optional<EntryPredicate<T>> optional = entryPredicate.tryCast(registryRef);
        return optional.orElseThrow(() -> entryPredicate.getEntry().map(entry -> {
            RegistryKey<?> registryKey2 = entry.registryKey();
            return RegistryEntryReferenceArgumentType.INVALID_TYPE_EXCEPTION.create(registryKey2.getValue(), registryKey2.getRegistry(), registryRef.getValue());
        }, entryList -> {
            TagKey<?> tagKey = entryList.getTag();
            return WRONG_TYPE_EXCEPTION.create(tagKey.id(), tagKey.registry(), registryRef.getValue());
        }));
    }

    @Override
    public EntryPredicate<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            int i = stringReader.getCursor();

            try {
                stringReader.skip();
                Identifier identifier = Identifier.fromCommandInput(stringReader);
                TagKey<T> tagKey = TagKey.of(this.registryRef, identifier);
                RegistryEntryList.Named<T> named = this.registryWrapper
                    .getOptional(tagKey)
                    .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier, this.registryRef.getValue()));
                return new TagBased<>(named);
            } catch (CommandSyntaxException var6) {
                stringReader.setCursor(i);
                throw var6;
            }
        } else {
            Identifier identifier2 = Identifier.fromCommandInput(stringReader);
            RegistryKey<T> registryKey = RegistryKey.of(this.registryRef, identifier2);
            RegistryEntry.Reference<T> reference = this.registryWrapper
                .getOptional(registryKey)
                .orElseThrow(() -> RegistryEntryReferenceArgumentType.NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier2, this.registryRef.getValue()));
            return new EntryBased<>(reference);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        CommandSource.suggestIdentifiers(this.registryWrapper.streamTagKeys().map(TagKey::id), builder, "#");
        return CommandSource.suggestIdentifiers(this.registryWrapper.streamKeys().map(RegistryKey::getValue), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    record EntryBased<T>(RegistryEntry.Reference<T> value) implements EntryPredicate<T> {
        @Override
        public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
            return Either.left(this.value);
        }

        @Override
        public <E> Optional<EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.value.registryKey().isOf(registryRef) ? Optional.of((EntryPredicate<E>) this) : Optional.empty();
        }

        public boolean test(RegistryEntry<T> registryEntry) {
            return registryEntry.equals(this.value);
        }

        @Override
        public String asString() {
            return this.value.registryKey().getValue().toString();
        }
    }

    public interface EntryPredicate<T> extends Predicate<RegistryEntry<T>> {
        Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry();

        <E> Optional<EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef);

        String asString();
    }

    record TagBased<T>(RegistryEntryList.Named<T> tag) implements EntryPredicate<T> {
        @Override
        public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
            return Either.right(this.tag);
        }

        @Override
        public <E> Optional<EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
            return this.tag.getTag().isOf(registryRef) ? Optional.of((EntryPredicate<E>) this) : Optional.empty();
        }

        public boolean test(RegistryEntry<T> registryEntry) {
            return this.tag.contains(registryEntry);
        }

        @Override
        public String asString() {
            return "#" + this.tag.getTag().id();
        }
    }
}
