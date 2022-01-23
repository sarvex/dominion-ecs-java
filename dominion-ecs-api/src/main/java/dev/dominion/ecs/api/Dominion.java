package dev.dominion.ecs.api;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;

public interface Dominion extends AutoCloseable {

    static Dominion init() {
        return init("dev.dominion.ecs.engine");
    }

    static Dominion init(String implementation) {
        return ServiceLoader
                .load(Dominion.class)
                .stream()
                .filter(p -> p.get().getClass().getName().contains(implementation))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unable to load " + implementation))
                .get();
    }

    Entity createEntity(Object... components);

    Entity createEntityAs(Entity prefab, Object... components);

    boolean destroyEntity(Entity entity);

    <T> Results<Results.Comp1<T>> findComponents(Class<T> type);

    <T1, T2> Results<Results.Comp2<T1, T2>> findComponents(Class<T1> type1, Class<T2> type2);
}
