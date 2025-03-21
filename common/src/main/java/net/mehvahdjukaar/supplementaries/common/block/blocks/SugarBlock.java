package net.mehvahdjukaar.supplementaries.common.block.blocks;


import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.integration.BumblezoneCompat;
import net.mehvahdjukaar.supplementaries.integration.CompatHandler;
import net.mehvahdjukaar.supplementaries.reg.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SugarBlock extends ConcretePowderBlock {

    public SugarBlock(BlockBehaviour.Properties properties) {
        super(Blocks.WATER, properties);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState blockState, BlockState blockState2, FallingBlockEntity fallingBlock) {
        if (level instanceof ServerLevel serverLevel) {
            this.tick(blockState, serverLevel, pos, level.random);
        }
        if (isWater(blockState2)) {
            //level.addDestroyBlockEffect(blockPos, blockState);
            //     level.destroyBlock(pos, false);
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        super.handlePrecipitation(state, level, pos, precipitation);
        if (precipitation == Biome.Precipitation.RAIN && CommonConfigs.Building.SUGAR_CUBE_RAIN.get()) {
            level.blockEvent(pos, state.getBlock(), 1, 0);
        }
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        level.scheduleTick(currentPos, this, this.getDelayAfterPlace());
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (touchesLiquid(level, pos)) {
            level.blockEvent(pos, state.getBlock(), 1, 0);
        } else super.tick(state, level, pos, random);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == 1) {
            if (level.isClientSide) {
                this.spawnDissolveParticles(level, pos);
            }
            if (shouldTurnToWater(level, pos)) {
                turnIntoWater(level, pos);
            } else level.removeBlock(pos, false);
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    private static void turnIntoWater(Level level, BlockPos pos) {
        if (CompatHandler.BUMBLEZONE) {
            BumblezoneCompat.turnToSugarWater(level, pos);
        } else {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        }
    }

    private boolean shouldTurnToWater(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN) {
                mutableBlockPos.setWithOffset(pos, direction);
                var s = level.getBlockState(mutableBlockPos);
                if (isWater(s) && (direction == Direction.UP || s.getFluidState().isSource())) {
                    count++;
                }
                if (count >= 2) return true;
            }
        }
        return false;
    }

    private boolean touchesLiquid(BlockGetter level, BlockPos pos) {
        boolean bl = false;
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        BlockState blockState = level.getBlockState(mutableBlockPos);
        if (isWater(blockState)) return true;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) continue;
            mutableBlockPos.setWithOffset(pos, direction);
            blockState = level.getBlockState(mutableBlockPos);
            if (isWater(blockState) && !blockState.isFaceSturdy(level, pos, direction.getOpposite())) {
                bl = true;
                break;
            }
        }
        return bl;
    }


    private boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }


    public void spawnDissolveParticles(Level level, BlockPos pos) {
        int d = 0, e = 0, f = 0;

        int amount = 4;
        for (int ax = 0; ax < amount; ++ax) {
            for (int ay = 0; ay < amount; ++ay) {
                for (int az = 0; az < amount; ++az) {
                    double s = (ax + 0.5) / amount;
                    double t = (ay + 0.5) / amount;
                    double u = (az + 0.5) / amount;
                    double px = s + d;
                    double py = t + e;
                    double pz = u + f;
                    level.addParticle(ModParticles.SUGAR_PARTICLE.get(),
                            pos.getX() + px, pos.getY() + py, pos.getZ() + pz,
                            s - 0.5, 0, u - 0.5);
                }
            }
        }

    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getMapColor(level, pos).col;
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            spawnDissolveParticles(level, pos);
        }
        SoundType soundtype = state.getSoundType();
        level.playSound(null, pos, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
    }


}
