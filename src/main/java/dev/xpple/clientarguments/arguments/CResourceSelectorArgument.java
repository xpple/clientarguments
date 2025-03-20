package dev.xpple.clientarguments.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FilenameUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CResourceSelectorArgument<T> implements ArgumentType<Collection<Holder.Reference<T>>> {
    private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
    public static final Dynamic2CommandExceptionType ERROR_NO_MATCHES = new Dynamic2CommandExceptionType(
        (object, object2) -> Component.translatableEscape("argument.resource_selector.not_found", object, object2)
    );
    final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    CResourceSelectorArgument(CommandBuildContext commandBuildContext, ResourceKey<? extends Registry<T>> resourceKey) {
        this.registryKey = resourceKey;
        this.registryLookup = commandBuildContext.lookupOrThrow(resourceKey);
    }

    public Collection<Holder.Reference<T>> parse(StringReader stringReader) throws CommandSyntaxException {
        String string = ensureNamespaced(readPattern(stringReader));
        List<Holder.Reference<T>> list = this.registryLookup.listElements().filter(reference -> matches(string, reference.key().location())).toList();
        if (list.isEmpty()) {
            throw ERROR_NO_MATCHES.createWithContext(stringReader, string, this.registryKey.location());
        } else {
            return list;
        }
    }

    public static <T> Collection<Holder.Reference<T>> parse(StringReader stringReader, HolderLookup<T> holderLookup) {
        String string = ensureNamespaced(readPattern(stringReader));
        return holderLookup.listElements().filter(reference -> matches(string, reference.key().location())).toList();
    }

    private static String readPattern(StringReader stringReader) {
        int i = stringReader.getCursor();

        while (stringReader.canRead() && isAllowedPatternCharacter(stringReader.peek())) {
            stringReader.skip();
        }

        return stringReader.getString().substring(i, stringReader.getCursor());
    }

    private static boolean isAllowedPatternCharacter(char c) {
        return ResourceLocation.isAllowedInResourceLocation(c) || c == '*' || c == '?';
    }

    private static String ensureNamespaced(String string) {
        return !string.contains(":") ? "minecraft:" + string : string;
    }

    private static boolean matches(String string, ResourceLocation resourceLocation) {
        return FilenameUtils.wildcardMatch(resourceLocation.toString(), string);
    }

    public static <T> CResourceSelectorArgument<T> resourceSelector(CommandBuildContext commandBuildContext, ResourceKey<? extends Registry<T>> resourceKey) {
        return new CResourceSelectorArgument<>(commandBuildContext, resourceKey);
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<Holder.Reference<T>> getSelectedResources(
        CommandContext<FabricClientCommandSource> commandContext, String string, ResourceKey<? extends Registry<T>> resourceKey
    ) {
        return commandContext.getArgument(string, Collection.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return commandContext.getSource() instanceof SharedSuggestionProvider sharedSuggestionProvider
            ? sharedSuggestionProvider.suggestRegistryElements(
            this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, suggestionsBuilder, commandContext
        )
            : SharedSuggestionProvider.suggest(this.registryLookup.listElementIds().map(ResourceKey::location).map(ResourceLocation::toString), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<CResourceSelectorArgument<T>, CResourceSelectorArgument.Info<T>.Template> {
        public void serializeToNetwork(CResourceSelectorArgument.Info<T>.Template template, FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeResourceKey(template.registryKey);
        }

        public CResourceSelectorArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            return new Template(friendlyByteBuf.readRegistryKey());
        }

        public void serializeToJson(CResourceSelectorArgument.Info<T>.Template template, JsonObject jsonObject) {
            jsonObject.addProperty("registry", template.registryKey.location().toString());
        }

        public CResourceSelectorArgument.Info<T>.Template unpack(CResourceSelectorArgument<T> resourceSelectorArgument) {
            return new Template(resourceSelectorArgument.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<CResourceSelectorArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(final ResourceKey<? extends Registry<T>> resourceKey) {
                this.registryKey = resourceKey;
            }

            public CResourceSelectorArgument<T> instantiate(CommandBuildContext commandBuildContext) {
                return new CResourceSelectorArgument<>(commandBuildContext, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<CResourceSelectorArgument<T>, ?> type() {
                return CResourceSelectorArgument.Info.this;
            }
        }
    }
}
