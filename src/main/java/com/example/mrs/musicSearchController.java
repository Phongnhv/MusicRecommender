package com.example.mrs;

import com.example.mrs.dataModel.ArtistData;
import com.example.mrs.dataModel.SongData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class musicSearchController implements Initializable {
    @FXML
    private TextField Existed_track_search;

    @FXML
    private Button add_btn;

    @FXML
    private VBox mainBoard;

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public void hideMain(){
        mainBoard.setVisible(false);
    }

    public void unHideMain(){
        mainBoard.setVisible(true);
    }

    public void searchSongData() {
        String selectedData = Existed_track_search.getText();
        if (selectedData.isEmpty()){
            return;
        }
        Socket socket = new Socket();
        int timeoutMs = 1000; // Adjust the timeout value as needed
        try {
            socket.connect(new InetSocketAddress("www.google.com", 80), timeoutMs);
            socket.close();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Message!");
            alert.setHeaderText(null);
            alert.setContentText("Please connect to your network");
            alert.showAndWait();
            return;
        }

        Runnable task = () -> {
            try {
                songList.clear();
                artistList.clear();
                getSongData(selectedData);

                Existed_track_search.setText("");

                Platform.runLater(()->{
                    System.out.println("artists:-------------------------");
                    for (ArtistData item: artistList){
                        System.out.println(item.getName());
                    }
                    System.out.println("Song:------------------------------");
                    for (SongData item: songList){
                        System.out.println(item.getName());
                    }
                    setTopSearch();
                    setSearchSong();
                    setSearchArtist();
                    unHideSearch();
                    hideMain();
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(System.out);
            }
        };

        // Create an ExecutorService and submit the task
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
        //String selectedData = Existed_track_search.getText();
    }

    @FXML
    private ImageView top_img;

    @FXML
    private Label top_name, top_info, top_info2, top_info3;

    @FXML
    private VBox song_search, search_board, top_pane;

    @FXML
    private HBox artist_search;

    public boolean check(){
        Socket socket = new Socket();
        int timeoutMs = 1000; // Adjust the timeout value as needed
        try {
            socket.connect(new InetSocketAddress("www.google.com", 80), timeoutMs);
            socket.close();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Message!");
            alert.setHeaderText(null);
            alert.setContentText("Please connect to your network");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    public void setTopSearch(){
        System.out.println(type);
        if (type.equals("artist")){
            ArtistData top = artistList.get(0);
            artistList.remove(0);
            songList.remove(songList.size()-1);

            top_name.setText(top.getName());
            top_info.setText("Artist");
            top_img.setImage(new Image(top.getCover_url()));

            Circle circle = new Circle();
            circle.setCenterX(top_img.getX() + 50);
            circle.setCenterY(top_img.getY() + 50);
            circle.setRadius(49);

            circle.setStroke(Color.BLACK);
            top_img.setClip(circle);
            top_info2.setText("");
            top_info3.setText("");

            top_pane.setOnMouseClicked( event ->{
                if (event.getClickCount() == 2){

                    musicArt.unHide();
                    musicArt.Init(top);
                }
            });
        } else{
            SongData top = songList.get(0);
            songList.remove(0);
            artistList.remove(artistList.size()-1);

            top_name.setText(top.getName());
            top_info.setText("Song");
            top_info2.setText("-");
            top_info3.setText(top.getArtists());

            top_img.setImage(new Image(top.getCover_url()));

            javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
            rectangle.setHeight(100);
            rectangle.setWidth(100);
            rectangle.setArcHeight(20);
            rectangle.setArcWidth(20);

            rectangle.setLayoutX(top_img.getX());
            rectangle.setLayoutY(top_img.getY());

            top_img.setClip(rectangle);

            top_pane.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2){
                    musicSup.unHide();
                    musicSup.Init(top);
                }
            });
        }
    }

    public void hideSearch(){
        search_board.setVisible(false);
    }

    public void unHideSearch(){
        search_board.setVisible(true);
    }

    List<Object> recentSearchList = new ArrayList<>();

    public void setRecentSearch(){
        String sql = "SELECT * FROM searchdata WHERE user = '" + userID + "' ORDER BY NO DESC";

        recentSearchList.clear();

        connect = Database.connectDB();
        try{
            assert connect != null;
            prepare = connect.prepareStatement(sql);
            result = prepare.executeQuery();

            while (result.next()){
                if (result.getString("type").equals("artist")){
                    ArtistData artist = new ArtistData(result.getString("name"),
                            result.getString("cover_url"),
                            result.getString("artist_id"),
                            result.getString("external_url"),
                            Integer.parseInt(result.getString("year_follower")));
                    recentSearchList.add(artist);
                }else{
                    SongData song = new SongData(result.getString("name"),
                            result.getString("artist_id"),
                            result.getString("cover_url"),
                            result.getString("external_url"),
                            result.getString("year_follower"));
                    recentSearchList.add(song);
                }
            }
        }catch (SQLException e){
            e.printStackTrace(System.out);
        }
        recent_searchBox.getChildren().clear();
        int count = 0;
        for (Object item: recentSearchList){
            count ++;
            if (item instanceof ArtistData artist){
                VBox mainBox = createVBox(artist);

                mainBox.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getClickCount() == 2){
                        musicArt.unHide();
                        musicArt.Init(artist);
                    }
                });

                recent_searchBox.getChildren().add(mainBox);

                mainBox.setOnMouseEntered(mouseEvent -> mainBox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;"));

                mainBox.setOnMouseExited(mouseEvent -> mainBox.setStyle("-fx-border-color: TRANSPARENT"));
            }else{
                SongData song = (SongData) item;
                VBox mainBox = createVBox(song);

                mainBox.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getClickCount() == 2){
                        musicSup.unHide();
                        musicSup.Init(song);
                    }
                });

                recent_searchBox.getChildren().add(mainBox);

                mainBox.setOnMouseEntered(mouseEvent -> mainBox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;"));

                mainBox.setOnMouseExited(mouseEvent -> mainBox.setStyle("-fx-border-color: TRANSPARENT"));
            }


            if (count == 4) break;
        }
    }

    @FXML
    private HBox recent_searchBox;

    Connection connect;
    PreparedStatement prepare;
    ResultSet result;

    private musicSupportController musicSup;

    public void setMusicSup(musicSupportController musicSup) {
        this.musicSup = musicSup;
    }

    public void setSearchSong(){
        song_search.getChildren().clear();
        for (SongData item: songList){
            HBox component = createHBox(item);
            song_search.getChildren().add(component);

            component.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2){
                    musicSup.unHide();
                    musicSup.Init(item);
                }
            });

            component.setOnMouseEntered(mouseEvent -> component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;"));

            component.setOnMouseExited(mouseEvent -> component.setStyle("-fx-border-color: TRANSPARENT"));
            //if (count == 5) break;
        }
    }

    public HBox createHBox(SongData song){
        HBox main = new HBox();
        main.setSpacing(5);
        ImageView img = new ImageView(new Image(song.getCover_url()));
        img.setFitHeight(49);
        img.setFitWidth(49);

        VBox imgHolder = new VBox(img);
        imgHolder.setPadding(new Insets(0,0,0,5));
        imgHolder.setPrefWidth(49);
        imgHolder.setPrefHeight(49);

        VBox metaData = new VBox();
        Label name = new Label(song.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label artist = new Label(song.getArtists());
        artist.setFont(Font.font("System",14));

        metaData.getChildren().addAll(name,artist);
        main.getChildren().addAll(imgHolder,metaData);
        Button button = new Button("...");
        button.setStyle("-fx-background-color: transparent; \n -fx-cursor: hand");
        main.getChildren().add(button);

        return main;
    }

    public void setSearchArtist(){
        artist_search.getChildren().clear();
        for (ArtistData item: artistList){
            VBox component = createVBox(item);
            artist_search.getChildren().add(component);

            component.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2){
                    musicArt.unHide();
                    musicArt.Init(item);
                }
            });

            component.setOnMouseEntered(mouseEvent -> component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;"));

            component.setOnMouseExited(mouseEvent -> component.setStyle("-fx-border-color: TRANSPARENT"));
            //if (count == 5) break;
        }
    }

    public VBox createVBox(ArtistData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(170);
        vbox.setPrefHeight(200);
        vbox.setAlignment(Pos.CENTER);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(150);
        imageView.setFitWidth(150);

        Circle circle = new Circle();
        circle.setCenterX(imageView.getX() + 75);
        circle.setCenterY(imageView.getY() + 75);
        circle.setRadius(74);

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
        //tmp3 = name;
        //tmp4 = artist;
        return vbox;
    }

    public VBox createVBox(SongData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(170);
        vbox.setPrefHeight(200);
        vbox.setAlignment(Pos.CENTER);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(150);
        imageView.setFitWidth(150);

        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
        rectangle.setHeight(150);
        rectangle.setWidth(150);
        rectangle.setArcHeight(20);
        rectangle.setArcWidth(20);

        rectangle.setLayoutX(imageView.getX());
        rectangle.setLayoutY(imageView.getY());

        imageView.setClip(rectangle);


        Label name = new Label(song.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label artist = new Label(song.getArtists());
        artist.setFont(Font.font("System", 14));

        name.setTooltip(new Tooltip(song.getName()));
        artist.setTooltip(new Tooltip(song.getArtists()));
        vbox.getChildren().add(imageView);
        vbox.getChildren().add(name);
        vbox.getChildren().add(artist);
        //tmp3 = name;
        //tmp4 = artist;
        return vbox;
    }

    private String type;

    private List<ArtistData> artistList = new ArrayList<>();

    private List<SongData> songList = new ArrayList<>();

    private musicArtistController musicArt;

    public void setMusicArt(musicArtistController musicArt) {
        this.musicArt = musicArt;
    }

    public void getSongData(String songName) throws IOException, InterruptedException {
        String pythonScript = "search.py";
        songName = songName.replace("\"","\\\"");
        songName = songName.replace("'","\\'");
        String argument = "\"" + songName + "\"";

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);
        command.add(argument);
        //command.add("1");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";
        processBuilder.directory(new File(pythonDirectory));

        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());

        Process process = processBuilder.start();

        //AtomicReference<SongData> song = new AtomicReference<>();

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread outputThread = new Thread(() -> {
            try {
                String line;
                int i = 0, count = 0;
                String name = "", cover_url = "", id = "", external_url = "", follower;
                String year = "", artist = "";
                while ((line = outputReader.readLine()) != null) {
                    // Process the output as needed
                    if (!line.equals("!")) {
                        if (count == 0) {

                            switch (i % 5) {
                                case 0 -> {
                                    name = line;
                                    System.out.println("name: " + line);
                                }
                                case 1 -> {
                                    cover_url = line;
                                    System.out.println("cover_url:" + line);
                                }
                                case 2 -> {
                                    id = line;
                                    System.out.println("id: " + id);
                                }
                                case 3 -> {
                                    System.out.println("url:" + line);
                                    external_url = line;
                                }
                                case 4 -> {
                                    follower = line;
                                    System.out.println("follower: " + follower);
                                    artistList.add(new ArtistData(name, cover_url,
                                            id, external_url, Integer.parseInt(follower)));
                                    //song.set(new SongData(name, artist, cover_url, external_url, year));
                                    //System.out.println(artistList.size());
                                }
                            }
                        } else if (count == 1){

                            switch (i % 5) {
                                case 0 -> {
                                    year = line;
                                    //System.out.println("year: " + line);
                                }
                                case 1 -> {
                                    //System.out.println("name: " + line);
                                    name = line;
                                }
                                case 2 -> {
                                    //System.out.println("artist: " + line);
                                    artist = line;
                                }
                                case 3 -> {
                                    //System.out.println("cover_url: " + line);
                                    cover_url = line;
                                }
                                case 4 -> {
                                    //System.out.println("external_url: " + line);
                                    external_url = line;
                                    songList.add(new SongData(name, artist, cover_url, external_url, year));
                                }
                            }
                        } else type = line;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        top_pane.setOnMouseEntered(event -> top_pane.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;"));
        top_pane.setOnMouseExited(mouseEvent -> top_pane.setStyle("-fx-background-color: transparent"));
    }
}
