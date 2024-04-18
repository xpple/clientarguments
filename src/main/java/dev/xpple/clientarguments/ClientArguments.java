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
import dev.xpple.clientarguments.arguments.CNumberRangeArgumentType.IntRangeArgumentType;
import dev.xpple.clientarguments.arguments.CNumberRangeArgumentType.FloatRangeArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import static dev.xpple.clientarguments.arguments.CAngleArgumentType.*;
import static dev.xpple.clientarguments.arguments.CBlockPosArgumentType.*;
import static dev.xpple.clientarguments.arguments.CBlockPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CBlockStateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CColorArgumentType.*;
import static dev.xpple.clientarguments.arguments.CColumnPosArgumentType.*;
import static dev.xpple.clientarguments.arguments.CDimensionArgumentType.*;
import static dev.xpple.clientarguments.arguments.CEntityAnchorArgumentType.*;
import static dev.xpple.clientarguments.arguments.CEntityArgumentType.*;
import static dev.xpple.clientarguments.arguments.CEnumArgumentType.*;
import static dev.xpple.clientarguments.arguments.CGameProfileArgumentType.*;
import static dev.xpple.clientarguments.arguments.CIdentifierArgumentType.*;
import static dev.xpple.clientarguments.arguments.CItemPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CItemSlotArgumentType.*;
import static dev.xpple.clientarguments.arguments.CItemStackArgumentType.*;
import static dev.xpple.clientarguments.arguments.CMessageArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNbtCompoundArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNbtElementArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNbtPathArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNumberRangeArgumentType.*;
import static dev.xpple.clientarguments.arguments.COperationArgumentType.*;
import static dev.xpple.clientarguments.arguments.CParticleEffectArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryEntryArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryEntryPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryEntryReferenceArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryKeyArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRotationArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreHolderArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreboardCriterionArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreboardObjectiveArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreboardSlotArgumentType.*;
import static dev.xpple.clientarguments.arguments.CSlotRangeArgumentType.*;
import static dev.xpple.clientarguments.arguments.CStyleArgumentType.*;
import static dev.xpple.clientarguments.arguments.CSwizzleArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTeamArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTestClassArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTestFunctionArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTextArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTimeArgumentType.*;
import static dev.xpple.clientarguments.arguments.CUuidArgumentType.*;
import static dev.xpple.clientarguments.arguments.CVec2ArgumentType.*;
import static dev.xpple.clientarguments.arguments.CVec3ArgumentType.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ClientArguments implements ClientModInitializer {
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("commands.locate.structure.invalid", id));

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
    private static void registerTestCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
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
            .then(literal("enum").then(argument("enum", enumArg(GameMode.class))
                .executes(ctx -> consume(getEnum(ctx, "enum")))))
            .then(literal("gameprofile").then(argument("gameprofile", gameProfile())
                .executes(ctx -> consume(getProfileArgument(ctx, "gameprofile")))))
            .then(literal("identifier").then(argument("identifier", identifier())
                .executes(ctx -> consume(getIdentifier(ctx, "identifier")))))
            .then(literal("itempredicate").then(argument("itempredicate", itemPredicate(registryAccess))
                .executes(ctx -> consume(getItemStackPredicate(ctx, "itempredicate")))))
            .then(literal("itemslot").then(argument("itemslot", itemSlot())
                .executes(ctx -> consume(getItemSlot(ctx, "itemslot")))))
            .then(literal("itemstack").then(argument("itemstack", itemStack(registryAccess))
                .executes(ctx -> consume(getItemStackArgument(ctx, "itemstack")))))
            .then(literal("message").then(argument("message", message())
                .executes(ctx -> consume(getMessage(ctx, "message")))))
            .then(literal("nbtcompound").then(argument("nbtcompound", nbtCompound())
                .executes(ctx -> consume(getNbtCompound(ctx, "nbtcompound")))))
            .then(literal("nbtelement").then(argument("nbtelement", nbtElement())
                .executes(ctx -> consume(getNbtElement(ctx, "nbtelement")))))
            .then(literal("nbtpath").then(argument("nbtpath", nbtPath())
                .executes(ctx -> consume(getNbtPath(ctx, "nbtpath")))))
            .then(literal("intrange").then(argument("intrange", intRange())
                .executes(ctx -> consume(IntRangeArgumentType.getRangeArgument(ctx, "intrange")))))
            .then(literal("floatrange").then(argument("floatrange", floatRange())
                .executes(ctx -> consume(FloatRangeArgumentType.getRangeArgument(ctx, "floatrange")))))
            .then(literal("operation").then(argument("operation", operation())
                .executes(ctx -> consume(getOperation(ctx, "operation")))))
            .then(literal("particleeffect").then(argument("particleeffect", particleEffect(registryAccess))
                .executes(ctx -> consume(getParticle(ctx, "particleeffect")))))
            .then(literal("registryentry").then(argument("registryentry", lootTable(registryAccess))
                .executes(ctx -> consume(getLootTable(ctx, "registryentry")))))
            .then(literal("registryentrypredicate").then(argument("registryentrypredicate", registryEntryPredicate(registryAccess, RegistryKeys.BIOME))
                .executes(ctx -> consume(getRegistryEntryPredicate(ctx, "registryentrypredicate", RegistryKeys.BIOME)))))
            .then(literal("registryentryreference").then(argument("registryentryreference", registryEntry(registryAccess, RegistryKeys.ENCHANTMENT))
                .executes(ctx -> consume(getEnchantment(ctx, "registryentryreference")))))
            .then(literal("registrykey").then(argument("registrykey", registryKey(RegistryKeys.STRUCTURE))
                .executes(ctx -> consume(getKey(ctx, "registrykey", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))
            .then(literal("registrypredicate").then(argument("registrypredicate", registryPredicate(RegistryKeys.STRUCTURE))
                .executes(ctx -> consume(getPredicate(ctx, "registrypredicate", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))
            .then(literal("rotation").then(argument("rotation", rotation())
                .executes(ctx -> consume(getRotation(ctx, "rotation")))))
            .then(literal("scoreboardcriterion").then(argument("scoreboardcriterion", scoreboardCriterion())
                .executes(ctx -> consume(getCriterion(ctx, "scoreboardcriterion")))))
            .then(literal("scoreboardobjective").then(argument("scoreboardobjective", scoreboardObjective())
                .executes(ctx -> consume(getObjective(ctx, "scoreboardobjective")))))
            .then(literal("scoreboardslot").then(argument("scoreboardslot", scoreboardSlot())
                .executes(ctx -> consume(getScoreboardSlot(ctx, "scoreboardslot")))))
            .then(literal("scoreholder").then(argument("scoreholder", scoreHolder())
                .executes(ctx -> consume(getScoreHolder(ctx, "scoreholder")))))
            .then(literal("slotrange").then(argument("slotrange", slotRange())
                .executes(ctx -> consume(getSlotRange(ctx, "slotrange")))))
            .then(literal("style").then(argument("style", style(registryAccess))
                .executes(ctx -> consume(getStyle(ctx, "style")))))
            .then(literal("swizzle").then(argument("swizzle", swizzle())
                .executes(ctx -> consume(getSwizzle(ctx, "swizzle")))))
            .then(literal("team").then(argument("team", team())
                .executes(ctx -> consume(getTeam(ctx, "team")))))
            .then(literal("testclass").then(argument("testclass", testClass())
                .executes(ctx -> consume(getTestClass(ctx, "testclass")))))
            .then(literal("testfunction").then(argument("testfunction", testFunction())
                .executes(ctx -> consume(getFunction(ctx, "testfunction")))))
            .then(literal("text").then(argument("text", text(registryAccess))
                .executes(ctx -> consume(getTextArgument(ctx, "text")))))
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
