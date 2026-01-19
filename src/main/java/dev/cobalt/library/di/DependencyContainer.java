package dev.cobalt.library.di;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Dependency Injection Container (IoC)
 * Supports singleton and transient registrations
 */
public class DependencyContainer {

    private final Plugin plugin;
    private final Map<Class<?>, Supplier<?>> singletons = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> transients = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    public DependencyContainer(Plugin plugin) {
        this.plugin = plugin;

        // Register self
        singleton(Plugin.class, () -> plugin);
        singleton(DependencyContainer.class, () -> this);
    }

    /**
     * Register a singleton (created once, reused)
     */
    public <T> void singleton(Class<T> type, Supplier<T> factory) {
        singletons.put(type, factory);
    }

    /**
     * Register a transient (created every time)
     */
    public <T> void tran(Class<T> type, Supplier<T> factory) {
        transients.put(type, factory);
    }

    /**
     * Register a singleton by class (auto-instantiate)
     */
    public <T> void singleton(Class<T> type) {
        singleton(type, () -> createInstance(type));
    }

    /**
     * Register a transient by class (auto-instantiate)
     */
    public <T> void tran(Class<T> type) {
        tran(type, () -> createInstance(type));
    }

    /**
     * Resolve a dependency
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        // Check if already instantiated singleton
        if (instances.containsKey(type)) {
            return (T) instances.get(type);
        }

        // Check singleton registration
        if (singletons.containsKey(type)) {
            T instance = (T) singletons.get(type).get();
            instances.put(type, instance);
            return instance;
        }

        // Check transient registration
        if (transients.containsKey(type)) {
            return (T) transients.get(type).get();
        }

        // Try auto-instantiate
        return createInstance(type);
    }

    /**
     * Check if a type is registered
     */
    public boolean isRegistered(Class<?> type) {
        return singletons.containsKey(type) || transients.containsKey(type);
    }

    /**
     * Create instance with constructor injection
     */
    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> type) {
        try {
            // Find constructor
            Constructor<?>[] constructors = type.getConstructors();
            if (constructors.length == 0) {
                constructors = type.getDeclaredConstructors();
            }

            if (constructors.length == 0) {
                throw new RuntimeException("No constructors found for " + type.getName());
            }

            // Use first constructor
            Constructor<?> constructor = constructors[0];
            constructor.setAccessible(true);

            // Resolve parameters
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = resolve(paramTypes[i]);
            }

            return (T) constructor.newInstance(params);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + type.getName(), e);
        }
    }

    /**
     * Inject dependencies into an existing object
     */
    public void inject(Object target) {
        // TODO: Implement field/setter injection
        // For now, only constructor injection is supported
    }

    /**
     * Clear all registrations
     */
    public void clear() {
        instances.clear();
        singletons.clear();
        transients.clear();
    }

    /**
     * Get all registered types
     */
    public Set<Class<?>> getRegisteredTypes() {
        Set<Class<?>> types = new java.util.HashSet<>();
        types.addAll(singletons.keySet());
        types.addAll(transients.keySet());
        return types;
    }
}
