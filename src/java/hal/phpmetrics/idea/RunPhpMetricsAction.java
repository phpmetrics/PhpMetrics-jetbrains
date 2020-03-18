package hal.phpmetrics.idea;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.io.IOException;

import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import hal.phpmetrics.idea.runner.CliRunner;
import hal.phpmetrics.idea.runner.ResultListener;
import hal.phpmetrics.idea.runner.OnEventDispatchThread;

public class RunPhpMetricsAction extends AnAction {

    public void actionPerformed(final AnActionEvent e) {

        Project project = e.getProject();
        if (project == null) {
            return;
        }

        VirtualFile currentDirectory = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (currentDirectory == null) {
            inform(e, "Please select a folder of your project before running PhpMetrics", MessageType.WARNING);
            return;
        }

        inform(e, "PhpMetrics started. Your browser will be run in few minutes....", MessageType.INFO);

        try {
            CliRunner cliRunner = new CliRunner(Settings.getInstance(project).pathToBinary);
            final File destination = File.createTempFile("phpmetrics-idea", ".html");
            String[] command = new String[]{"--report-html=" + destination, currentDirectory.getPath()};

            cliRunner.run(command, new OnEventDispatchThread(new ResultListener() {
                @Override
                public void onSuccess(String output) {
                    BrowserUtil.browse(destination);
                }

                @Override
                public void onError(String error, String output, int exitCode) {
                    inform(e, "An error occurred. Please verify your PhpMetrics installation\n" + output, MessageType.ERROR);
                }
            }));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void inform(AnActionEvent e, String text, MessageType messageType) {
        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(PlatformDataKeys.PROJECT.getData(e.getDataContext()));
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, messageType, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
    }
}
