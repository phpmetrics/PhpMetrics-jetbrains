package hal.phpmetrics.idea;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

public class RunPhpMetricsAction extends AnAction {

    private Project project;

    public void actionPerformed(AnActionEvent e) {

        project = e.getProject();
        if(project == null) {
            return;
        }

        inform(e, "PhpMetrics started. Your browser will be run in few minutes....", MessageType.INFO);

        VirtualFile currentDirectory = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        try {
            final File destination = File.createTempFile("phpmetrics-idea", ".html");

            File phar = getExternalPath("/phar/phpmetrics.phar");

            String[] commands = new String[]{"php", phar.toURI().getPath(), "--report-html=" + destination, currentDirectory.getPath()};


            Runtime runtime = Runtime.getRuntime();
            final Process process = runtime.exec(commands);

            new Thread() {
                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line = "";
                        try {
                            while ((line = reader.readLine()) != null) {
                            }
                        } finally {
                            reader.close();
                        }

                        // open browser
                        try {
                            URI uri = new URI("file://" + destination.toString());
                            BrowserUtil.browse(uri);
                        } catch (URISyntaxException uriE) {
                            uriE.printStackTrace();
                        }

                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }.start();

            new Thread() {
                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        String line = "";
                        try {
                            while ((line = reader.readLine()) != null) {
                            }
                        } finally {
                            reader.close();
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }.start();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void inform(AnActionEvent e, String text, MessageType messageType) {
        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(PlatformDataKeys.PROJECT.getData(e.getDataContext()));
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, messageType, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
    }



    public File getExternalPath(String resource) {
        File file = null;
        URL res = getClass().getResource(resource);
        if (res.toString().startsWith("jar:") ||res.toString().contains(".jar!")) {
            try {
                InputStream input = getClass().getResourceAsStream(resource);
                file = File.createTempFile("tempfile", ".tmp");
                OutputStream out = new FileOutputStream(file);
                int read;
                byte[] bytes = new byte[1024];

                while ((read = input.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                file.deleteOnExit();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            file = new File(res.getFile().toString().replace("%20", "\\ "));
        }

        if (file != null && !file.exists()) {
            throw new RuntimeException("Error: File " + file + " not found!");
        }

        return file;
    }
}
