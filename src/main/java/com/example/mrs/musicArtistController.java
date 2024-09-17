package com.example.mrs;

import com.example.mrs.dataModel.ArtistData;
import com.example.mrs.dataModel.SongData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class musicArtistController implements Initializable {

    @FXML
    private Label artist1;

    @FXML
    private ImageView track_img;

    @FXML
    private HBox artist_section;

    @FXML
    private AnchorPane main;

    @FXML
    private Button navigate_btn;

    @FXML
    private Button play_btn;

    @FXML
    private VBox recommend_section;

    @FXML
    private Label track_artist;

    @FXML
    private Label track_name;

    public double getX(){
        return art_form.getLayoutX();
    }

    public double getY(){
        return art_form.getLayoutY();
    }

    public void insertIntoDB(ArtistData artist) throws SQLException {
        String name = artist.getName().replace("'","\\'");
        name = name.replace("\"","\\\"");

        String sql = "DELETE FROM searchdata WHERE user = '" + userID + "' " +
                "AND artist_id = '" + artist.getId() + "' AND type = 'artist'";

        connect = Database.connectDB();
        assert connect != null;

        prepare = connect.prepareStatement(sql);
        prepare.executeUpdate();

        sql = "INSERT INTO searchdata (name, cover_url, external_url, user, type, artist_id, year_follower) " +
                "VALUES (?,?,?,?,'artist',?,?)";
        prepare = connect.prepareStatement(sql);
        prepare.setString(1, name);
        prepare.setString(2, artist.getCover_url());
        prepare.setString(3, artist.getExternal_url());
        prepare.setString(4, String.valueOf(userID));
        //prepare.setString(5, "'artist'");
        prepare.setString(5, artist.getId());
        prepare.setString(6, String.valueOf(artist.getFollower()));

        prepare.executeUpdate();
    }

    Connection connect;

    PreparedStatement prepare;

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public void Init(ArtistData artist){
        try{
            insertIntoDB(artist);
        }catch (Exception e){
            e.printStackTrace(System.out);
        }

        musicSup.hide();
        art_form.setLayoutX(musicSup.getX());
        art_form.setLayoutY(musicSup.getY());

        track_img.setImage(new Image(artist.getCover_url()));
        track_name.setText(artist.getName());
        track_artist.setText(String.valueOf(artist.getFollower()));

        track_name.setTooltip(new Tooltip(artist.getName()));
        setPopularSection(artist);
    }

    private List<ArtistData> artistList = new ArrayList<>();

    private final String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";

    public void runFindSong(ArtistData vector) throws InterruptedException, IOException, ParseException {
        if (vector == null) return;
        StringBuilder cmd = new StringBuilder("\"");
        cmd.append(vector.getId());
        cmd.append("\"");

        //System.out.println(cmd);
        String pythonScript = "getArtistData.py";
        String argument = cmd.toString();

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);
        command.add(argument);
        command.add("id");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));

        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());

        Process process = processBuilder.start();

        //List<List<SongData>> songList = new ArrayList<>();

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread outputThread = new Thread(() -> {
            try {
                String line;
                int i = 0, count = 0;
                String name = "", cover_url = "", id = "", external_url = "", follower;
                String year = "", artist = "";
                artistList.clear();
                recommendList.clear();
                while ((line = outputReader.readLine()) != null) {
                    // Process the output as needed
                    if (!line.equals("!")) {
                        if (count == 0) {
                            switch (i % 5) {
                                case 0 -> {
                                    name = line;
//                                    System.out.println("name: " + line);
                                }
                                case 1 -> {
                                    cover_url = line;
//                                    System.out.println("cover_url:" + line);
                                }
                                case 2 -> {
                                    id = line;
//                                    System.out.println("id: " + id);
                                }
                                case 3 -> {
//                                    System.out.println("url:" + line);
                                    external_url = line;
                                }
                                case 4 -> {
                                    follower = line;
//                                    System.out.println("follower: " + follower);
                                    artistList.add(new ArtistData(name, cover_url,
                                            id, external_url, Integer.parseInt(follower)));
                                }
                            }
                        } else if (count == 1){

                            switch (i % 5) {
                                case 0 -> {
                                    year = line;
//                                    System.out.println("year: " + line);
                                }
                                case 1 -> {
//                                    System.out.println("name: " + line);;
                                    name = line;
                                }
                                case 2 -> {
//                                    System.out.println("artist: " + line);
                                    artist = line;
                                }
                                case 3 -> {
//                                    System.out.println("cover_url: " + line);
                                    cover_url = line;
                                }
                                case 4 -> {
//                                    System.out.println("external_url: " + line);
                                    external_url = line;
                                    recommendList.add(new SongData(name, artist, cover_url, external_url, year));
                                }
                            }
                        }
                        //i++;
                        //System.out.println(line);
                    } else {
                        count++;
                        i = -1;
                    }
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        });
        outputThread.start();

        // Asynchronously read the error stream
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread errorThread = new Thread(() -> {
            try {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    // Process the error output as needed
                    System.err.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        });
        errorThread.start();

        // Wait for the process to finish
        process.waitFor();

        outputThread.join();
        errorThread.join();
    }

    private List<SongData> recommendList = new ArrayList<>();

    public HBox createHBox(SongData song){
        HBox main = new HBox();
        main.setSpacing(5);
        ImageView img = new ImageView(new Image(song.getCover_url()));
        img.setFitHeight(50);
        img.setFitWidth(50);

        VBox metaData = new VBox();
        Label name = new Label(song.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label artist = new Label(song.getArtists());
        artist.setFont(Font.font("System",14));

        //tmp1 = name;
        //tmp2 = artist;

        metaData.getChildren().addAll(name,artist);
        main.getChildren().addAll(img,metaData);
        Button button = new Button("...");
        button.setStyle("-fx-background-color: transparent; \n -fx-cursor: hand");
        main.getChildren().add(button);
        tmp = button;

        return main;
    }

    public void setArtistSection(){
        artist_section.getChildren().clear();
        int count = 0;
        for (ArtistData item: artistList){
            VBox component = createVBox(item);
            artist_section.getChildren().add(component);

            component.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2){
                    Init(item);
                }
            });
            component.setOnMouseEntered(mouseEvent -> component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;"));
            component.setOnMouseExited(mouseEvent -> component.setStyle("-fx-border-color: TRANSPARENT"));
            count++;
            if (count == 4) break;
        }
    }

    public VBox createVBox(ArtistData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(120);
        vbox.setPrefHeight(150);
        vbox.setAlignment(Pos.CENTER);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(110);
        imageView.setFitWidth(110);

        Circle circle = new Circle();
        circle.setCenterX(imageView.getX() + 55);
        circle.setCenterY(imageView.getY() + 55);
        circle.setRadius(54);

        circle.setStroke(Color.BLACK);
        imageView.setClip(circle);

        Label name = new Label(song.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label artist = new Label("Artist");
        artist.setFont(Font.font("System", 14));

        name.setTooltip(new Tooltip(song.getName()));
        //artist.setTooltip(new Tooltip(song.getArtists()));
        vbox.getChildren().add(imageView);
        vbox.getChildren().add(name);
        vbox.getChildren().add(artist);
        return vbox;
    }

    public void setPopularSection(ArtistData artist){
        recommend_section.getChildren().clear();
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setProgress(-1);
        indicator.setPrefSize(120,120);

        recommend_section.getChildren().add(indicator);
        Runnable task = () -> {
            //List<ArtistData> vectors  = new ArrayList<>();
            //vectors.add(artist);
            try {
                runFindSong(artist);
            } catch (InterruptedException | IOException | ParseException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() ->{
                recommend_section.getChildren().clear();
                int count = 0;
                for (SongData item: recommendList){
                    HBox component = createHBox(item);
                    count++;
                    recommend_section.getChildren().add(component);

                    component.setOnMouseClicked(mouseEvent -> {
                        if (mouseEvent.getClickCount() == 2){
                            musicSup.unHide();
                            musicSup.Init(item);
                        }
                    });

                    ContextMenu contextMenu = new ContextMenu();
                    //MenuItem item1 = new MenuItem("Play");
                    MenuItem item2 = new MenuItem("Go to Spotify Song");
                    MenuItem item3 = new MenuItem("Copy Song Link");

//                    item1.setOnAction(event -> {
//
//                    });

                    item2.setOnAction(event -> {
                        toUri(item.getPath());
                    });

                    item3.setOnAction(event -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();

                        ClipboardContent content = new ClipboardContent();
                        content.putString(item.getPath());
                        clipboard.setContent(content);
                    });

                    contextMenu.getItems().addAll(item2,item3);

                    tmp.setContextMenu(contextMenu);
                    tmp.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            contextMenu.show(tmp, event.getScreenX(), event.getScreenY());
                        }
                    });

                    component.setOnMouseEntered(mouseEvent -> component.setStyle("-fx-border-color: linear-gradient(to bottom right, #2d658c, #2ca772); " +
                            "\n -fx-border-radius: 4px 4px 4px 4px"));

                    component.setOnMouseExited(mouseEvent -> component.setStyle("-fx-border-color: TRANSPARENT"));
                    if (count == 5) break;
                }
                setArtistSection();
            });
        };
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
    }

    public void toUri(String url){
        try {
            URI uri = new URI(url);
            Desktop desktop = Desktop.getDesktop();

            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
                System.out.println("URL opened successfully.");
            } else {
                System.out.println("Desktop browsing is not supported.");
            }
        } catch (Exception e) {
            System.out.println("Error occurred while opening the URL: " + e.getMessage());
        }
    }

    private AnchorPane art_form;

    public void setArt_form(AnchorPane art_form) {
        this.art_form = art_form;
    }

    private Button tmp;

    public void hide(){
//        System.out.println("hide");
        art_form.setVisible(false);
    }
    public void unHide(){
        art_form.setVisible(true);
    }

    @FXML
    private Button hideBtn;

    private musicSupportController musicSup;

    public void setImgShape(ImageView img){
        Circle circle = new Circle();
        circle.setCenterX(img.getX() + img.getFitHeight()/2);
        circle.setCenterY(img.getY() + img.getFitWidth()/2);
        circle.setRadius(img.getFitHeight()/2 - 1);

        circle.setStroke(Color.BLACK);
        img.setClip(circle);
    }
    public void setMusicSup(musicSupportController musicSup) {
        this.musicSup = musicSup;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        hideBtn.setOnAction(event -> {
            hide();
        });
        setImgShape(track_img);
    }
}
