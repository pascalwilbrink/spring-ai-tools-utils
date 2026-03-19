package org.springaicommunity.tool.callback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractToolCallbacksTest {

    @Mock ApplicationContext applicationContext;

    // Concrete subclass to make AbstractToolCallbacks testable
    static class TestToolCallbacks extends AbstractToolCallbacks {
        TestToolCallbacks(ApplicationContext ctx) { super(ctx); }

        <T> T resolvePublic(Class<? extends T> clazz, Class<T> type) {
            return resolve(clazz, type);
        }
    }

    interface SomeService {}

    static class ConcreteService implements SomeService {}

    static class NoArgService implements SomeService {}

    static class NoNoArgService implements SomeService {
        NoNoArgService(String required) {}
    }

    @Test
    void resolve_returnsBean_whenPresentInContext() {
        ConcreteService bean = new ConcreteService();
        when(applicationContext.getBean(ConcreteService.class)).thenReturn(bean);

        TestToolCallbacks callbacks = new TestToolCallbacks(applicationContext);
        SomeService result = callbacks.resolvePublic(ConcreteService.class, SomeService.class);

        assertThat(result).isSameAs(bean);
    }

    @Test
    void resolve_instantiatesViaReflection_whenBeanNotFound() {
        when(applicationContext.getBean(NoArgService.class))
            .thenThrow(new NoSuchBeanDefinitionException(NoArgService.class));

        TestToolCallbacks callbacks = new TestToolCallbacks(applicationContext);
        SomeService result = callbacks.resolvePublic(NoArgService.class, SomeService.class);

        assertThat(result).isInstanceOf(NoArgService.class);
    }

    @Test
    void resolve_throwsIllegalStateException_whenBeanMissingAndNoNoArgConstructor() {
        when(applicationContext.getBean(NoNoArgService.class))
            .thenThrow(new NoSuchBeanDefinitionException(NoNoArgService.class));

        TestToolCallbacks callbacks = new TestToolCallbacks(applicationContext);

        assertThatThrownBy(() -> callbacks.resolvePublic(NoNoArgService.class, SomeService.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(NoNoArgService.class.getName());
    }

}