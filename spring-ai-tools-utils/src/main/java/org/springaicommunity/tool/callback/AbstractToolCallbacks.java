package org.springaicommunity.tool.callback;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

/**
 * Base class for tool-callback wrappers that need to resolve guardrail,
 * confirmation, or fallback strategy instances either from the Spring
 * {@link ApplicationContext} or via reflection.
 */
public abstract class AbstractToolCallbacks {

    /** The Spring application context used for bean resolution. */
    protected final ApplicationContext applicationContext;

    /**
     * Constructs the base handler with the given application context.
     *
     * @param applicationContext the application context used to look up strategy beans
     */
    protected AbstractToolCallbacks(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Resolves an instance of {@code clazz} by first looking it up in the
     * application context and falling back to no-arg constructor instantiation.
     *
     * @param <T>   the target type
     * @param clazz the concrete class to resolve
     * @param type  the expected interface or supertype
     * @return an instance of {@code type} backed by {@code clazz}
     * @throws IllegalStateException if the instance cannot be obtained
     */
    protected <T> T resolve(Class<? extends T> clazz, Class<T> type) {
        try {
            return type.cast(applicationContext.getBean(clazz));
        } catch (NoSuchBeanDefinitionException e) {
            try {
                return type.cast(clazz.getDeclaredConstructor().newInstance());
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Cannot instantiate " + clazz.getName() +
                                ". Declare it as a @Bean or provide a no-arg constructor.", ex);
            }
        }
    }
}
