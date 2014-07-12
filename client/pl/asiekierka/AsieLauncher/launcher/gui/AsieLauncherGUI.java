package pl.asiekierka.AsieLauncher.launcher.gui;

import com.camick.BackgroundPanel;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.AsieLauncher;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class AsieLauncherGUI extends JFrame implements IProgressUpdater
{
    private static final long serialVersionUID = 550781190397000747L;
    public boolean isRunning;
    protected AsieLauncher launcher;
    private JPanel panel;
    private JButton quitButton, launchButton, optionsButton;
    private JLabel statusLabel, loginLabel, passwordLabel;
    private JTextField loginField;
    private JPasswordField passwordField;
    private JProgressBar progressBar;
    private Image background;
    private AsieLauncherOptionsGUI options;
    public boolean hasInternet = true;
    private boolean controlDown = false;

    public boolean canKeepPassword()
    {
        return launcher.canKeepPassword();
    }

    private boolean hasFile(String fn)
    {
        return getClass().getResource(fn) != null;
    }

    private void setControl(boolean c)
    {
        controlDown = c;
        if (!controlDown && hasInternet)
        {
            launchButton.setText(Strings.LAUNCH_UPDATE);
        } else
        {
            launchButton.setText(Strings.LAUNCH_ONLY);
        }
    }

    public AsieLauncherGUI()
    {
        launcher = new AsieLauncher();
        launcher.setUpdater((IProgressUpdater) this);
        isRunning = true;
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            public void windowClosed(WindowEvent ev)
            {
                isRunning = false;
            }
        });
        boolean has2x = hasFile("/resources/background@2x.png") || hasFile("/resources/background@2x.jpg");
        boolean useJPG = hasFile("/resources/background@2x.jpg") || hasFile("/resources/background.jpg");
        boolean usePNG = hasFile("/resources/background@2x.png") || hasFile("/resources/background.png");
        if (useJPG || usePNG)
        {
            if (!has2x || Utils.getScaleFactor() <= 1.0)
            {
                background = getToolkit().getImage(getClass().getResource("/resources/background."
                        + (useJPG ? "jpg" : "png")));
            } else
            {
                background = getToolkit().getImage(getClass().getResource("/resources/background@2x."
                        + (useJPG ? "jpg" : "png")));
            }
            panel = new BackgroundPanel(background);
            ((BackgroundPanel) panel).setTransparentAdd(false);
        } else
        {
            panel = new JPanel();
        }
        getContentPane().setSize(320, 240);
        getContentPane().setPreferredSize(new Dimension(320, 240));
        getContentPane().setMaximumSize(new Dimension(320, 240));
        getContentPane().setMinimumSize(new Dimension(320, 240));
        getContentPane().add(panel);
        panel.setLayout(null);
        pack();
    }

    public boolean getLaunchedMinecraft()
    {
        return !launcher.isActive();
    }

    public boolean isActive()
    {
        return launcher.isActive();
    }

    public boolean validateLaunch()
    {
        return (loginField.getText().length() > 1);
    }

    public void update(int progress, int finish)
    {
        progressBar.setMaximum(finish);
        progressBar.setValue(progress);
        setStatus(cStatus);
    }

    private String cStatus;

    public void setStatus(String status)
    {
        cStatus = status;
        int statusProgress = (int) Math.round(progressBar.getValue() * 100.0 / progressBar.getMaximum());
        statusLabel.setText("[" + statusProgress + "%] " + status);
    }

    public void beginInstallation()
    {
        if (validateLaunch())
        {
            quitButton.setEnabled(false);
            launchButton.setEnabled(false);
            loginLabel.setText(Strings.PROGRESS + ":");
            loginLabel.setBounds(10, 162, 70, 15);
            panel.remove(loginField);
            String password = "";
            if (passwordField != null)
            {
                password = new String(passwordField.getPassword());
                panel.remove(passwordLabel);
                panel.remove(passwordField);
            }
            if (options.loginCheckbox.isSelected())
            {
                launcher.setKeepPassword(true);
            }
            Utils.saveStringToFile(launcher.directory + "nickname.txt", loginField.getText());
            if (hasInternet)
            {
                options.saveSelectedOptions();
            }
            panel.add(progressBar);
            statusLabel.setText(Strings.START_UPDATE);
            repaint();
            LauncherThread thread = new LauncherThread(AsieLauncherGUI.this, launcher, options, loginField.getText(), password, hasInternet && !controlDown);
            thread.start();
        } else
        {
            statusLabel.setText(Strings.INVALID_LOGIN);
            repaint();
        }
    }

    public void initGUILogin()
    {
        quitButton = new JButton(Strings.QUIT);
        quitButton.setBounds(245, 189, 65, 25);
        quitButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                setVisible(false);
                isRunning = false;
            }
        });

        optionsButton = new JButton(Strings.OPTIONS);
        optionsButton.setBounds(8, 189, 76, 25);
        optionsButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                options.setVisible(true);
                options.repaint();
            }
        });

        this.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent event)
            {
            }

            @Override
            public void keyPressed(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_CONTROL)
                {
                    setControl(true);
                } 
            }

            @Override
            public void keyReleased(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_CONTROL)
                {
                    setControl(false);
                }
            }
        });

        launchButton = new JButton(Strings.LAUNCH_UPDATE);
        if (!hasInternet)
        {
            launchButton.setText(Strings.LAUNCH_ONLY);
        }
        launchButton.setBounds(90, 189, 149, 25);
        launchButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                beginInstallation();
            }
        });
        
        loginField.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    System.out.println("'ENTER' key was pressed");
                }
            }

            @Override
            public void keyReleased(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    System.out.println("'ENTER' key was released");
                }
            }

            @Override
            public void keyTyped(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    System.out.println("'ENTER' key was typed");
                }
            }

        });

        statusLabel = new JLabel(Strings.READY + " (" + Strings.VERSION + ": " + AsieLauncher.VERSION_STRING + ")");
        statusLabel.setBounds(6, 219, 300, 15);

        reinstateLoginBox();

        progressBar = new JProgressBar();
        progressBar.setBounds(86, 160, 224, 20);

        panel.add(launchButton);
        panel.add(optionsButton);
        panel.add(quitButton);
        panel.add(statusLabel);
        repaint();
    }

    public String[] generateLogs()
    {
        launcher.install(options.options, options.oldOptions, true);
        return launcher.getInstallLog();
    }

    public boolean init()
    {
        if (!launcher.init())
        {
            hasInternet = false;
        }
        setTitle("AsieLauncher - " + launcher.WINDOW_NAME);
        options = new AsieLauncherOptionsGUI(this, launcher.getOptionMap(), launcher.directory + "also-options.txt");
        options.setDefaultArgs(launcher.defaultJvmArgs);
        setVisible(true);
        if (!launcher.isSupported())
        {
            JOptionPane.showMessageDialog(this, Strings.WRONG_MINECRAFT_VERSION);
        }
        if (options.loginCheckbox.isSelected())
        {
            launcher.setKeepPassword(true);
        }
        initGUILogin();
        if (!launcher.compatibleClientRevision())
        {
            JOptionPane.showMessageDialog(this, Strings.WRONG_CLIENT_REVISION);
        }
        return true;
    }

    public void reinstateLoginBox()
    {
        loginLabel = new JLabel(Strings.LOGIN + ":");
        loginField = new JTextField();
        loginField.setText(Utils.loadStringFromFile(launcher.directory + "nickname.txt"));
        loginField.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {

            }
        });

        if (launcher.askForPassword())
        {
            passwordLabel = new JLabel(Strings.PASSWORD + ":");
            passwordField = new JPasswordField();
            passwordLabel.setBounds(10, 160, 70, 15);
            passwordField.setBounds(80, 156, 231, 24);
            loginLabel.setBounds(10, 134, 50, 15);
            loginField.setBounds(80, 130, 231, 24);
            panel.add(passwordLabel);
            panel.add(passwordField);
        } else
        {
            loginLabel.setBounds(10, 160, 50, 15);
            loginField.setBounds(60, 156, 251, 24);
        }

        panel.add(loginLabel);
        panel.add(loginField);
        this.getRootPane().setDefaultButton(launchButton); //Pressing Enter will press the Launch Button
        if (progressBar != null)
        {
            panel.remove(progressBar);
        }
        quitButton.setEnabled(true);
        launchButton.setEnabled(true);
        repaint();
    }
}
