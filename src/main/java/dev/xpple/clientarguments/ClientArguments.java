package dev.xpple.clientarguments;

import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.xpple.clientarguments.arguments.CEntitySelectorOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import static dev.xpple.clientarguments.arguments.CAngleArgument.*;
import static dev.xpple.clientarguments.arguments.CBlockPosArgument.*;
import static dev.xpple.clientarguments.arguments.CBlockPredicateArgument.*;
import static dev.xpple.clientarguments.arguments.CBlockStateArgument.*;
import static dev.xpple.clientarguments.arguments.CColorArgument.*;
import static dev.xpple.clientarguments.arguments.CColumnPosArgument.*;
import static dev.xpple.clientarguments.arguments.CDimensionArgument.*;
import static dev.xpple.clientarguments.arguments.CEntityAnchorArgument.*;
import static dev.xpple.clientarguments.arguments.CEntityArgument.*;
import static dev.xpple.clientarguments.arguments.CEnumArgument.*;
import static dev.xpple.clientarguments.arguments.CGameProfileArgument.*;
import static dev.xpple.clientarguments.arguments.CResourceLocationArgument.*;
import static dev.xpple.clientarguments.arguments.CItemPredicateArgument.*;
import static dev.xpple.clientarguments.arguments.CSlotArgument.*;
import static dev.xpple.clientarguments.arguments.CItemArgument.*;
import static dev.xpple.clientarguments.arguments.CMessageArgument.*;
import static dev.xpple.clientarguments.arguments.CCompoundTagArgument.*;
import static dev.xpple.clientarguments.arguments.CNbtTagArgument.*;
import static dev.xpple.clientarguments.arguments.CNbtPathArgument.*;
import static dev.xpple.clientarguments.arguments.CRangeArgument.*;
import static dev.xpple.clientarguments.arguments.COperationArgument.*;
import static dev.xpple.clientarguments.arguments.CParticleArgument.*;
import static dev.xpple.clientarguments.arguments.CResourceOrIdArgument.*;
import static dev.xpple.clientarguments.arguments.CResourceOrTagArgument.*;
import static dev.xpple.clientarguments.arguments.CResourceArgument.*;
import static dev.xpple.clientarguments.arguments.CResourceKeyArgument.*;
import static dev.xpple.clientarguments.arguments.CResourceOrTagKeyArgument.*;
import static dev.xpple.clientarguments.arguments.CRotationArgument.*;
import static dev.xpple.clientarguments.arguments.CScoreHolderArgument.*;
import static dev.xpple.clientarguments.arguments.CObjectiveCriteriaArgument.*;
import static dev.xpple.clientarguments.arguments.CObjectiveArgument.*;
import static dev.xpple.clientarguments.arguments.CScoreboardSlotArgument.*;
import static dev.xpple.clientarguments.arguments.CSlotsArgument.*;
import static dev.xpple.clientarguments.arguments.CStyleArgument.*;
import static dev.xpple.clientarguments.arguments.CSwizzleArgument.*;
import static dev.xpple.clientarguments.arguments.CTeamArgument.*;
import static dev.xpple.clientarguments.arguments.CTestClassNameArgument.*;
import static dev.xpple.clientarguments.arguments.CTestFunctionArgument.*;
import static dev.xpple.clientarguments.arguments.CComponentArgument.*;
import static dev.xpple.clientarguments.arguments.CTimeArgument.*;
import static dev.xpple.clientarguments.arguments.CUuidArgument.*;
import static dev.xpple.clientarguments.arguments.CVec2Argument.*;
import static dev.xpple.clientarguments.arguments.CVec3Argument.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ClientArguments implements ClientModInitializer {
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.locate.structure.invalid", id));

    @Override
    public void onInitializeClient() {
        CEntitySelectorOptions.register();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ClientCommandRegistrationCallback.EVENT.register(ClientArguments::registerTestCommand);
        }
    }

    /**
     * <p>
     * Registering this test command will trigger {@link com.mojang.brigadier.tree.CommandNode#findAmbiguities(AmbiguityConsumer)},
     * which checks the validity of the example inputs - and with that also the validity of the argument in question.
     */
    private static void registerTestCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(literal("clientarguments:test")
            .then(literal("angle").then(argument("angle", angle())
                .executes(ctx -> consume(getAngle(ctx, "angle")))))
            .then(literal("blockpos").then(argument("blockpos", blockPos())
                .executes(ctx -> consume(getBlockPos(ctx, "blockpos")))))
            .then(literal("blockpredicate").then(argument("blockpredicate", blockPredicate(registryAccess))
                .executes(ctx -> consume(getBlockPredicate(ctx, "blockpredicate")))))
            .then(literal("blockstate").then(argument("blockstate", blockState(registryAccess))
                .executes(ctx -> consume(getBlockState(ctx, "blockstate")))))
            .then(literal("color").then(argument("color", color())
                .executes(ctx -> consume(getColor(ctx, "color")))))
            .then(literal("columnpos").then(argument("columnpos", columnPos())
                .executes(ctx -> consume(getColumnPos(ctx, "columnpos")))))
            .then(literal("dimension").then(argument("dimension", dimension())
                .executes(ctx -> consume(getDimension(ctx, "dimension")))))
            .then(literal("entityanchor").then(argument("entityanchor", entityAnchor())
                .executes(ctx -> consume(getEntityAnchor(ctx, "entityanchor")))))
            .then(literal("entity").then(argument("entity", entity())
                .executes(ctx -> consume(getEntity(ctx, "entity")))))
            .then(literal("enum").then(argument("enum", enumArg(GameType.class))
                .executes(ctx -> consume(getEnum(ctx, "enum")))))
            .then(literal("gameprofile").then(argument("gameprofile", gameProfile())
                .executes(ctx -> consume(getProfileArgument(ctx, "gameprofile")))))
            .then(literal("identifier").then(argument("identifier", id())
                .executes(ctx -> consume(getId(ctx, "identifier")))))
            .then(literal("itempredicate").then(argument("itempredicate", itemPredicate(registryAccess))
                .executes(ctx -> consume(getItemStackPredicate(ctx, "itempredicate")))))
            .then(literal("itemslot").then(argument("itemslot", itemSlot())
                .executes(ctx -> consume(getItemSlot(ctx, "itemslot")))))
            .then(literal("itemstack").then(argument("itemstack", itemStack(registryAccess))
                .executes(ctx -> consume(getItemStackArgument(ctx, "itemstack")))))
            .then(literal("message").then(argument("message", message())
                .executes(ctx -> consume(getMessage(ctx, "message")))))
            .then(literal("nbtcompound").then(argument("nbtcompound", compoundTag())
                .executes(ctx -> consume(getCompoundTag(ctx, "nbtcompound")))))
            .then(literal("nbtelement").then(argument("nbtelement", nbtTag())
                .executes(ctx -> consume(getNbtTag(ctx, "nbtelement")))))
            .then(literal("nbtpath").then(argument("nbtpath", nbtPath())
                .executes(ctx -> consume(getNbtPath(ctx, "nbtpath")))))
            .then(literal("intrange").then(argument("intrange", intRange())
                .executes(ctx -> consume(Ints.getRangeArgument(ctx, "intrange")))))
            .then(literal("floatrange").then(argument("floatrange", floatRange())
                .executes(ctx -> consume(Floats.getRangeArgument(ctx, "floatrange")))))
            .then(literal("operation").then(argument("operation", operation())
                .executes(ctx -> consume(getOperation(ctx, "operation")))))
            .then(literal("particleeffect").then(argument("particleeffect", particle(registryAccess))
                .executes(ctx -> consume(getParticle(ctx, "particleeffect")))))
            .then(literal("registryentry").then(argument("registryentry", lootTable(registryAccess))
                .executes(ctx -> consume(getLootTable(ctx, "registryentry")))))
            .then(literal("registryentrypredicate").then(argument("registryentrypredicate", resourceOrTag(registryAccess, Registries.BIOME))
                .executes(ctx -> consume(getResourceOrTag(ctx, "registryentrypredicate", Registries.BIOME)))))
            .then(literal("registryentryreference").then(argument("registryentryreference", registryEntry(registryAccess, Registries.ENCHANTMENT))
                .executes(ctx -> consume(getEnchantment(ctx, "registryentryreference")))))
            .then(literal("registrykey").then(argument("registrykey", key(Registries.STRUCTURE))
                .executes(ctx -> consume(getKey(ctx, "registrykey", Registries.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))
            .then(literal("registrypredicate").then(argument("registrypredicate", registryPredicate(Registries.STRUCTURE))
                .executes(ctx -> consume(getPredicate(ctx, "registrypredicate", Registries.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))
            .then(literal("rotation").then(argument("rotation", rotation())
                .executes(ctx -> consume(getRotation(ctx, "rotation")))))
            .then(literal("scoreboardcriterion").then(argument("scoreboardcriterion", criteria())
                .executes(ctx -> consume(getCriteria(ctx, "scoreboardcriterion")))))
            .then(literal("scoreboardobjective").then(argument("scoreboardobjective", objective())
                .executes(ctx -> consume(getObjective(ctx, "scoreboardobjective")))))
            .then(literal("scoreboardslot").then(argument("scoreboardslot", scoreboardSlot())
                .executes(ctx -> consume(getScoreboardSlot(ctx, "scoreboardslot")))))
            .then(literal("scoreholder").then(argument("scoreholder", scoreHolder())
                .executes(ctx -> consume(getScoreHolder(ctx, "scoreholder")))))
            .then(literal("slotrange").then(argument("slotrange", slots())
                .executes(ctx -> consume(getSlots(ctx, "slotrange")))))
            .then(literal("style").then(argument("style", style(registryAccess))
                .executes(ctx -> consume(getStyle(ctx, "style")))))
            .then(literal("swizzle").then(argument("swizzle", swizzle())
                .executes(ctx -> consume(getSwizzle(ctx, "swizzle")))))
            .then(literal("team").then(argument("team", team())
                .executes(ctx -> consume(getTeam(ctx, "team")))))
            .then(literal("testclass").then(argument("testclass", testClassName())
                .executes(ctx -> consume(getTestClassName(ctx, "testclass")))))
            .then(literal("testfunction").then(argument("testfunction", testFunction())
                .executes(ctx -> consume(getFunction(ctx, "testfunction")))))
            .then(literal("text").then(argument("text", textComponent(registryAccess))
                .executes(ctx -> consume(getComponent(ctx, "text")))))
            .then(literal("time").then(argument("time", time())
                .executes(ctx -> consume(getTime(ctx, "time")))))
            .then(literal("uuid").then(argument("uuid", uuid())
                .executes(ctx -> consume(getUuid(ctx, "uuid")))))
            .then(literal("vec2").then(argument("vec2", vec2())
                .executes(ctx -> consume(getVec2(ctx, "vec2")))))
            .then(literal("vec3").then(argument("vec3", vec3())
                .executes(ctx -> consume(getVec3(ctx, "vec3")))))
        );
    }

    private static int consume(Object object) {
        return Command.SINGLE_SUCCESS;
    }
}
