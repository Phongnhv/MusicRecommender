package com.example.mrs;

import com.example.mrs.dataModel.SongData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.json.simple.parser.ParseException;

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
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class musicRecommenderController implements Initializable {


    @FXML
    private ImageView track_img;

    @FXML
    private Label track_name;

    private Connection connect;

    private PreparedStatement prepare;

    private ResultSet result;

    private musicPlayerController musicPlayer;

    public void setMusicPlayer(musicPlayerController musicPlayer){
        this.musicPlayer = musicPlayer;
    }

    ObservableList<SongData> songData = FXCollections.observableArrayList();

    private final String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";

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

    @FXML
    private Button refreshBtn;

    public List<SongData> runRecommendSong(List<SongData> vectors) throws InterruptedException, IOException, ParseException {
        if (vectors.isEmpty()) return new ArrayList<>();
        StringBuilder cmd = new StringBuilder("\"[");
        for (SongData vector: vectors){
            String name = vector.getName();

            name = name.replace("'","\\'");
            name = name.replace("\"","\\\"");
            cmd.append("{'name': '").append(name).append("', ");
            cmd.append("'year' :").append(vector.getYear()).append("}, ");
        }
        cmd.delete(cmd.length() - 2, cmd.length());
        cmd.append("]\"");
        //System.out.println(cmd);
        String pythonScript = "recommendSong.py";
        String argument = cmd.toString();

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);
        command.add(argument);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));

        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());

        Process process = processBuilder.start();

        AtomicReference<SongData> song = new AtomicReference<>();
        List<SongData> songList = new ArrayList<>();

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
                            case 0 -> {
                                year = line;
                                //System.out.println("year: " + year);
                            }
                            case 1 -> {
                                //name = line;
                                byte[] songBytes = line.getBytes(StandardCharsets.UTF_8);
                                name = new String(songBytes, StandardCharsets.UTF_8);
                                //System.out.println("name: " + name);
                            }
                            case 2 -> {
                                artist = line;
                                //System.out.println("art: " + artist);
                            }
                            case 3 -> {
                                cover_url = line;
                                //System.out.println("cover: " + cover_url);
                            }
                            case 4 -> {
                                external_url = line;
                                //System.out.println("url: " + external_url);
                                song.set(new SongData(name, artist, cover_url, external_url, year));
                                songList.add(song.get());
                            }
                        }
                        //System.out.print(i);
                        //System.out.println(" : " + line);
                        i++;
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
        return songList;
    }

    private List<SongData> basedOnSpotifyData = new ArrayList<>();
    private List<SongData> basedOnLocalData = new ArrayList<>();
    private List<SongData> basedOnSearchData = new ArrayList<>();

    @FXML
    private VBox rec_box;

    public void recommendSong() {

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

        rec_box.getChildren().clear();
        Label RecommendedTrack = new Label("Recommended Tracks");
        RecommendedTrack.setPadding(new Insets(10,0,0,10));
        RecommendedTrack.setFont(Font.font("System", FontWeight.BOLD, 18));


        Label basedOnSpotifyDataLabel = new Label("Based on Spotify listening history");
        Label basedOnLocalDataLabel = new Label("Based on local listening history");
        Label basedOnSearchDataLabel = new Label("Based on search history");
        basedOnLocalDataLabel.setFont(Font.font("System", FontWeight.MEDIUM, 16));
        basedOnSpotifyDataLabel.setFont(Font.font("System", FontWeight.MEDIUM,16));
        basedOnSearchDataLabel.setFont(Font.font("System", FontWeight.MEDIUM, 16));
        basedOnSpotifyDataLabel.setPadding(new Insets(0,0,0,10));
        basedOnLocalDataLabel.setPadding(new Insets(0,0,0,10));
        basedOnSearchDataLabel.setPadding(new Insets(0,0,0,10));

        VBox basedOnSpotifyDataVbox = new VBox();
        VBox basedOnLocalDataVbox = new VBox();
        VBox basedOnSearchDataVbox = new VBox();
        basedOnLocalDataVbox.setSpacing(5);
        basedOnSpotifyDataVbox.setSpacing(5);
        basedOnSearchDataVbox.setSpacing(5);
        basedOnLocalDataVbox.setPrefWidth(426);
        basedOnSpotifyDataVbox.setPrefWidth(426);
        basedOnSearchDataVbox.setPrefWidth(426);

        basedOnSpotifyDataVbox.setPadding(new Insets(0,0,0,10));
        basedOnLocalDataVbox.setPadding(new Insets(0,0,0,10));
        basedOnSearchDataVbox.setPadding(new Insets(0,0,0,10));
        basedOnSpotifyDataVbox.setPrefHeight(150);
        basedOnLocalDataVbox.setPrefHeight(150);
        basedOnSearchDataVbox.setPrefHeight(150);

        ProgressIndicator indicator1 = new ProgressIndicator(), indicator2 = new ProgressIndicator(),
        indicator3 = new ProgressIndicator();
        indicator1.setProgress(-1);
        indicator1.setPrefSize(120,120);

        indicator2.setProgress(-1);
        indicator2.setPrefSize(120,120);

        indicator3.setProgress(-1);
        indicator3.setPrefSize(120,120);

        VBox vBox1 = new VBox(indicator1, new Label("Loading"));
        VBox vBox2 = new VBox(indicator2, new Label("Loading"));
        VBox vBox3 = new VBox(indicator3, new Label("Loading"));

        vBox1.setPrefHeight(150);
        vBox1.setPrefWidth(120);
        vBox1.setAlignment(Pos.CENTER);

        vBox2.setPrefHeight(150);
        vBox2.setPrefWidth(120);
        vBox2.setAlignment(Pos.CENTER);

        vBox3.setPrefHeight(150);
        vBox3.setPrefWidth(120);
        vBox3.setAlignment(Pos.CENTER);

        basedOnSpotifyDataVbox.getChildren().add(vBox1);
        basedOnLocalDataVbox.getChildren().add(vBox2);
        basedOnSearchDataVbox.getChildren().add(vBox3);

        rec_box.getChildren().addAll(RecommendedTrack,basedOnSpotifyDataLabel,basedOnSpotifyDataVbox,
                basedOnLocalDataLabel,basedOnLocalDataVbox, basedOnSearchDataLabel, basedOnSearchDataVbox);

        Runnable task1 = () -> {
            try {
                List<SongData> vectors = getSpotifySongDataVector();
                if (!vectors.isEmpty()) basedOnSpotifyData = runRecommendSong(vectors);

                List<SongData> vectors2 = getListeningHistorySongDataVector();
                basedOnLocalData = runRecommendSong(vectors2);

                List<SongData> vectors3 = getSearchHistorySongDataVector();
                basedOnSearchData = runRecommendSong(vectors3);

                Platform.runLater(() -> {
                    List<SongData> toPlay = new ArrayList<>(basedOnSpotifyData);
                    //toPlay.addAll(basedOnSpotifyData);
                    for (SongData item: basedOnLocalData){
                        if(!toPlay.contains(item)){
                            toPlay.add(item);
                        }
                    }
                    for (SongData item: basedOnSearchData){
                        if(!toPlay.contains(item)){
                            toPlay.add(item);
                        }
                    }
                    musicPlayer.setRecommends(toPlay);
                    setBasedOnHBox(basedOnSpotifyDataVbox, basedOnSpotifyData);
                    setBasedOnHBox(basedOnLocalDataVbox, basedOnLocalData);
                    setBasedOnHBox(basedOnSearchDataVbox, basedOnSearchData);
                    List<SongData> toHome = new ArrayList<>();
                    if (!basedOnSpotifyData.isEmpty()){
                        toHome.add(basedOnSpotifyData.get(0));
                        toHome.add(basedOnSpotifyData.get(1));
                        toHome.add(basedOnSpotifyData.get(2));
                    }
                    int k = toHome.size() - 1;
                    if (!basedOnLocalData.isEmpty()) {
                        for (int j = k; j < 3; j++) {
                            toHome.add(basedOnLocalData.get(j - k));
                        }
                    }
                    musicHome.setRecommendation(toHome);

                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(System.out);
            } catch (SQLException | ParseException e) {
                throw new RuntimeException(e);
            }
        };

        // Create an ExecutorService and submit the task
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task1);
        executorService.shutdown();
    }
    @FXML
    private VBox song_data;

    int firstTime = 0;

    public void unHideSongData(){
        for (int i = 0 ; i < song_data.getChildren().size() ; i++){
            song_data.getChildren().get(i).setVisible(true);
        }
    }

    private musicHomeController musicHome;

    public void setMusicHome(musicHomeController musicHome) {
        this.musicHome = musicHome;
    }

    private Label tmp1, tmp2;

    public void setBasedOnHBox(VBox asc, List<SongData> itemList){
        if (itemList.isEmpty()){
            rec_box.getChildren().remove(rec_box.getChildren().indexOf(asc) - 1);
            asc.getChildren().clear();
            return;
        }
        asc.getChildren().clear();

        int count = 0;
        //System.out.println((itemList.size()));
        //System.out.println((itemList.size() - 1)/5 + 1);
        for (int i = 0 ; i < (itemList.size() - 1)/4 + 1 ; i++) {
            HBox hbox = new HBox();
            hbox.setPrefWidth(426);
            hbox.setPrefHeight(150);
            hbox.setSpacing(10);

            for ( ; count < itemList.size() ; count++) {
                SongData song = itemList.get(count);
                VBox vbox = createVBox(song);

                hbox.getChildren().add(vbox);
                Label name = tmp1;
                Label artist = tmp2;
                //count++;

                //if (count == 1) vbox.setPadding(new Insets(0,0,0,20));
                vbox.setOnMouseClicked(mouseEvent -> {
                    if (firstTime == 0){
                        unHideSongData();
                        firstTime = 1;
                    }
                    int parentPos = asc.getChildren().indexOf(vbox.getParent());
                    int pos = hbox.getChildren().indexOf(vbox);
                    int idx = parentPos * 4 + pos;
                    SongData selectedSong = itemList.get(idx);
                    showSongData(selectedSong);
                    if (mouseEvent.getClickCount() == 2){
                        musicSup.unHide();
                        musicSup.Init(song);
                    }
                });

                vbox.setOnMouseEntered(mouseEvent -> {
                    vbox.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                    name.setTextFill(javafx.scene.paint.Color.WHITE);
                    artist.setTextFill(javafx.scene.paint.Color.WHITE);
                });

                vbox.setOnMouseExited(mouseEvent -> {
                    vbox.setStyle("-fx-background-color: TRANSPARENT");
                    name.setTextFill(javafx.scene.paint.Color.BLACK);
                    artist.setTextFill(Color.BLACK);
                });

                if (count % 4 == 3) break;
            }
            count ++;
            asc.getChildren().add(hbox);
        }
    }

    private musicSupportController musicSup;

    public void setMusicSup(musicSupportController musicSup) {
        this.musicSup = musicSup;
    }

    public VBox createVBox(SongData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(120);
        vbox.setPrefHeight(170);
        vbox.setAlignment(Pos.CENTER);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(110);
        imageView.setFitWidth(110);

        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
        rectangle.setHeight(110);
        rectangle.setWidth(110);
        rectangle.setArcHeight(20);
        rectangle.setArcWidth(20);

        rectangle.setLayoutX(imageView.getX());
        rectangle.setLayoutY(imageView.getY());

        imageView.setClip(rectangle);

        imageView.setStyle("-fx-border-radius: 8px 8px 8px 8px");

        Label name = new Label(song.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label artist = new Label(song.getArtists());

        name.setTooltip(new Tooltip(song.getName()));
        artist.setTooltip(new Tooltip(song.getArtists()));
        vbox.getChildren().add(imageView);
        vbox.getChildren().add(name);
        vbox.getChildren().add(artist);
        tmp1 = name;
        tmp2 = artist;
        return vbox;
    }

    public List<SongData> getSearchHistorySongDataVector() throws  SQLException{
        String sql  = "SELECT * FROM searchdata WHERE user = '" + userID + "' AND type = 'song' ORDER BY no LIMIT 5";
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();

        List<SongData> songVector = new ArrayList<>();

        while (result.next()){
            String name = result.getString("name").replace("\\","");
            //name = name.replace("\\","\\");
            //System.out.println(name);
            SongData data = new SongData(name,
                    result.getString("artist_id"),
                    result.getString("cover_url"),
                    result.getString("external_url"),
                    result.getString("year_follower"));

            songVector.add(data);
        }

        return songVector;
    }

    public List<SongData> getListeningHistorySongDataVector() throws SQLException {
        String sql  = "SELECT * FROM listeningHistory WHERE user = '" + userID + "' ORDER BY no LIMIT 5";
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();

        List<SongData> songVector = new ArrayList<>();

        while (result.next()){
            //System.out.println(result.getString("name"));
            SongData data = new SongData(result.getString("name"),
                    result.getString("artist"),
                    "", "", result.getString("year"));

            songVector.add(data);
        }

        return songVector;
    }

    public List<SongData> getSpotifySongDataVector() throws SQLException {
        String sql  = "SELECT * FROM user_recently_played WHERE user = '" + userID + "' LIMIT 5";
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();

        List<SongData> songVector = new ArrayList<>();

        while (result.next()){
            //System.out.println(result.getString("name"));
            SongData data = new SongData(result.getString("name"),
                    result.getString("artists"),
                    "", "", result.getString("year"));

            songVector.add(data);
        }

        return songVector;
    }

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
    }

    List<SongData> counter = new ArrayList<>();

    public List<SongData> getCounter() {
        return counter;
    }

    public void addCounter(SongData song){
        counter.add(song);
    }

    public void showSongData(SongData songData){
        track_name.setText(songData.getName());

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
                addCounter(songData);
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
            addCounter(songData);
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
        Credits.setFont(javafx.scene.text.Font.font("System", FontWeight.BOLD,18));
        Credits.setPadding(new javafx.geometry.Insets(0,0,0,10));
        track_credit.getChildren().add(Credits);

        List<String> Artists = List.of(songData.getArtists().split(", "));
        for (String artist: Artists){
            Label artistName = new Label(artist);
            artistName.setFont(javafx.scene.text.Font.font("System",FontWeight.BOLD,16));

            Label role = new Label("Main artist");
            role.setFont(new Font(14));

            VBox vbox = new VBox(artistName,role);
            vbox.setPrefHeight(54);
            vbox.setPrefWidth(200);

//            Button navigate = new Button("...");
//            navigate.setStyle("-fx-background-color: transparent;\n -fx-cursor:hand");

            HBox hBox = new HBox(vbox);
            hBox.setPrefWidth(248);
            hBox.setPrefHeight(54);
            hBox.setPadding(new Insets(0,0,0,10));

            track_credit.getChildren().add(hBox);
        }
    }
    @FXML
    private VBox track_credit;
    private List<SongData> localItems = new ArrayList<>();

    public void setLocalItems(List<SongData> localItems) {
        this.localItems = localItems;
    }

    public void setTrackImgShape(){
        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
        rectangle.setHeight(220);
        rectangle.setWidth(220);
        rectangle.setArcHeight(30);
        rectangle.setArcWidth(30);

        rectangle.setLayoutX(track_img.getX());
        rectangle.setLayoutY(track_img.getY());

        track_img.setClip(rectangle);
    }

    public void getUserData() throws SQLException, IOException, InterruptedException {
        String sql = "SELECT spotify_email from user_data WHERE userID = '" + userID +"'";
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();
        result.next();
        if (Objects.equals(result.getString("spotify_email"), "-1")){
            return;
        }

        String pythonScript = "getUserData.py";

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));

        Process process = processBuilder.start();

        process.waitFor();
    }

    public boolean checkData() throws SQLException {
        String sql  = "SELECT * FROM user_recently_played WHERE user = '" + userID + "' LIMIT 5";
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();

        List<SongData> songVector = new ArrayList<>();

        while (result.next()){
            SongData data = new SongData(result.getString("name"),
                    result.getString("artists"),
                    "", "", result.getString("year"));

            songVector.add(data);
        }
        int count = 0;

        for (SongData items: songVector){
            if (basedOnSpotifyData.contains(items)) count++;
        }

        sql  = "SELECT * FROM listeningHistory WHERE user = '" + userID + "' ORDER BY no LIMIT 5";
        connect = Database.connectDB();
        assert connect != null;
        prepare = connect.prepareStatement(sql);
        result = prepare.executeQuery();

        songVector = new ArrayList<>();

        while (result.next()){
            SongData data = new SongData(result.getString("name"),
                    result.getString("artist"),
                    "", "", result.getString("year"));

            songVector.add(data);
        }

        for (SongData items: songVector){
            if (basedOnLocalData.contains(items)) count++;
        }
        return count <= 4;
    }

    public void setTimeTask(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // Code to be executed
                Runnable task1 = ()->{
                    try {
                        getUserData();
                        if (!checkData()) return;
                        recommendSong();
                    } catch (SQLException | IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                };

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(task1);
                executorService.shutdown();

            }
        };
        // Schedule the task to run after a delay (in milliseconds)
        long delay = 600000; // 2 seconds
        //timer.schedule(task, delay);

        // Stop the timer after a certain period (in milliseconds)
        long period = 600000; // 10 min
        timer.schedule(task, delay, period);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTrackImgShape();
    }
}
