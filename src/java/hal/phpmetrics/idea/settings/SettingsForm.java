package hal.phpmetrics.idea.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import hal.phpmetrics.idea.Settings;
import hal.phpmetrics.idea.runner.CliRunner;
import hal.phpmetrics.idea.runner.ResultListener;
import hal.phpmetrics.idea.runner.OnEventDispatchThread;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;

public class SettingsForm implements Configurable {

    private Project project;

    private JPanel settingsPanel;
    private TextFieldWithBrowseButton customPath;
    private JLabel includedVersion;
    private JLabel customVersion;
    private JButton useIncludedButton;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "PhpMetrics";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        loadIncludedVersion();
        loadCustomVersion();

        FileChooserDescriptor binaryDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        binaryDescriptor.setTitle("Choose PhpMetrics Installation");
        customPath.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(true, false, false, false, false, false)));
        customPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new CliRunner(customPath.getText()).run(new String[]{"--version"}, new OnEventDispatchThread(new ResultListener() {
                    @Override
                    public void onSuccess(String output) {
                        if (isValidInstallation(output)) {
                            setCustomVersion(extractVersion(output));
                            inform(customVersion, "Seems to be a valid PhpMetrics installation :-)", MessageType.INFO);
                            return;
                        }

                        unsetCustomVersion();
                        inform(customPath, "Seems to be no PhpMetrics installation :-(", MessageType.ERROR);
                    }

                    @Override
                    public void onError(String error, String output, int exitCode) {
                        unsetCustomVersion();
                        inform(customPath, "Seems to be no PhpMetrics installation :-(", MessageType.ERROR);
                    }
                }, customVersion));
            }
        });

        useIncludedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                unsetCustomVersion();
            }
        });

        return settingsPanel;
    }

    @Override
    public void disposeUIResources() {
    }

    @Override
    public boolean isModified() {
        return ! customPath.getText().equals(getSettings().pathToBinary);
    }

    @Override
    public void apply() throws ConfigurationException {
        String pathToBinary = null;
        if (customPath.getText().length() > 0) {
            pathToBinary = customPath.getText();
        }
        getSettings().pathToBinary = pathToBinary;
    }

    @Override
    public void reset() {
        if (getSettings().pathToBinary == null) {
            unsetCustomVersion();
            return;
        }
        customPath.setText(getSettings().pathToBinary);
        loadCustomVersion();
    }

    private void loadCustomVersion() {
        new CliRunner(getSettings().pathToBinary).run(new String[]{"--version"}, new OnEventDispatchThread(new ResultListener() {
            @Override
            public void onSuccess(String output) {
                if (isValidInstallation(output)) {
                    setCustomVersion(extractVersion(output));
                    return;
                }
                unsetCustomVersion();
            }

            @Override
            public void onError(String error, String output, int exitCode) {
                unsetCustomVersion();
            }
        }, customVersion));
    }

    private void loadIncludedVersion() {
        CliRunner.withIncludedPhar().run(new String[]{"--version"}, new OnEventDispatchThread(new ResultListener() {
            @Override
            public void onSuccess(String output) {
                includedVersion.setText(extractVersion(output));
            }

            @Override
            public void onError(String error, String output, int exitCode) {
            }
        }, includedVersion));
    }
    private boolean isValidInstallation(@NotNull String output) {
        return output.contains("PhpMetrics")
                && ! extractVersion(output).isEmpty();
    }

    private String extractVersion(@NotNull String versionText) {
        String version = versionText.replaceFirst("^.* version v?", "");
        if (version.equals(versionText)) {
            return "";
        }
        return version;
    }

    private void setCustomVersion(String version) {
        customVersion.setText(version);
        useIncludedButton.setEnabled(true);
    }

    private void unsetCustomVersion() {
        customPath.setText("");
        customVersion.setText("");
        useIncludedButton.setEnabled(false);
    }

    private void inform(JComponent target, String text, MessageType messageType) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, messageType, null)
                .setFadeoutTime(2000)
                .createBalloon()
                .show(RelativePoint.getNorthEastOf(target), Balloon.Position.atRight);
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }
}
