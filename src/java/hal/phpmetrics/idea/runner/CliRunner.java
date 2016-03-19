package hal.phpmetrics.idea.runner;

import java.io.*;
import java.net.URL;

public class CliRunner {

    private String binPath;

    private static String resultFromStream(InputStream stream) {
        try {
            String processResult = "";
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                processResult += line;
            }
            reader.close();
            return processResult;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return "";
    }


    public static CliRunner withIncludedPhar() {
        return new CliRunner(null);
    }

    public CliRunner(String binPath) {
        this.binPath = binPath;
    }

    public void run(String[] arguments, final ResultListener listener) {
        final String[] command = new String[arguments.length + 2];
        command[0] = "php";
        command[1] = binPath != null ? binPath : getExternalPath("/phar/phpmetrics.phar");
        System.arraycopy(arguments, 0, command, 2, arguments.length);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Process process = Runtime.getRuntime().exec(command);
                    process.waitFor();

                    if (process.exitValue() == 0) {
                        listener.onSuccess(resultFromStream(process.getInputStream()));
                        return;
                    }

                    listener.onError(resultFromStream(process.getErrorStream()), resultFromStream(process.getInputStream()), process.exitValue());
                } catch (IOException exception) {
                    exception.printStackTrace();
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }.start();
    }

    private String getExternalPath(String resource) throws NullPointerException {
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
            file = new File(res.getFile().replace("%20", "\\ "));

            if (!file.exists()) {
                file = new File(res.getFile().replace("%20", " "));
            }
        }

        if (file != null && ! file.exists()) {
            throw new RuntimeException("Error: File " + file + " not found!");
        }
        if (file == null) {
            throw new RuntimeException("Error: Could not found the phpmetrics bin file");
        }

        return file.getAbsolutePath();
    }
}
