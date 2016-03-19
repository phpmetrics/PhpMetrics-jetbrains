package hal.phpmetrics.idea.runner;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Decorator that allows to to run the listener from the event dispatch thread.
 *
 * It is just a convenience class.
 * See http://www.jetbrains.org/intellij/sdk/docs/basics/architectural_overview/general_threading_rules.html for more
 * information.
 */
public class OnEventDispatchThread implements ResultListener {
    private Component modifiedComponent;
    private ResultListener decorated;

    public OnEventDispatchThread(@NotNull ResultListener decorated, @NotNull Component target) {
        this.modifiedComponent = target;
        this.decorated = decorated;
    }

    public OnEventDispatchThread(@NotNull ResultListener decorated) {
        this.decorated = decorated;
    }

    @Override
    public void onSuccess(final String output) {
        final Application application = ApplicationManager.getApplication();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                decorated.onSuccess(output);
            }
        }, modalityStateFor(modifiedComponent));
    }

    @Override
    public void onError(final String error, final String output, final int exitCode) {
        final Application application = ApplicationManager.getApplication();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                decorated.onError(error, output, exitCode);
            }
        }, modalityStateFor(modifiedComponent));
    }

    private ModalityState modalityStateFor(Component component) {
        return component != null
                ? ModalityState.stateForComponent(component)
                : ModalityState.any();
    }
}
