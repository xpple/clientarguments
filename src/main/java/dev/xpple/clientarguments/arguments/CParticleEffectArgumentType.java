package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CParticleEffectArgumentType implements ArgumentType<ParticleEffect> {

	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle with options");
	public static final DynamicCommandExceptionType UNKNOWN_PARTICLE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("particle.notFound", id));

	public static CParticleEffectArgumentType particleEffect() {
		return new CParticleEffectArgumentType();
	}

	public static ParticleEffect getCParticle(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ParticleEffect.class);
	}

	@Override
	public ParticleEffect parse(final StringReader stringReader) throws CommandSyntaxException {
		return readParameters(stringReader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestIdentifiers(Registry.PARTICLE_TYPE.getIds(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static ParticleEffect readParameters(StringReader reader) throws CommandSyntaxException {
		Identifier identifier = Identifier.fromCommandInput(reader);
		ParticleType<?> particleType = Registry.PARTICLE_TYPE.getOrEmpty(identifier).orElseThrow(() -> UNKNOWN_PARTICLE_EXCEPTION.create(identifier));
		return readParameters(reader, particleType);
	}

	private static <T extends ParticleEffect> T readParameters(StringReader reader, ParticleType<T> type) throws CommandSyntaxException {
		return type.getParametersFactory().read(type, reader);
	}
}
