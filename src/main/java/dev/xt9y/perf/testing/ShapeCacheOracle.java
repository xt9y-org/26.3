package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Test the real Minecraft method, including repeated hits, against its exact predicate. */
public final class ShapeCacheOracle {
    private ShapeCacheOracle() {}

    public static void verify() {
        final List<VoxelShape> shapes = new ArrayList<>(6_000);
        shapes.add(Shapes.empty());
        shapes.add(Shapes.block());
        shapes.add(Shapes.box(0, 0, 0, 1, 1, 1));
        shapes.add(Shapes.box(0, 0, 0, 0.5, 1, 1));
        shapes.add(Shapes.box(0, 0, 0, 1, 0.5, 1));
        shapes.add(Shapes.or(Shapes.box(0, 0, 0, 0.5, 1, 1),
                             Shapes.box(0.5, 0, 0, 1, 1, 1)));

        for (int i = 1; i <= 4096; i++) {
            double x = (i % 31 + 1) / 32.0;
            double y = (i % 61 + 1) / 64.0;
            double z = (i % 127 + 1) / 128.0;
            shapes.add(Shapes.box(0, 0, 0, x, y, z));
        }

        int checked = 0;
        for (int pass = 0; pass < 4; pass++) {
            for (VoxelShape shape : shapes) {
                boolean expected = !Shapes.joinIsNotEmpty(
                        Shapes.block(), shape, BooleanOp.NOT_SAME);
                boolean actual = Block.isShapeFullBlock(shape);
                if (expected != actual) {
                    throw new AssertionError("Full-block shape predicate diverged in pass " +
                            pass + " for " + shape);
                }
                checked++;
            }
        }
        PerfMod.LOGGER.info("Shape cache oracle passed: {} exact full-block comparisons", checked);
    }
}
