package hal.phpmetrics.idea;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.PrintWriter;
import com.intellij.openapi.diagnostic.Logger;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RunPhpMetricsAction extends AnAction {

    private Project project;
    private static final Logger LOG = Logger.getInstance("#" + RunPhpMetricsAction.class.getName());

    public void actionPerformed(AnActionEvent e) {

        project = e.getProject();
        if(project == null) {
            return;
        }

        VirtualFile currentDirectory = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());

        try {
            final File destination = File.createTempFile("phpmetrics-idea", ".html");
            String phar = getClass().getResource("/hal/phpmetrics/idea/phar/phpmetrics.phar").toURI().getPath();
LOG.info("-----------------");
LOG.info(phar);
            String[] commands = new String[] {"php", phar, "--report-html=" + destination, currentDirectory.getPath()};

            Runtime runtime = Runtime.getRuntime();
            final Process process = runtime.exec(commands);

            new Thread() {
                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line = "";
                        try {
                            while((line = reader.readLine()) != null) {
                            }
                        } finally {
                            reader.close();
                        }

                        // open browser
                        try {
                            URI uri = new URI("file://" + destination.toString());
                            BrowserUtil.browse(uri);
                        } catch(URISyntaxException uriE) {
                            uriE.printStackTrace();
                        }

                    } catch(IOException ioe) {
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
                            while((line = reader.readLine()) != null) {
                                // Traitement du flux d'erreur de l'application si besoin est
                            }
                        } finally {
                            reader.close();
                        }
                    } catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }.start();




        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e2) {
            e2.printStackTrace();
//        } catch (InterruptedException e3) {
//            e3.printStackTrace();
        }
    }
}
