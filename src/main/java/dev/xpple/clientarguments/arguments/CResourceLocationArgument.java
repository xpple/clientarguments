package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CResourceLocationArgument implements ArgumentType<ResourceLocation> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
	private static final DynamicCommandExceptionType UNKNOWN_ADVANCEMENT_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("advancement.advancementNotFound", id));
	private static final DynamicCommandExceptionType UNKNOWN_RECIPE_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("recipe.notFound", id));

	public static CResourceLocationArgument id() {
		return new CResourceLocationArgument();
	}

	public static ResourceLocation getId(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, ResourceLocation.class);
	}

	@Override
	public ResourceLocation parse(final StringReader stringReader) throws CommandSyntaxException {
		return ResourceLocation.read(stringReader);
	}

	public static RecipeHolder<?> getRecipe(final CommandContext<FabricClientCommandSource> context, final String argumentName) throws CommandSyntaxException {
		RecipeManager recipeManager = context.getSource().getWorld().getRecipeManager();
		ResourceLocation id = getId(context, argumentName);
		return recipeManager.byKey(id).orElseThrow(() -> UNKNOWN_RECIPE_EXCEPTION.create(id));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
