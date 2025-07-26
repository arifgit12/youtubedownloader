package com.example.mailforgood.test;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.regex.*;
import java.util.Scanner;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * YouTube Video Downloader
 *
 * IMPORTANT LEGAL NOTICE:
 * This application is for educational purposes only.
 * Only download videos you have permission to download.
 * Respect YouTube's Terms of Service and copyright laws.
 * Use responsibly and legally.
 */
public class YoutubeDownloader {

    private static final String YT_DLP_PATH = "C:\\Users\\arifa\\Downloads\\yt-dlp_x86.exe"; // Requires yt-dlp to be installed
    private JFrame frame;
    private JTextField urlField;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton downloadButton;
    private JComboBox<String> qualityCombo;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Skip look and feel setting if having issues
            // Uncomment the try-catch block below if you want to try setting system look and feel
            /*
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (ClassNotFoundException | InstantiationException |
                     IllegalAccessException | UnsupportedLookAndFeelException e) {
                System.err.println("Could not set system look and feel: " + e.getMessage());
            }
            */
            new YoutubeDownloader().createGUI();
        });
    }

    private void createGUI() {
        frame = new JFrame("YouTube Video Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create components
        createTopPanel();
        createCenterPanel();
        createBottomPanel();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Check if yt-dlp is available
        checkYtDlpAvailability();
    }

    private void createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel urlLabel = new JLabel("YouTube URL:");
        urlField = new JTextField(50);
        urlField.setToolTipText("Enter YouTube video URL here");

        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.add(urlLabel, BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);

        // Quality selection
        JLabel qualityLabel = new JLabel("Quality:");
        qualityCombo = new JComboBox<>(new String[]{
                "Best", "720p", "480p", "360p", "Audio Only (MP3)"
        });

        JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        qualityPanel.add(qualityLabel);
        qualityPanel.add(qualityCombo);

        downloadButton = new JButton("Download");
        downloadButton.addActionListener(this::downloadVideo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(downloadButton);

        topPanel.add(urlPanel, BorderLayout.NORTH);
        topPanel.add(qualityPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);
    }

    private void createCenterPanel() {
        logArea = new JTextArea(15, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Download Log"));

        frame.add(scrollPane, BorderLayout.CENTER);
    }

    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");

        bottomPanel.add(progressBar, BorderLayout.CENTER);

        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void checkYtDlpAvailability() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Process process = Runtime.getRuntime().exec(YT_DLP_PATH + " --version");
                process.waitFor();

                SwingUtilities.invokeLater(() -> {
                    if (process.exitValue() == 0) {
                        logMessage("✓ yt-dlp is available and ready to use.");
                        logMessage("Ready to download videos.");
                    } else {
                        logMessage("✗ yt-dlp not found. Please install yt-dlp first.");
                        logMessage("Visit: https://github.com/yt-dlp/yt-dlp");
                        downloadButton.setEnabled(false);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    logMessage("✗ yt-dlp not found: " + e.getMessage());
                    logMessage("Please install yt-dlp to use this application.");
                    logMessage("Visit: https://github.com/yt-dlp/yt-dlp");
                    downloadButton.setEnabled(false);
                });
            }
        });
        executor.shutdown();
    }

    private void downloadVideo(ActionEvent e) {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a YouTube URL",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isValidYouTubeUrl(url)) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid YouTube URL",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable download button during download
        downloadButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Downloading...");

        // Clear log
        logArea.setText("");

        // Start download in background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> performDownload(url));
        executor.shutdown();
    }

    private boolean isValidYouTubeUrl(String url) {
        String pattern = "^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]{11}.*";
        return Pattern.matches(pattern, url);
    }

    private void performDownload(String url) {
        try {
            // Create downloads directory
            Path downloadsDir = Paths.get("downloads");
            if (!Files.exists(downloadsDir)) {
                Files.createDirectory(downloadsDir);
            }

            // Build command based on quality selection
            String quality = (String) qualityCombo.getSelectedItem();
            String[] command = buildDownloadCommand(url, quality, downloadsDir.toString());

            logMessage("Starting download...");
            logMessage("URL: " + url);
            logMessage("Quality: " + quality);
            logMessage("Output directory: " + downloadsDir.toAbsolutePath());
            logMessage("Command: " + String.join(" ", command));
            logMessage("----------------------------------------");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("."));
            Process process = pb.start();

            // Read output in real-time
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Read stdout
            Thread outputThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String logLine = line;
                        SwingUtilities.invokeLater(() -> logMessage(logLine));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> logMessage("Error reading output: " + e.getMessage()));
                }
            });

            // Read stderr
            Thread errorThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        final String logLine = line;
                        SwingUtilities.invokeLater(() -> logMessage("ERROR: " + logLine));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> logMessage("Error reading error stream: " + e.getMessage()));
                }
            });

            outputThread.start();
            errorThread.start();

            int exitCode = process.waitFor();

            outputThread.join();
            errorThread.join();

            SwingUtilities.invokeLater(() -> {
                if (exitCode == 0) {
                    logMessage("----------------------------------------");
                    logMessage("✓ Download completed successfully!");
                    progressBar.setString("Download completed");
                    JOptionPane.showMessageDialog(frame, "Download completed successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    logMessage("----------------------------------------");
                    logMessage("✗ Download failed with exit code: " + exitCode);
                    progressBar.setString("Download failed");
                    JOptionPane.showMessageDialog(frame, "Download failed. Check the log for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }

                // Re-enable download button
                downloadButton.setEnabled(true);
                progressBar.setIndeterminate(false);
                if (exitCode != 0) {
                    progressBar.setString("Ready");
                }
            });

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                logMessage("Exception occurred: " + e.getMessage());
                downloadButton.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setString("Error");
                JOptionPane.showMessageDialog(frame, "An error occurred: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private String[] buildDownloadCommand(String url, String quality, String outputDir) {
        switch (quality) {
            case "720p":
                return new String[]{YT_DLP_PATH, "-f", "best[height<=720]", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
            case "480p":
                return new String[]{YT_DLP_PATH, "-f", "best[height<=480]", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
            case "360p":
                return new String[]{YT_DLP_PATH, "-f", "best[height<=360]", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
            case "Audio Only (MP3)":
                return new String[]{YT_DLP_PATH, "-f", "bestaudio", "--extract-audio",
                        "--audio-format", "mp3", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
            default: // "Best"
                return new String[]{YT_DLP_PATH, "-f", "best", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
        }
    }

    private void logMessage(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}

// Alternative command-line version
class YoutubeDownloaderCLI {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================");
        System.out.println("YouTube Video Downloader (CLI)");
        System.out.println("=================================");
        System.out.println("LEGAL NOTICE: Only download videos you have permission to download.");
        System.out.println("Respect YouTube's Terms of Service and copyright laws.");
        System.out.println();

        while (true) {
            System.out.print("Enter YouTube URL (or 'quit' to exit): ");
            String url = scanner.nextLine().trim();

            if (url.equalsIgnoreCase("quit")) {
                break;
            }

            if (url.isEmpty()) {
                System.out.println("Please enter a valid URL.");
                continue;
            }

            System.out.print("Select quality (1=Best, 2=720p, 3=480p, 4=360p, 5=Audio only): ");
            String qualityChoice = scanner.nextLine().trim();

            try {
                downloadVideoCLI(url, qualityChoice);
            } catch (Exception e) {
                System.err.println("Error downloading video: " + e.getMessage());
            }

            System.out.println();
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    private static void downloadVideoCLI(String url, String qualityChoice) throws Exception {
        String[] command;
        String outputDir = "downloads";

        // Create downloads directory
        new File(outputDir).mkdirs();

        switch (qualityChoice) {
            case "2":
                command = new String[]{"yt-dlp", "-f", "best[height<=720]", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
                break;
            case "3":
                command = new String[]{"yt-dlp", "-f", "best[height<=480]", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
                break;
            case "4":
                command = new String[]{"yt-dlp", "-f", "best[height<=360]", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
                break;
            case "5":
                command = new String[]{"yt-dlp", "-f", "bestaudio", "--extract-audio",
                        "--audio-format", "mp3", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
                break;
            default:
                command = new String[]{"yt-dlp", "-f", "best", "-o",
                        outputDir + "/%(title)s.%(ext)s", url};
                break;
        }

        System.out.println("Starting download...");
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        // Show output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Download completed successfully!");
        } else {
            System.out.println("Download failed with exit code: " + exitCode);
        }
    }
}