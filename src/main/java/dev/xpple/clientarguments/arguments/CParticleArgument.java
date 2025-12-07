package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CParticleArgument implements ArgumentType<ParticleOptions> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle{foo:bar}");
	public static final DynamicCommandExceptionType UNKNOWN_PARTICLE_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("particle.notFound", id));
	public static final DynamicCommandExceptionType INVALID_OPTIONS_EXCEPTION = new DynamicCommandExceptionType(error -> Component.translatableEscape("particle.invalidOptions", error));
	private final HolderLookup.Provider holderLookupProvider;
	private static final TagParser<?> VALUE_PARSER = TagParser.create(NbtOps.INSTANCE);

	public CParticleArgument(CommandBuildContext buildContext) {
		this.holderLookupProvider = buildContext;
	}

	public static CParticleArgument particle(CommandBuildContext buildContext) {
		return new CParticleArgument(buildContext);
	}

	public static ParticleOptions getParticle(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ParticleOptions.class);
	}

	@Override
	public ParticleOptions parse(final StringReader stringReader) throws CommandSyntaxException {
		return readParameters(stringReader, this.holderLookupProvider);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static ParticleOptions readParameters(StringReader reader, HolderLookup.Provider holderLookupProvider) throws CommandSyntaxException {
		ParticleType<?> particleType = getType(reader, holderLookupProvider.lookupOrThrow(Registries.PARTICLE_TYPE));
		return readParameters(VALUE_PARSER, reader, particleType, holderLookupProvider);
	}

	private static ParticleType<?> getType(StringReader reader, HolderLookup<ParticleType<?>> holderLookup) throws CommandSyntaxException {
		Identifier id = Identifier.read(reader);
		ResourceKey<ParticleType<?>> resourceKey = ResourceKey.create(Registries.PARTICLE_TYPE, id);
		return holderLookup.get(resourceKey)
			.orElseThrow(() -> UNKNOWN_PARTICLE_EXCEPTION.createWithContext(reader, id))
			.value();
	}

	private static <T extends ParticleOptions, O> T readParameters(TagParser<O> tagParser, StringReader reader, ParticleType<T> type, HolderLookup.Provider provider) throws CommandSyntaxException {
		RegistryOps<O> ops = provider.createSerializationContext(tagParser.getOps());
		O nbt;
		if (reader.canRead() && reader.peek() == '{') {
			nbt = tagParser.parseAsArgument(reader);
		} else {
			nbt = ops.emptyMap();
		}

		return type.codec().codec().parse(ops, nbt).getOrThrow(INVALID_OPTIONS_EXCEPTION::create);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		HolderLookup.RegistryLookup<ParticleType<?>> registryLookup = this.holderLookupProvider.lookupOrThrow(Registries.PARTICLE_TYPE);
		return SharedSuggestionProvider.suggestResource(registryLookup.listElementIds().map(ResourceKey::identifier), builder);
	}
}
