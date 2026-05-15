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

import java.util.Set;

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

                .then(Commands.literal("list").executes(ctx -> listSpeakers(ctx.getSource())))

                .then(Commands.literal("stop").executes(ctx -> stopAll(ctx.getSource())))

                .then(Commands.literal("play")
                    .then(Commands.argument("sound", IdentifierArgument.id())
                        .suggests(SOUND_SUGGESTIONS)
                        .executes(ctx -> playAll(ctx, 1.0f, 1.0f, OggSpeakerMod.DEFAULT_RANGE_BLOCKS))
                        .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 1.0f))
                            .executes(ctx -> playAll(ctx,
                                FloatArgumentType.getFloat(ctx, "volume"), 1.0f, OggSpeakerMod.DEFAULT_RANGE_BLOCKS))
                            .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5f, 2.0f))
                                .executes(ctx -> playAll(ctx,
                                    FloatArgumentType.getFloat(ctx, "volume"),
                                    FloatArgumentType.getFloat(ctx, "pitch"),
                                    OggSpeakerMod.DEFAULT_RANGE_BLOCKS))
                                .then(Commands.argument("range", FloatArgumentType.floatArg(1.0f))
                                    .executes(ctx -> playAll(ctx,
                                        FloatArgumentType.getFloat(ctx, "volume"),
                                        FloatArgumentType.getFloat(ctx, "pitch"),
                                        FloatArgumentType.getFloat(ctx, "range")))))))));
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

    private static int listSpeakers(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Set<BlockPos> positions = SpeakerBlockEntity.getAllPositions(level);
        StringBuilder sb = new StringBuilder("Loaded speakers in ")
            .append(level.dimension()).append(": ").append(positions.size()).append('\n');
        for (BlockPos p : positions) sb.append(" - ").append(p.toShortString()).append('\n');
        sb.append("Registered sound slots: ").append(OggSpeakerMod.SOUND_SLOT_COUNT);
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return positions.size();
    }

    private static int playAll(CommandContext<CommandSourceStack> ctx,
                               float volume, float pitch, float range) throws CommandSyntaxException {
        Identifier soundId = IdentifierArgument.getId(ctx, "sound");
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        Set<BlockPos> positions = SpeakerBlockEntity.getAllPositions(level);
        if (positions.isEmpty()) {
            src.sendFailure(Component.literal("No OGG Speakers placed in this dimension — use /speaker give and place one."));
            return 0;
        }
        int played = 0;
        for (BlockPos pos : positions) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SpeakerBlockEntity speaker)) continue;
            SpeakerSoundManager.play(level, pos, soundId, volume, pitch, range);
            speaker.rememberLastPlayed(soundId, volume, pitch, range);
            played++;
        }
        final int count = played;
        src.sendSuccess(() -> Component.literal(
            "Playing " + soundId + " on " + count + " speaker(s)"
            + " (vol=" + volume + " pitch=" + pitch + " range=" + range + ")"), true);
        return played;
    }

    private static int stopAll(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Set<BlockPos> positions = SpeakerBlockEntity.getAllPositions(level);
        int stopped = 0;
        for (BlockPos pos : positions) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SpeakerBlockEntity speaker)) continue;
            Identifier last = speaker.getLastSoundId();
            if (last == null) continue;
            SpeakerSoundManager.stop(level, pos, last, speaker.getLastRange());
            stopped++;
        }
        final int count = stopped;
        src.sendSuccess(() -> Component.literal("Stopped sound on " + count + " speaker(s)"), true);
        return stopped;
    }
}
