package com.yourname.magi.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Carves a bowl-shaped crater a few columns per tick (no lag spike, no item spam).
 * Respects claims/protection mods (fires BlockEvent.BreakEvent as the caster), never touches unbreakable blocks,
 * block entities (chests etc.) or fluids. Sand on the crater floor fuses into glass (fulgurite), stone scorches.
 */
public class CraterCarver extends ArtSequence {
    private static final int COLUMNS_PER_TICK = 48;
    private final BlockPos center;
    private final int radius;
    private final List<int[]> columns = new ArrayList<>();
    private int index;

    public CraterCarver(ServerLevel level, ServerPlayer caster, BlockPos center, int radius) {
        super(level, caster);
        this.center = center;
        this.radius = radius;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= radius * radius) columns.add(new int[]{dx, dz});
        columns.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
    }

    @Override
    protected boolean onTick(int age) {
        RandomSource rnd = level.random;
        double depth = radius * 0.55;
        int end = Math.min(columns.size(), index + COLUMNS_PER_TICK);
        for (; index < end; index++) {
            int dx = columns.get(index)[0], dz = columns.get(index)[1];
            double r = Math.sqrt(dx * dx + dz * dz) / radius;
            int bottom = center.getY() - (int) Math.round(depth * Math.sqrt(Math.max(0, 1 - r * r)));
            int top = center.getY() + (int) Math.round(radius * 0.45 * (1 - r));
            for (int y = top; y > bottom; y--) carve(new BlockPos(center.getX() + dx, y, center.getZ() + dz));
            scorch(new BlockPos(center.getX() + dx, bottom, center.getZ() + dz), rnd);
        }
        return index < columns.size();
    }

    private boolean allowed(BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) return false;
        if (!caster.isRemoved()) {
            return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, caster));
        }
        return true;
    }

    private void carve(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (allowed(pos, state)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private void scorch(BlockPos pos, RandomSource rnd) {
        BlockState state = level.getBlockState(pos);
        if (!allowed(pos, state)) return;
        BlockState result;
        if (state.is(BlockTags.SAND)) result = Blocks.GLASS.defaultBlockState();
        else if (rnd.nextFloat() < 0.35F) result = Blocks.BLACKSTONE.defaultBlockState();
        else if (rnd.nextFloat() < 0.2F) result = Blocks.SMOOTH_BASALT.defaultBlockState();
        else return;
        level.setBlock(pos, result, Block.UPDATE_ALL);
    }
}
