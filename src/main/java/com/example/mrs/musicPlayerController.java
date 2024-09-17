package com.example.mrs;

import com.example.mrs.dataModel.SongData;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.Duration;


import javafx.scene.media.*;


import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class musicPlayerController implements Initializable {

    @FXML
    private Button nextBtn;

    @FXML
    private Button playBtn;

    @FXML
    private Button prevBtn;

    @FXML
    private Label trackName;

    @FXML
    private Slider trackSlider;

    @FXML
    private ImageView track_img;

    @FXML
    private ListView<SongData> track_list;

    @FXML
    private TextField track_search;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Button volumeBtn;

    @FXML
    private Label artistName;

    private MediaPlayer mediaPlayer;


    public void preCreateMediaPlayer(){
        volumeSlider.setValue(mediaPlayer.getVolume()*100);
        volumeSlider.valueProperty().addListener(observable -> mediaPlayer.setVolume(volumeSlider.getValue()/100));

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> trackSlider.setValue(newValue.toSeconds()));

        trackSlider.setOnMousePressed(event -> mediaPlayer.seek(Duration.seconds(trackSlider.getValue())));

        trackSlider.setOnMouseDragged(event -> {
            mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
            if (!isPause) mediaPlayer.pause();
        });

        trackSlider.setOnMouseReleased(event -> {
            mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
            if (!isPause) mediaPlayer.play();
        });
    }

    private Connection connect;
    private PreparedStatement prepare;
    private ResultSet result;

    private final List<SongData> playItems = new ArrayList<>();

    private final List<String> external = new ArrayList<>();

    public void createLocalSongList() throws SQLException {
        String sql = "SELECT * FROM local_song_data ORDER BY name";
        connect = Database.connectDB();
        try{
            assert connect != null;
            prepare =connect.prepareStatement(sql);
            result = prepare.executeQuery();

            while (result.next()){
                playItems.add(new SongData(result.getString("name"),
                        result.getString("artist"),
                        result.getString("cover_url"),
                        result.getString("external_url"),
                        result.getString("year")));
                external.add(result.getString("url"));
            }

        } catch (Exception e){
            e.printStackTrace(System.out);
        }

        ObservableList<SongData> songList = FXCollections.observableList(playItems);

        track_list.setCellFactory(new Callback<>() {

            @Override
            public ListCell<SongData> call(ListView<SongData> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(SongData item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(item.getNameArtists());
                        }
                    }
                };
            }
        });
        track_list.setItems(songList);
    }

    @FXML
    private Button pauseBtn;

    public void pause(){
        mediaPlayer.pause();
        pauseBtn.setVisible(false);
        playBtn.setVisible(true);
        isPause = true;
        rotateTransition.pause();
    }

    public void play() throws SQLException {
        mediaPlayer.play();
        rotateTransition.play();
        isPause = false;
        playBtn.setVisible(false);
        pauseBtn.setVisible(true);

        connect = Database.connectDB();
        String sql = "DELETE FROM listeningHistory WHERE name = ? AND artist = ?";

        assert connect != null;
        prepare = connect.prepareStatement(sql);
        prepare.setString(1,currentTrack.getName());
        prepare.setString(2,currentTrack.getArtists());
        prepare.executeUpdate();

        sql = "INSERT INTO listeningHistory (name, artist, cover_url, external_url,user,year) VALUES " +
                "(?,?,?,?,?,?)";
        prepare = connect.prepareStatement(sql);
        prepare.setString(1, currentTrack.getName());
        prepare.setString(2, currentTrack.getArtists());
        prepare.setString(3, currentTrack.getCover_url());
        prepare.setString(4, external.get(playItems.indexOf(currentTrack)));
        prepare.setString(5, String.valueOf(userID));
        prepare.setString(6, currentTrack.getYear());
        prepare.executeUpdate();
        if (toggleSelect){

            addCounter(currentTrack);
        }
    }


    private SongData currentTrack;

    private boolean isPause = false;

    private Media media;

    public RotateTransition newRotation(){
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(20), track_img);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setAutoReverse(false);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        return rotateTransition;
    }

    //TODO add playlist function and add to listening
    public void setUpPlayDisc(){
        String imageUrl = "https://i.scdn.co/image/ab67616d0000b2730af4476af141051c728ee8b9";
        Image image = new Image(imageUrl);

        Circle circle = new Circle();
        circle.setCenterX(track_img.getX() + 90);
        circle.setCenterY(track_img.getY() + 90);
        circle.setRadius(89);

        currentTrack = playItems.get(0);

        circle.setStroke(Color.BLACK);
        track_img.setImage(image);
        track_img.setClip(circle);
    }

    public void InitMediaPlayer(){
        setUpPlayDisc();

        //RotateTransition rotateTransition = newRotation();
        rotateTransition = newRotation();

        playBtn.setOnAction(actionEvent -> {
            try {
                play();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            this.rotateTransition.play();
        });

        pauseBtn.setOnAction(actionEvent -> {
            pause();
            this.rotateTransition.pause();
        });

        EventHandler<MouseEvent> itemSelectedHandler = event -> {
            if (event.getClickCount() == 2){
            SongData selectedItem = track_list.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                setNewMedia(selectedItem);
            }}
        };

        track_list.setOnMouseClicked(itemSelectedHandler);

        String filePath = "file:///D:/IdeaProjects/MRS/src/main/resources/Tracks/JVKE_golden_hour.mp4";

        media = new Media(filePath);
        mediaPlayer = new MediaPlayer(media);

        preCreateMediaPlayer();
        ListeningHistory.add(playItems.get(0));

        mediaPlayer.setOnReady(() -> {
            Duration total = media.getDuration();
            //System.out.println(media.getDuration().toSeconds());
            trackSlider.setMax(total.toSeconds());
            //System.out.println(trackSlider.getMax());
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            if (currentIndex < ListeningHistory.size() - 1)
                setNewMedia(ListeningHistory.get(currentIndex+1));
            else setNewMedia(generateNextSong(currentTrack));
            //System.out.println("Media playback completed.");

        });

        nextBtn.setOnAction(event -> setNewMedia(generateNextSong(currentTrack)));

        prevBtn.setOnAction(event->{
            if (currentIndex>0){
                currentIndex--;
                setNewMedia(ListeningHistory.get(currentIndex));
            }
        });
    }

    public void setNewMedia(SongData selectedItem){
        if (!selectedItem.equals(ListeningHistory.get(currentIndex))) {
            if (currentIndex == ListeningHistory.size() - 1) {
                ListeningHistory.add(selectedItem);
                //System.out.println(1);
            } else {
                if (!ListeningHistory.get(currentIndex + 1).equals(selectedItem)) {
                    for (int i = ListeningHistory.size() - 1; i > currentIndex; i--) {
                        ListeningHistory.remove(i);
                    }
                    if (!selectedItem.equals(ListeningHistory.get(currentIndex))){
                        //System.out.println(selectedItem.getNameArtists());
                        //System.out.println(ListeningHistory.get(currentIndex).getNameArtists());
                        ListeningHistory.add(selectedItem);
                    }
                    else {currentIndex--;}//System.out.println(2.2);}
                }
            }
            currentIndex++;
        }

        isPause = false;
        mediaPlayer.stop();
        mediaPlayer = null;
        currentTrack = selectedItem;

        String filepath = selectedItem.getPath();

        String TempImageUrl = selectedItem.getCover_url();
        String name = selectedItem.getName();
        String artist = selectedItem.getArtists();

        trackSlider.setValue(0);
        double volume = volumeSlider.getValue();

        Image TempImage = new Image(TempImageUrl);
        track_img.setImage(TempImage);

        trackName.setText(name);
        artistName.setText(artist);

        media = new Media(filepath);
        mediaPlayer = new MediaPlayer(media);

        rotateTransition.setByAngle(0);
        rotateTransition.pause();
        rotateTransition.jumpTo(rotateTransition.getDuration());
        track_img.setRotate(0);

        playBtn.setVisible(true);
        pauseBtn.setVisible(false);
        preCreateMediaPlayer();

        mediaPlayer.setOnReady(() -> {
            Duration total = media.getDuration();
            //System.out.println(media.getDuration().toSeconds());
            trackSlider.setMax(total.toSeconds());
            //System.out.println(trackSlider.getMax());
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            if (currentIndex < ListeningHistory.size() - 1)
                setNewMedia(ListeningHistory.get(currentIndex+1));
            else setNewMedia(generateNextSong(currentTrack));
            //System.out.println("Media playback completed.");
        });

        mediaPlayer.setVolume(volume);
        volumeSlider.setValue(volume);
    }

    public List<SongData> recs = new ArrayList<>();

    public void setRecommends(List<SongData> recs){
        List<SongData> recsList = new ArrayList<>();
        for (SongData item: playItems){
            for (SongData item1: recs){
                if(item.getName().equals(item1.getName()) && !recsList.contains(item)){
                    recsList.add(item);
                }
            }
        }
        this.recs = recsList;
        recsBtn.setDisable(false);
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

    @FXML
    private Button refreshBtn, randomBtn;

    public void changeToRefresh(){
        refreshBtn.setVisible(true);
        randomBtn.setVisible(false);
        playType = 0;
    }

    public void changeToRandom(){
        refreshBtn.setVisible(false);
        randomBtn.setVisible(true);
        playType = 1;
    }

    public SongData generateNextSong(SongData selectedItem){
        int len = playItems.size();
        int index = playItems.indexOf(selectedItem) ;
        SongData nextSong = null;
        if (toggleSelect){
            boolean k = false;
            for (int i = index; i < len ; i++){
                if(i == index) continue;
                if (recs.contains(playItems.get(i))){
                    nextSong = playItems.get(i);
                    k = true;
                    break;
                }
            }
            if (!k){
                for(SongData item: playItems){
                    if (recs.contains(item)) {
                        nextSong = item;
                        break;
                    }
                }
            }
            return  nextSong;
        }
        if(playType == 0){
            index += 1;
            if (index >= len) index = index - len;
            nextSong = playItems.get(index);
        } else{
            Random random = new Random();
            if (index == len - 1)
                nextSong = playItems.get(0);
            else {
                int nextIndex = random.nextInt(len - index - 1) + index + 1;
                nextSong = playItems.get(nextIndex);
            }
        }
        return nextSong;
    }

    private boolean isInVolumeSlider = false;
    private boolean isInVolumeButton = false;
    public void InitVolumeButton(){

        //volumeSlider.setVisible(false);
        EventHandler<MouseEvent> enterVolumeBtn = event -> {
            isInVolumeButton = true;
            volumeSlider.setVisible(true);
            //System.out.println("isInVolumeButton:" + isInVolumeButton);
        };
        EventHandler<MouseEvent> exitVolumeBtn = event -> {
            isInVolumeButton = false;

            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (!isInVolumeSlider && !isInVolumeButton) volumeSlider.setVisible(false);
                }
            };
            timer.schedule(task,800);
            //System.out.println("isInVolumeSlider:" + isInVolumeSlider);

        };
        EventHandler<MouseEvent> enterVolumeSlider = event ->{
            isInVolumeSlider = true;
            volumeSlider.setVisible(true);
            //System.out.println("isInVolumeSlider:" + isInVolumeSlider);
        };


        EventHandler<MouseEvent> exitVolumeSlider = mouseEvent -> {
            isInVolumeSlider = false;
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (!isInVolumeButton && !isInVolumeSlider) volumeSlider.setVisible(false);
                }
            };
            timer.schedule(task,800);
        };

        volumeBtn.setOnMouseEntered(enterVolumeBtn);
        volumeBtn.setOnMouseExited(exitVolumeBtn);

        volumeSlider.setOnMouseEntered(enterVolumeSlider);
        volumeSlider.setOnMouseExited(exitVolumeSlider);

        volumeSlider.setValue(mediaPlayer.getVolume()*100);
        volumeSlider.valueProperty().addListener(observable -> {
            mediaPlayer.setVolume(volumeSlider.getValue()/100);
            if (isPause) mediaPlayer.pause();
        });
    }

    public void createSearchList(){
        track_search.textProperty().addListener((observable, oldValue, newValue) -> {
            ObservableList<SongData> filteredList = FXCollections.observableArrayList();
            for (SongData item : playItems) {
                if (item.getNameArtists().toLowerCase().contains(newValue.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            track_list.setItems(filteredList);
        });
    }

    private ArrayList<SongData> ListeningHistory = new ArrayList<>();
    private int currentIndex = 0;

    private int playType = 0;

    public SongData returnCurrentSong(){
        return currentTrack;
    }

    public List<SongData> getPlayItems(){
        return playItems;
    }

    private musicRecommenderController musicRecommender;

    public void setMusicRecommender(musicRecommenderController musicRecommender){
        this.musicRecommender = musicRecommender;
    }

    public void toggleRecsMode(){
        if(!toggleSelect){
            recsBtn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772);\n" +
                    "     -fx-background-radius:4px;\n" +
                    "     -fx-border-color:linear-gradient(to bottom right, #2d658c, #2ca772);\n" +
                    "     -fx-border-width:.8px;\n" +
                    "     -fx-border-radius:4px;");
            recsBtn.setTextFill(Color.WHITE);
//            for (SongData item: recs){
//                System.out.println(item.getName());
//            }

            toggleSelect = true;
            return;
        }
        recsBtn.setStyle("-fx-background-color:linear-gradient(to bottom, #efefef, #eee);\n" +
                "    -fx-border-width: 1px;");
        recsBtn.setTextFill(Color.BLACK);

        toggleSelect = false;
    }

    boolean toggleSelect = false;

    @FXML
    private Button recsBtn;

    private RotateTransition rotateTransition = new RotateTransition(Duration.seconds(5), track_img);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            createLocalSongList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        InitMediaPlayer();
        InitVolumeButton();
        createSearchList();
        //musicRecommender.setLocalItems(items);
    }
}
