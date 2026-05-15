package io.github.kacarong.oggspeaker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.kacarong.oggspeaker.OggSpeakerMod;
import io.github.kacarong.oggspeaker.block.SpeakerBlockEntity;
import io.github.kacarong.oggspeaker.registry.ModItems;
import io.github.kacarong.oggspeaker.registry.ModSounds;
import io.github.kacarong.oggspeaker.sound.SpeakerSoundManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SpeakerCommand {
    private SpeakerCommand() {}

    private static final SuggestionProvider<CommandSourceStack> SOUND_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggestResource(
            BuiltInRegistries.SOUND_EVENT.keySet().stream(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("speaker")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))

                .then(Commands.literal("give").executes(ctx -> giveSpeaker(ctx.getSource())))

                .then(Commands.literal("list").executes(ctx -> listSlots(ctx.getSource())))

                .then(Commands.literal("stop")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                            return stopAt(ctx.getSource(), pos);
                        })))

                .then(Commands.literal("play")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("sound", IdentifierArgument.id())
                            .suggests(SOUND_SUGGESTIONS)
                            .executes(ctx -> playAt(ctx, 1.0f, 1.0f, OggSpeakerMod.DEFAULT_RANGE_BLOCKS))
                            .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f))
                                .executes(ctx -> playAt(ctx,
                                    FloatArgumentType.getFloat(ctx, "volume"), 1.0f, OggSpeakerMod.DEFAULT_RANGE_BLOCKS))
                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5f, 2.0f))
                                    .executes(ctx -> playAt(ctx,
                                        FloatArgumentType.getFloat(ctx, "volume"),
                                        FloatArgumentType.getFloat(ctx, "pitch"),
                                        OggSpeakerMod.DEFAULT_RANGE_BLOCKS))
                                    .then(Commands.argument("range", FloatArgumentType.floatArg(1.0f))
                                        .executes(ctx -> playAt(ctx,
                                            FloatArgumentType.getFloat(ctx, "volume"),
                                            FloatArgumentType.getFloat(ctx, "pitch"),
                                            FloatArgumentType.getFloat(ctx, "range"))))))))));
    }

    private static int giveSpeaker(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("/speaker give must be run by a player"));
            return 0;
        }
        ItemStack stack = new ItemStack(ModItems.SPEAKER_ITEM);
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(player.level(),
                player.getX(), player.getY(), player.getZ(), stack);
            player.level().addFreshEntity(drop);
        }
        src.sendSuccess(() -> Component.literal("Gave 1 OGG Speaker to " + player.getName().getString()), true);
        return 1;
    }

    private static int listSlots(CommandSourceStack src) {
        StringBuilder sb = new StringBuilder("Registered slots (").append(OggSpeakerMod.SOUND_SLOT_COUNT).append("):\n");
        for (Identifier id : ModSounds.getSlotIds()) sb.append(" - ").append(id).append('\n');
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return ModSounds.getSlotIds().size();
    }

    private static int stopAt(CommandSourceStack src, BlockPos pos) {
        ServerLevel level = src.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SpeakerBlockEntity speaker)) {
            src.sendFailure(Component.literal("No OGG Speaker at " + pos.toShortString()));
            return 0;
        }
        Identifier last = speaker.getLastSoundId();
        if (last == null) {
            src.sendFailure(Component.literal("Speaker has no sound to stop"));
            return 0;
        }
        SpeakerSoundManager.stop(level, pos, last, speaker.getLastRange());
        src.sendSuccess(() -> Component.literal("Stopped " + last + " near " + pos.toShortString()), true);
        return 1;
    }

    private static int playAt(CommandContext<CommandSourceStack> ctx,
                              float volume, float pitch, float range) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        Identifier soundId = IdentifierArgument.getId(ctx, "sound");
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SpeakerBlockEntity speaker)) {
            src.sendFailure(Component.literal("No OGG Speaker at " + pos.toShortString() + " — place one first or use /speaker give"));
            return 0;
        }
        boolean ok = SpeakerSoundManager.play(level, pos, soundId, volume, pitch, range);
        if (ok) {
            speaker.rememberLastPlayed(soundId, volume, pitch, range);
            src.sendSuccess(() -> Component.literal(
                "Playing " + soundId + " at " + pos.toShortString()
                + " (vol=" + volume + " pitch=" + pitch + " range=" + range + ")"), true);
            return 1;
        }
        src.sendFailure(Component.literal("Failed to play sound: " + soundId));
        return 0;
    }
}
