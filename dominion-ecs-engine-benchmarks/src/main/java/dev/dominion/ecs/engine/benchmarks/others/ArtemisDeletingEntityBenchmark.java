/*
 * Copyright (c) 2021 Enrico Stara
 * This code is licensed under the MIT license. See the LICENSE file in the project root for license terms.
 */

package dev.dominion.ecs.engine.benchmarks.others;

import com.artemis.*;
import dev.dominion.ecs.engine.benchmarks.DominionBenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

public class ArtemisDeletingEntityBenchmark extends DominionBenchmark {

    World world;
    Archetype archetype1;
    Archetype archetype2;
    Archetype archetype4;
    Archetype archetype8;
    Archetype archetype16;

    int[] entities1;
    int[] entities2;
    int[] entities4;
    int[] entities8;
    int[] entities16;
    @Param(value = {"1000000"})
    int size;

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(
                new String[]{ArtemisDeletingEntityBenchmark.class.getName()}
        );
    }

    @Setup(Level.Iteration)
    public void setup() {
        WorldConfiguration worldConfiguration = new WorldConfigurationBuilder().build();
        world = new World(worldConfiguration);
        archetype1 = new ArchetypeBuilder().add(C1.class).build(world);
        archetype2 = new ArchetypeBuilder().add(C1.class).add(C2.class).build(world);
        archetype4 = new ArchetypeBuilder().add(C1.class).add(C2.class).add(C3.class).add(C4.class).build(world);
        archetype8 = new ArchetypeBuilder().add(C1.class).add(C2.class).add(C3.class).add(C4.class).add(C5.class).add(C6.class).add(C7.class).add(C8.class).build(world);
        archetype16 = new ArchetypeBuilder().add(C1.class).add(C2.class).add(C3.class).add(C4.class).add(C5.class).add(C6.class).add(C7.class).add(C8.class).add(C9.class).add(C10.class).add(C11.class).add(C12.class).add(C13.class).add(C14.class).add(C15.class).add(C16.class).build(world);
        entities1 = new int[size];
        entities2 = new int[size];
        entities4 = new int[size];
        entities8 = new int[size];
        entities16 = new int[size];
        world.process();
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        for (int i = 0; i < size; i++) {
//            entities1[i] = world.create(archetype1);
//            entities2[i] = world.create(archetype2);
//            entities4[i] = world.create(archetype4);
//            entities8[i] = world.create(archetype8);
            entities16[i] = world.create(archetype16);
        }
        world.process();
    }

    @Benchmark
    public void createEntityWith01(Blackhole bh) {
        for (int i = 0; i < size; i++) {
            world.delete(entities16[i]);
        }
        world.process();
    }

    //    @Benchmark
    public void createEntityWith02(Blackhole bh) {
        for (int i = 0; i < size; i++) {
            bh.consume(entities2[i] = world.create(archetype2));
        }
        world.process();
    }

    //    @Benchmark
    public void createEntityWith04(Blackhole bh) {
        for (int i = 0; i < size; i++) {
            bh.consume(entities4[i] = world.create(archetype4));
        }
        world.process();
    }

    //    @Benchmark
    public void createEntityWith08(Blackhole bh) {
        for (int i = 0; i < size; i++) {
            bh.consume(entities8[i] = world.create(archetype8));
        }
        world.process();
    }

    //    @Benchmark
    public void createEntityWith16(Blackhole bh) {
        for (int i = 0; i < size; i++) {
            bh.consume(entities16[i] = world.create(archetype16));
        }
        world.process();
    }


    public static class C1 extends Component {
    }

    public static class C2 extends Component {
    }

    public static class C3 extends Component {
    }

    public static class C4 extends Component {
    }

    public static class C5 extends Component {
    }

    public static class C6 extends Component {
    }

    public static class C7 extends Component {
    }

    public static class C8 extends Component {
    }

    public static class C9 extends Component {
    }

    public static class C10 extends Component {
    }

    public static class C11 extends Component {
    }

    public static class C12 extends Component {
    }

    public static class C13 extends Component {
    }

    public static class C14 extends Component {
    }

    public static class C15 extends Component {
    }

    public static class C16 extends Component {
    }
}
