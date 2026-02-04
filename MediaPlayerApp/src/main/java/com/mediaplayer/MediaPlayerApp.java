  package com.mediaplayer;

import java.io.File;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;// CORRECT PACKAGE
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * Advanced Media Player Application
 * Features: Video Playback, ML Subtitle Creation, Code Extraction
 */
public class MediaPlayerApp extends Application {
    
    private Stage primaryStage;
    private BorderPane mainLayout;
    private StackPane videoContainer;
    private VBox controlPanel;
    private TabPane sidePanel;
    
    // UI Components
    private Button playPauseButton;
    private Button stopButton;
    private Slider timeSlider;
    private Slider volumeSlider;
    private Label timeLabel;
    private Label statusLabel;
    
    // Feature panels
    private TextArea subtitleTextArea;
    private TextArea codeExtractionArea;
    private ListView<String> playlistView;
    
    //media palye
    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer vlcjPlayer;
    private ImageView videoView;


    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Advanced Media Player - ML Subtitle & Code Extraction");
        
        // Create main layout
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-layout");
        
        
        // Create UI components
        createMenuBar();
        createVideoContainer();
        createControlPanel();
        createSidePanel();
        

        

        // Assemble layout
        mainLayout.setTop(createMenuBar());
        //mainLayout.setCenter(videoContainer);
        mainLayout.setBottom(controlPanel);
        //mainLayout.setRight(sidePanel);
        StackPane videoOverlayContainer = new StackPane(videoContainer, sidePanel);
        sidePanel.setVisible(false);
        sidePanel.setManaged(false);
        // Set as center of main layout
        mainLayout.setCenter(videoOverlayContainer);
        




        // Create toggle button for overlay
        Button overlayToggleBtn = new Button("◀");
        overlayToggleBtn.getStyleClass().add("overlay-toggle"); // CSS class
        overlayToggleBtn.setMinSize(30, 60);

        // Align to right-center of video
        StackPane.setAlignment(overlayToggleBtn, Pos.CENTER_RIGHT);

        // Add to the stack
        videoOverlayContainer.getChildren().add(overlayToggleBtn);

        // Toggle action
        overlayToggleBtn.setOnAction(e -> {
            boolean visible = sidePanel.isVisible();
            sidePanel.setVisible(!visible);
            sidePanel.setManaged(!visible);
            overlayToggleBtn.setText(visible ? "◀" : "▶"); // swap arrow
        });





        // Create scene and apply CSS
        Scene scene = new Scene(mainLayout, 1400, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS file not found, using default styling");
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        updateStatus("Ready - Open a video file to start");
    }
    
    /**
     * Creates the menu bar with File, Tools, and Help menus
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open Video File...");
        MenuItem openFolder = new MenuItem("Open Folder...");
        MenuItem exit = new MenuItem("Exit");
        
        openFile.setOnAction(e -> openVideoFile());
        exit.setOnAction(e -> primaryStage.close());
        
        fileMenu.getItems().addAll(openFile, openFolder, new SeparatorMenuItem(), exit);
        
        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        MenuItem generateSubtitles = new MenuItem("Generate Subtitles (ML)");
        MenuItem extractCode = new MenuItem("Extract Code from Video");
        MenuItem settings = new MenuItem("Settings");
        
        generateSubtitles.setOnAction(e -> showSubtitleGenerationDialog());
        extractCode.setOnAction(e -> showCodeExtractionDialog());
        
        toolsMenu.getItems().addAll(generateSubtitles, extractCode, new SeparatorMenuItem(), settings);
        
        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");
        MenuItem documentation = new MenuItem("Documentation");
        
        about.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().addAll(documentation, about);
        
        menuBar.getMenus().addAll(fileMenu, toolsMenu, helpMenu);
        return menuBar;
    }
    
    /**
     * Creates the video container area
     */
    private void createVideoContainer() {
        // Create JavaFX ImageView for video
        videoView = new ImageView();
        videoView.setPreserveRatio(true);
        videoView.setFitWidth(1200);
        videoView.setFitHeight(600);

        // StackPane to hold video (and overlays later)
        videoContainer = new StackPane(videoView);
        videoContainer.setStyle("-fx-background-color: black;");
        videoContainer.setPrefHeight(600);

        // Initialize VLCJ
        mediaPlayerFactory = new MediaPlayerFactory();
        vlcjPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        vlcjPlayer.videoSurface().set(new ImageViewVideoSurface(videoView));


    }
    
