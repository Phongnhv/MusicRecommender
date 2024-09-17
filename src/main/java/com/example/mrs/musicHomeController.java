package com.example.mrs;

import com.example.mrs.dataModel.SongData;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Rectangle;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
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
import java.util.concurrent.atomic.AtomicReference;

public class musicHomeController implements Initializable {

    @FXML
    private TextField Existed_track_search;

    @FXML
    private ImageView track_img;

    @FXML
    private Label track_name;

    @FXML
    private HBox recentPlay;

    private List<SongData> items = new ArrayList<>();

    private Connection connect;

    private PreparedStatement prepare;

    private ResultSet result;

    private musicPlayerController musicPlayer;

    public void setMusicPlayer(musicPlayerController musicPlayer){
        this.musicPlayer = musicPlayer;
    }

    private AnchorPane recommend_form;

    public void setRecommend_form(AnchorPane recommend_form) {
        this.recommend_form = recommend_form;
    }

    private List<SongData> recent_playlist = new ArrayList<>();

    private List<SongData> listeningHistoryList = new ArrayList<>();

    @FXML
    private HBox recentPlay2;

    @FXML
    private HBox recommendList;

    @FXML
    private Button toRecFormBtn;

    public void toRecForm(){
        rec_form.setVisible(false);
        recommend_form.setVisible(true);
    }

    public void setRecentPlay2() throws SQLException {
        String sql = "SELECT * FROM listeningHistory WHERE user = '" + userID +"' ORDER BY no DESC LIMIT 4";
        //System.out.println(userID);
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();
        //listeningHistoryList.clear();
        List<SongData>  vector = new ArrayList<>();

        while (result.next()){
            SongData song = new SongData(result.getString("name"),
                    result.getString("artist"),
                    result.getString("cover_url"),
                    result.getString("external_url"),
                    result.getString("year"));
            //System.out.println(song.getName());
            vector.add(song);
        }
        if (vector.equals(listeningHistoryList) && !vector.isEmpty()) return;
        listeningHistoryList = vector;

        recentPlay2.getChildren().clear();
        for (SongData song: listeningHistoryList){
            VBox vbox = createVBox(song);

            recentPlay2.getChildren().add(vbox);

            Label name = tmp1;
            Label artist = tmp2;

            vbox.setOnMouseClicked(mouseEvent -> {
                if (firstTime == 0){
                    unHideSongData();
                    firstTime = 1;
                }
                int pos = recentPlay2.getChildren().indexOf(vbox);
                SongData selectedSong = listeningHistoryList.get(pos);
                showSongData(selectedSong);
                if (mouseEvent.getClickCount() == 2) {
                    musicSup.unHide();
                    musicSup.Init(selectedSong);
                }
            });

            vbox.setOnMouseEntered(mouseEvent -> {
                vbox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                name.setTextFill(Color.WHITE);
                artist.setTextFill(Color.WHITE);
            });

            vbox.setOnMouseExited(mouseEvent -> {
                vbox.setStyle("-fx-background-color: TRANSPARENT");
                name.setTextFill(Color.BLACK);
                artist.setTextFill(Color.BLACK);
            });

        }
    }

