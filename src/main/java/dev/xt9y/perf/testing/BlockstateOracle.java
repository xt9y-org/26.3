package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test-only oracle: enumerate expected vanilla state transitions independently. */
public final class BlockstateOracle {
    private BlockstateOracle() {}

    private static <T extends Comparable<T>> Comparable<?> value(BlockState state, Property<T> property) {
        return state.getValue(property);
    }

    private static List<Comparable<?>> key(BlockState state, List<Property<?>> properties) {
        List<Comparable<?>> result = new ArrayList<>(properties.size());
        for (Property<?> property : properties) {
            result.add(value(state, property));
        }
        return List.copyOf(result);
    }

    private static <T extends Comparable<T>> int check(
            BlockState state, Property<T> property, int propertyIndex,
            List<Property<?>> properties, Map<List<Comparable<?>>, BlockState> states) {
        int tested = 0;
        for (T replacement : property.getPossibleValues()) {
            List<Comparable<?>> target = new ArrayList<>(key(state, properties));
            target.set(propertyIndex, replacement);
            BlockState expected = states.get(target);
            BlockState actual = state.setValue(property, replacement);
            if (expected == null || actual != expected) {
                throw new AssertionError("Block state transition mismatch: " + state
                        + ", property " + property.getName() + "=" + replacement
                        + ", expected " + expected + ", got " + actual);
            }
            tested++;
        }
        return tested;
    }

    public static void verify() {
        long began = System.nanoTime();
        long transitions = 0;
        int distinctStates = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var definition = block.getStateDefinition();
            List<Property<?>> properties = List.copyOf(definition.getProperties());
            if (properties.isEmpty()) continue;
            Map<List<Comparable<?>>, BlockState> states = new HashMap<>();
            for (BlockState state : definition.getPossibleStates()) {
                states.put(key(state, properties), state);
                distinctStates++;
            }
            for (BlockState state : definition.getPossibleStates()) {
                for (int index = 0; index < properties.size(); index++) {
                    transitions += check(state, properties.get(index), index, properties, states);
                }
            }
        }
        if (transitions == 0) throw new AssertionError("Block state oracle did not execute");
        PerfMod.LOGGER.info("BlockState oracle passed: {} states, {} exact transitions, {} ms",
                distinctStates, transitions, (System.nanoTime() - began) / 1_000_000);
    }
}