    /**
     * Creates the control panel with playback controls
     */
    private void createControlPanel() {
        controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(4));
        controlPanel.getStyleClass().add("control-panel");
        
        // Time slider
        timeSlider = new Slider(0, 100, 0);
        timeSlider.setPrefWidth(Double.MAX_VALUE);
        timeSlider.setDisable(true);
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-font-family: monospace;");
        
        HBox timeBox = new HBox(10, timeSlider, timeLabel);
        timeBox.setAlignment(Pos.CENTER);
        
        // Control buttons
        playPauseButton = new Button("▶ Play");
        playPauseButton.setPrefWidth(100);
        playPauseButton.setPrefHeight(28);
        playPauseButton.setDisable(true);
        playPauseButton.setOnAction(e -> {
        if (vlcjPlayer.status().isPlaying()) {
            vlcjPlayer.controls().pause();
            playPauseButton.setText("▶ Play");
        } else {
            vlcjPlayer.controls().play();
            playPauseButton.setText("⏸ Pause");
        }
        });
        
        stopButton = new Button("⬛ Stop");
        stopButton.setPrefWidth(100);
        stopButton.setPrefHeight(28);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> {
        vlcjPlayer.controls().stop();
        playPauseButton.setText("▶ Play");
        });
        
        Button previousButton = new Button("⏮ Prev");
        Button nextButton = new Button("⏭ Next");
        
        Label volumeLabel = new Label("🔊");
        volumeSlider = new Slider(0, 100, 70);
        volumeSlider.setPrefWidth(120);
        
        HBox volumeBox = new HBox(5, volumeLabel, volumeSlider);
        volumeBox.setAlignment(Pos.CENTER);
        
        Button fullscreenButton = new Button("⛶ Fullscreen");
        Button snapshotButton = new Button("📷 Snapshot");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(
            previousButton, playPauseButton, stopButton, nextButton,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            volumeBox,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            snapshotButton, fullscreenButton
        );
        
        // Status bar
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        controlPanel.getChildren().addAll(timeBox, buttonBox, statusLabel);
    }
    
    /**
     * Creates the side panel with tabs for features
     */
    private void createSidePanel() {
        sidePanel = new TabPane();
        sidePanel.setPrefWidth(350);
        sidePanel.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Playlist Tab
        Tab playlistTab = new Tab("📋 Playlist");
        playlistView = new ListView<>();
        playlistView.setPlaceholder(new Label("No videos in playlist"));
        playlistTab.setContent(playlistView);
        
        // Subtitles Tab
        Tab subtitlesTab = new Tab("💬 Subtitles");
        VBox subtitleBox = new VBox(10);
        subtitleBox.setPadding(new Insets(10));
        
        Label subtitleLabel = new Label("Generated Subtitles (ML)");
        subtitleLabel.setStyle("-fx-font-weight: bold;");
        
        subtitleTextArea = new TextArea();
        subtitleTextArea.setPromptText("Subtitles will appear here after ML generation...");
        subtitleTextArea.setWrapText(true);
        VBox.setVgrow(subtitleTextArea, Priority.ALWAYS);
        
        Button generateSubBtn = new Button("🤖 Generate Subtitles");
        Button exportSubBtn = new Button("💾 Export SRT");
        exportSubBtn.setDisable(true);
        
        HBox subtitleButtons = new HBox(10, generateSubBtn, exportSubBtn);
        
        subtitleBox.getChildren().addAll(subtitleLabel, subtitleTextArea, subtitleButtons);
        subtitlesTab.setContent(subtitleBox);
        
        // Code Extraction Tab
        Tab codeTab = new Tab("</> Code Extract");
        VBox codeBox = new VBox(10);
        codeBox.setPadding(new Insets(10));
        
        Label codeLabel = new Label("Extracted Code (OCR)");
        codeLabel.setStyle("-fx-font-weight: bold;");
        
        codeExtractionArea = new TextArea();
        codeExtractionArea.setPromptText("Extracted code will appear here...");
        codeExtractionArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace;");
        VBox.setVgrow(codeExtractionArea, Priority.ALWAYS);
        
        Button extractCodeBtn = new Button("🔍 Extract Code");
        Button exportCodeBtn = new Button("💾 Export Code");
        exportCodeBtn.setDisable(true);
        
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("Auto Detect", "Java", "Python", "JavaScript", "C++", "C#");
        languageCombo.setValue("Auto Detect");
        
        HBox codeButtons = new HBox(10, extractCodeBtn, exportCodeBtn, languageCombo);
        
        codeBox.getChildren().addAll(codeLabel, codeExtractionArea, codeButtons);
        codeTab.setContent(codeBox);
        
        // Info Tab
        Tab infoTab = new Tab("ℹ Info");
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(10));
        
        Label infoTitle = new Label("Video Information");
        infoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TextArea infoArea = new TextArea();
        infoArea.setEditable(false);
        infoArea.setText("File: Not loaded\nDuration: --\nResolution: --\nCodec: --\nFrame Rate: --");
        VBox.setVgrow(infoArea, Priority.ALWAYS);
        
        infoBox.getChildren().addAll(infoTitle, infoArea);
        infoTab.setContent(infoBox);
        
        sidePanel.getTabs().addAll(playlistTab, subtitlesTab, codeTab, infoTab);
    }
    
    /**
     * Opens a video file
     */
    private void openVideoFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Video File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.flv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            loadVideo(file);
        }
    }
    
    /**
     * Loads a video file (placeholder for VLCJ integration)
     */
    private void loadVideo(File file) {
    try {
        String mediaPath = file.getAbsolutePath();
        vlcjPlayer.media().startPaused(mediaPath); // start paused
        playPauseButton.setDisable(false);
        stopButton.setDisable(false);
        timeSlider.setDisable(false);

        playlistView.getItems().add(file.getName());
        updateStatus("Loaded: " + file.getName());
        } catch (Exception e) {
            System.out.println("Error loading video: " + e.getMessage());
        }
    }
    
    /**
     * Shows subtitle generation dialog
     */
    private void showSubtitleGenerationDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Generate Subtitles");
        alert.setHeaderText("ML Subtitle Generation");
        alert.setContentText("This feature will use machine learning (Whisper model) to generate subtitles.\n\nImplementation coming in next phase!");
        alert.showAndWait();
    }
    
    /**
     * Shows code extraction dialog
     */
    private void showCodeExtractionDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Extract Code");
        alert.setHeaderText("Code Extraction (OCR)");
        alert.setContentText("This feature will use OCR to extract code from video frames.\n\nImplementation coming in next phase!");
        alert.showAndWait();
    }
    
    /**
     * Shows about dialog
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Advanced Media Player v1.0");
        alert.setContentText(
            "Features:\n" +
            "• Video Playback (VLCJ)\n" +
            "• ML-powered Subtitle Generation\n" +
            "• Code Extraction from Videos (OCR)\n\n" +
            "Built with JavaFX 21"
        );
        alert.showAndWait();
    }
    
    /**
     * Updates the status label
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    @Override
    public void stop() {
        if (vlcjPlayer != null) {
            vlcjPlayer.release();
        }
        if (mediaPlayerFactory != null) {
            mediaPlayerFactory.release();
        }
    }

    
    public static void main(String[] args) {
        launch(args);
    }
}