    public int connectToSpotify() throws InterruptedException, IOException {
        String pythonScript = "connectSpotify.py";

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);
        command.add(String.valueOf(userID));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));

        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        System.out.println("Python script executed with exit code: " + exitCode);

        int res = -2;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt"));
            String line = reader.readLine();
            if (line.isEmpty()) res = -1;
            else {
                int number = Integer.parseInt(line);
                if (number == 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText(null);
                    alert.setContentText("This email is already been registered");
                    alert.showAndWait();
                    res = 0;
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("Successfully connect to Spotify");
                    alert.showAndWait();
                    res = 1;
                }
            }
            FileWriter writer = new FileWriter("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt");
            // Ghi dữ liệu vào file
            writer.write("");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return res;
    }

    private Label tmp1;
    private Label tmp2;

    public void setRecentPlay() throws SQLException {
        String sql = "SELECT * FROM user_recently_played WHERE user = '" + userID +"'";
        //System.out.println(userID);
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();
        List<SongData> vector = new ArrayList<>();
        //recent_playlist.clear();
        while (result.next()){
            SongData song = new SongData(result.getString("name"),
                    result.getString("artists"),
                    result.getString("cover_url"),
                    result.getString("external_url"),
                    result.getString("year"));

            vector.add(song);
        }
        if(vector.equals(recent_playlist) && !vector.isEmpty()){
            return;
        }
        recent_playlist = vector;

        if (recent_playlist.isEmpty()){
            String checkSql = "SELECT spotify_email FROM user_data WHERE userID = " + userID;
            prepare = connect.prepareStatement(checkSql);
            result = prepare.executeQuery();
            result.next();
            String email = result.getString("spotify_email");

            VBox vbox = new VBox();
            vbox.setPrefWidth(120);
            vbox.setPrefHeight(150);
            vbox.setAlignment(Pos.CENTER);

            String url = "D:\\IdeaProjects\\MRS\\src\\main\\resources\\com\\example\\mrs\\spotify-playlist.jpeg";
            Image img = new Image(url);
            ImageView imageView = new ImageView(img);
            imageView.setFitHeight(110);
            imageView.setFitWidth(110);
            imageView.setStyle("-fx-border-radius: 8px 8px 8px 8px");

            Label name;
            if (email.equals("-1")) name = new Label("Connect to Spotify");
            else name = new Label("Your listening history is empty");
            //Label artist = new Label(song.getArtists());

            if (email.equals("-1")) name.setTooltip(new Tooltip("Click to connect to Spotify and get more recommended songs"));
            vbox.getChildren().add(imageView);
            vbox.getChildren().add(name);

            recentPlay.getChildren().clear();
            recentPlay.getChildren().add(vbox);
            if (email.equals("-1")) {
                vbox.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getClickCount() == 2) {
                        try {
                            int k = connectToSpotify();
                            if (k == 1) setRecentPlay();
                        } catch (InterruptedException | IOException | SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            vbox.setOnMouseEntered(mouseEvent -> {
                vbox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                name.setTextFill(Color.WHITE);
            });

            vbox.setOnMouseExited(mouseEvent -> {
                vbox.setStyle("-fx-background-color: TRANSPARENT");
                name.setTextFill(Color.BLACK);
            });
            return;
        }

        int count = 0;
        recentPlay.getChildren().clear();
        for (SongData song : recent_playlist){
            VBox vbox = createVBox(song);
            vbox.setMinWidth(120);
            vbox.setMinHeight(150);

            recentPlay.getChildren().add(vbox);
            Label name = tmp1;
            Label artist = tmp2;
            count ++;

            //if (count == 1) vbox.setPadding(new Insets(0,0,0,20));
            vbox.setOnMouseClicked(mouseEvent -> {
                if (firstTime == 0){
                    unHideSongData();
                    firstTime = 1;
                }
                int pos = recentPlay.getChildren().indexOf(vbox);
                SongData selectedSong = recent_playlist.get(pos);
                showSongData(selectedSong);
                if (mouseEvent.getClickCount() == 2) {
                    musicSup.unHide();
                    musicSup.Init(selectedSong);
                }
            });

            vbox.setOnMouseEntered(mouseEvent -> {
                vbox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                name.setTextFill(Color.WHITE);
                artist.setTextFill(Color.WHITE);
            });

            vbox.setOnMouseExited(mouseEvent -> {
                vbox.setStyle("-fx-background-color: TRANSPARENT");
                name.setTextFill(Color.BLACK);
                artist.setTextFill(Color.BLACK);
            });

            if (count == 5) break;
        }
    }

    private List<SongData> recommendation = new ArrayList<>();

    private musicSupportController musicSup;

    public void setMusicSup(musicSupportController musicSup) {
        this.musicSup = musicSup;
    }

    public void setRecommendation(List<SongData> itemsList){
        if (itemsList.isEmpty())return;
        recommendation = itemsList;
        recommendList.getChildren().clear();
        for (SongData song: itemsList){
            VBox vbox = createVBox(song);

            recommendList.getChildren().add(vbox);

            Label name = tmp1;
            Label artist = tmp2;

            vbox.setOnMouseClicked(mouseEvent -> {
                if (firstTime == 0){
                    unHideSongData();
                    firstTime = 1;
                }
                //int pos = recommend_form.getChildren().indexOf(vbox);
                showSongData(song);
                if (mouseEvent.getClickCount() == 2) {
                    musicSup.unHide();
                    musicSup.Init(song);
                }
            });

            vbox.setOnMouseEntered(mouseEvent -> {
                vbox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                name.setTextFill(Color.WHITE);
                artist.setTextFill(Color.WHITE);
            });

            vbox.setOnMouseExited(mouseEvent -> {
                vbox.setStyle("-fx-background-color: TRANSPARENT");
                name.setTextFill(Color.BLACK);
                artist.setTextFill(Color.BLACK);
            });

        }
    }

    public VBox createVBox(SongData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(120);
        vbox.setPrefHeight(150);
        vbox.setAlignment(Pos.CENTER);

        Rectangle rectangle = new Rectangle();
        rectangle.setHeight(110);
        rectangle.setWidth(110);
        rectangle.setArcHeight(20);
        rectangle.setArcWidth(20);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(110);
        imageView.setFitWidth(110);
        imageView.setStyle("-fx-border-radius: 8px 8px 8px 8px");

        rectangle.setLayoutX(imageView.getX());
        rectangle.setLayoutY(imageView.getY());

        imageView.setClip(rectangle);

        Label name = new Label(song.getName());
        Label artist = new Label(song.getArtists());

        name.setFont(Font.font("System", FontWeight.BOLD, 12));

        name.setTooltip(new Tooltip(song.getName()));
        artist.setTooltip(new Tooltip(song.getArtists()));
        vbox.getChildren().add(imageView);
        vbox.getChildren().add(name);
        vbox.getChildren().add(artist);
        tmp1 = name;
        tmp2 = artist;
        return vbox;
    }

    @FXML
    private VBox song_data;

    private int firstTime = 0;

    public void unHideSongData(){
        for (int i = 0 ; i < song_data.getChildren().size() ; i++){
            song_data.getChildren().get(i).setVisible(true);
        }
    }
    public void showSongData(SongData songData){
        track_name.setText(songData.getName());
        //track_artist.setText(songData.getArtists());

        Image img = new Image(songData.getCover_url());
        track_img.setImage(img);
        localItems = musicPlayer.getPlayItems();
        SongData chosen = null;
        int check = 0;
        for (SongData item : localItems){
            if(songData.getName().equals(item.getName())) {
                check = 1;
                chosen = item;
                break;
            }
        }
        if (check == 1){
            playBtn.setDisable(false);
            playBtn.setTooltip(new Tooltip("Navigate to Music Player"));
            SongData finalChosen = chosen;
            playBtn.setOnAction(actionEvent -> {
                recFormBtn.setStyle("-fx-background-color:TRANSPARENT");
                playFormBtn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772)");
                currentNavigator = 2;
                musicPlayer.setNewMedia(finalChosen);
                play_form.setVisible(true);
                rec_form.setVisible(false);
            });
        }else {
            playBtn.setDisable(true);
            playBtn.setTooltip(null);
        }
        url_btn.setTooltip(new Tooltip("Navigate to Spotify web"));

        url_btn.setOnAction(actionEvent -> {
            String url = songData.getPath();

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
        });
        track_credit.getChildren().clear();

        Label Credits = new Label("Credits");
        Credits.setFont(Font.font("System",FontWeight.BOLD,18));
        Credits.setPadding(new Insets(0,0,0,10));
        track_credit.getChildren().add(Credits);

        List<String> Artists = List.of(songData.getArtists().split(", "));
        for (String artist: Artists){
            Label artistName = new Label(artist);
            artistName.setFont(Font.font("System",FontWeight.BOLD,16));

            Label role = new Label("Main artist");
            role.setFont(new Font(14));

            VBox vbox = new VBox(artistName,role);
            vbox.setPrefHeight(54);
            vbox.setPrefWidth(200);

            //Button navigate = new Button("...");
            //navigate.setStyle("-fx-background-color: transparent;\n -fx-cursor:hand");

            HBox hBox = new HBox(vbox);
            hBox.setPrefWidth(248);
            hBox.setPrefHeight(54);
            hBox.setPadding(new Insets(0,0,0,10));

            track_credit.getChildren().add(hBox);
        }
    }

    @FXML
    private VBox track_credit;


    private final String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";
    public void searchSongData() {
        String selectedData = Existed_track_search.getText();
        if (selectedData.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Message!");
            alert.setHeaderText(null);
            alert.setContentText("Please Choose a Song");
            alert.showAndWait();
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
                SongData songData = getSongData(selectedData);
//        SongData songData =  getSongDataFromJSON();

                if (songData == null){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error Message!");
                    alert.setHeaderText(null);
                    alert.setContentText("This song may not existed in Spotify Database");
                    alert.showAndWait();
                    return;
                }

                showSongData(songData);

                Existed_track_search.setText("");
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
    private Button url_btn;

    private AnchorPane play_form, rec_form;

    private Button playFormBtn, recFormBtn;

    private int currentNavigator;

    public void setForm(AnchorPane play_form, AnchorPane rec_form, Button playFormBtn, Button recFormBtn,
                        int currentNavigator){
        this.play_form = play_form;
        this.rec_form = rec_form;
        this.playFormBtn = playFormBtn;
        this.recFormBtn = recFormBtn;
        this.currentNavigator = currentNavigator;
    }

    @FXML
    private Button playBtn;

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public SongData getSongData(String songName) throws IOException, InterruptedException {
        String pythonScript = "getSong.py";
        String argument = "\"" + songName + "\"";

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);
        command.add(argument);
        command.add("3");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());

        Process process = processBuilder.start();

        AtomicReference<SongData> song = new AtomicReference<>();

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread outputThread = new Thread(() -> {
            try {
                String line;
                int i = 0;
                String name = "", artist= "", cover_url= "", external_url,year= "";
                while ((line = outputReader.readLine()) != null) {
                    // Process the output as needed

                    if (!line.isEmpty()) {
                        switch (i % 5) {
                            case 0 -> year = line;
                            case 1 -> name = line;
                            case 2 -> artist = line;
                            case 3 -> cover_url = line;
                            case 4 -> {
                                external_url = line;
                                song.set(new SongData(name, artist, cover_url, external_url, year));
                            }
                        }
                        i++;
                        //System.out.println(line);
                    }
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
        return song.get();
//        System.out.println("Python script executed with exit code: " + exitCode);
    }

//    public SongData getSongDataFromJSON() throws FileNotFoundException {
//        JSONParser parser = new JSONParser();
//        SongData songData = null;
//        //ArrayList<SongData> songList = new ArrayList<>();
//        try (FileReader reader = new FileReader("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\song_data.json")){
//            JSONArray jsArr = (JSONArray) parser.parse(reader);
//
//            for (Object obj: jsArr){
//                JSONObject jsObj = (JSONObject) obj;
//                String name = (String) jsObj.get("name");
//                String year = String.valueOf(jsObj.get("year"));
//                String artist = (String) jsObj.get("artist");
//                String cover_url = (String) jsObj.get("cover_url");
//                String external_url = (String) jsObj.get("external_url");
//                songData = new SongData(name, artist, cover_url, external_url, year);
//                //songList.add(songData);
//            }
//        } catch (IOException | ParseException e) {
//            throw new RuntimeException(e);
//        }
//        return songData;
//    }

    private List<SongData> localItems = new ArrayList<>();

    public void setLocalItems(List<SongData> localItems) {
        this.localItems = localItems;
    }

    public void setImageViewShape(){
        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
        rectangle.setHeight(220);
        rectangle.setWidth(220);
        rectangle.setArcHeight(20);
        rectangle.setArcWidth(20);

        rectangle.setLayoutX(track_img.getLayoutX());
        rectangle.setLayoutY(track_img.getLayoutY());

        Circle circle = new Circle();
        circle.setCenterX(track_img.getX() + 110);
        circle.setCenterY(track_img.getY() + 110);
        circle.setRadius(109);

        circle.setStroke(Color.BLACK);
        track_img.setClip(rectangle);

        //track_img.setClip(clip);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //inputTrackList();
        setImageViewShape();

    }
}
