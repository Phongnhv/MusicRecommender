package com.example.mrs;

import com.example.mrs.dataModel.SongData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;

import java.sql.*;
import java.util.*;

public class dashboardController implements Initializable {

    @FXML
    private Button Rec_btn;

    @FXML
    private AnchorPane main_form;

    @FXML
    private Button mp_btn;

    @FXML
    private AnchorPane rec_form;

    @FXML
    private Label username;

    @FXML
    private Button search_btn;

    private int CurrentNavigator = 3;

    public void recBtn(){
        rec_form.setVisible(true);
        play_form.setVisible(false);
        home_form.setVisible(false);
        search_form.setVisible(false);
        setting_form.setVisible(false);

        Rec_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #3f82ae, #26bf7d)");
        mp_btn.setStyle("-fx-background-color: transparent");
        home_btn.setStyle("-fx-background-color: transparent");
        search_btn.setStyle("-fx-background-color: transparent");
        setting_btn.setStyle("-fx-background-color: transparent");
        musicSearch.unHideMain();
        musicSearch.hideSearch();

        CurrentNavigator = 1;
    }

    public void mpBtn(){
        rec_form.setVisible(false);
        play_form.setVisible(true);
        home_form.setVisible(false);
        search_form.setVisible(false);
        setting_form.setVisible(false);

        Rec_btn.setStyle("-fx-background-color: transparent");
        home_btn.setStyle("-fx-background-color: transparent");
        mp_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #3f82ae, #26bf7d)");
        search_btn.setStyle("-fx-background-color: transparent");
        setting_btn.setStyle("-fx-background-color: transparent");
        musicSearch.unHideMain();
        musicSearch.hideSearch();

        CurrentNavigator = 2;
    }

    public void home_btn() {
        rec_form.setVisible(false);
        play_form.setVisible(false);
        home_form.setVisible(true);
        search_form.setVisible(false);
        setting_form.setVisible(false);

        mp_btn.setStyle("-fx-background-color: transparent");
        Rec_btn.setStyle("-fx-background-color: transparent");
        home_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #3f82ae, #26bf7d)");
        search_btn.setStyle("-fx-background-color: transparent");
        setting_btn.setStyle("-fx-background-color: transparent");

        try{
            musicHome.setRecentPlay();
            musicHome.setRecentPlay2();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        musicSearch.unHideMain();
        musicSearch.hideSearch();
        CurrentNavigator = 3;
    }

    public void searchBtn(){
        rec_form.setVisible(false);
        play_form.setVisible(false);
        home_form.setVisible(false);
        search_form.setVisible(true);
        setting_form.setVisible(false);

        mp_btn.setStyle("-fx-background-color: transparent");
        Rec_btn.setStyle("-fx-background-color: transparent");
        home_btn.setStyle("-fx-background-color:transparent");
        search_btn.setStyle("-fx-background-color: linear-gradient(to bottom right, #3f82ae, #26bf7d)");
        setting_btn.setStyle("-fx-background-color: transparent");

        musicSearch.setRecentSearch();
        CurrentNavigator = 4;
    }

    public void settingBtn(){
        rec_form.setVisible(false);
        play_form.setVisible(false);
        home_form.setVisible(false);
        search_form.setVisible(false);
        setting_form.setVisible(true);

        mp_btn.setStyle("-fx-background-color: transparent");
        Rec_btn.setStyle("-fx-background-color: transparent");
        home_btn.setStyle("-fx-background-color:transparent");
        search_btn.setStyle("-fx-background-color:transparent");
        setting_btn.setStyle("-fx-background-color: linear-gradient(to bottom right, #3f82ae, #26bf7d)");

        musicSearch.unHideMain();
        musicSearch.hideSearch();
        //musicSearch.setRecentSearch();
        CurrentNavigator = 5;
    }
    public void switchForm(ActionEvent event) throws SQLException {

    }
    public void Dclose()
    {
        try {
            FileWriter writer = new FileWriter("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt");
            // Ghi dữ liệu vào file
            writer.write("");
        }catch (Exception e){
            e.printStackTrace(System.out);
        }

        List<SongData> items = musicSupport.getCounter();
        items.addAll(musicRecommender.getCounter());
        items.addAll(musicPlayer.getCounter());

        Connection connection = Database.connectDB();
        try {
            for (SongData item: items) {
                String sql = "INSERT INTO findHistoryTraces (name, year, artist, userID) " +
                        "VALUES (?,?,?,?)";
                assert connection != null;
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, item.getName());
                preparedStatement.setString(2, item.getYear());
                preparedStatement.setString(3, item.getArtists());
                preparedStatement.setString(4, String.valueOf(userID));
                System.out.println(preparedStatement);
                preparedStatement.executeUpdate();
            }
        }catch (SQLException e){
            e.printStackTrace(System.out);
        }
        System.exit(0);
    }

    public void Minimize()
    {
        Stage stage = (Stage)main_form.getScene().getWindow();
        stage.setIconified(true);
    }

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
        String sql = "SELECT * FROM user_data WHERE userID = '" + userID + "'";
        Connection connection = Database.connectDB();
        try {
            assert connection != null;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet  resultSet = preparedStatement.executeQuery();
            resultSet.next();
            username.setText(resultSet.getString("username"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void defaultNav(){
        //Navigate Home Form First
        home_form.setVisible(true);
        rec_form.setVisible(false);
        play_form.setVisible(false);

        CurrentNavigator = 3;

        home_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #3f82ae, #26bf7d)");
        //mp_btn.setStyle("-fx-background-color: transparent");

        //Re-create hover css on navigate buttons
        Rec_btn.setOnMouseEntered(MouseEvent ->{
            if (CurrentNavigator == 1) return;
            Rec_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772)");
        });
        Rec_btn.setOnMouseExited(MouseEvent ->{
            if (CurrentNavigator == 1) return;
            Rec_btn.setStyle("-fx-background-color: transparent");
        });

        mp_btn.setOnMouseEntered(MouseEvent ->{
            if (CurrentNavigator == 2) return;
            mp_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772)");
        });
        mp_btn.setOnMouseExited(MouseEvent ->{
            if (CurrentNavigator == 2) return;
            mp_btn.setStyle("-fx-background-color: transparent");
        });

        home_btn.setOnMouseEntered(MouseEvent ->{
            if (CurrentNavigator == 3) return;
            home_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772)");
        });
        home_btn.setOnMouseExited(MouseEvent ->{
            if (CurrentNavigator == 3) return;
            home_btn.setStyle("-fx-background-color: transparent");
        });

        search_btn.setOnMouseEntered(MouseEvent ->{
            if (CurrentNavigator == 4) return;
            search_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772)");
        });
        search_btn.setOnMouseExited(MouseEvent ->{
            if (CurrentNavigator == 4) return;
            search_btn.setStyle("-fx-background-color: transparent");
        });

        setting_btn.setOnMouseEntered(MouseEvent ->{
            if (CurrentNavigator == 5) return;
            setting_btn.setStyle("-fx-background-color:linear-gradient(to bottom right, #2d658c, #2ca772)");
        });
        setting_btn.setOnMouseExited(MouseEvent ->{
            if (CurrentNavigator == 5) return;
            setting_btn.setStyle("-fx-background-color: transparent");
        });
    }

    public void initRecommenderForm() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("music_rec.fxml"));
        AnchorPane newContent = fxmlLoader.load();

        musicRecommender = fxmlLoader.getController();

        rec_form.getChildren().setAll(newContent);
    }

    private musicSupportController musicSupport;

    private musicPlayerController musicPlayer;

    private musicRecommenderController musicRecommender;

    @FXML
    private AnchorPane play_form;
    public void initMusicPlayerForm() throws  IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("music_play.fxml"));
        AnchorPane LoadContent = fxmlLoader.load();

        musicPlayer = fxmlLoader.getController();

        play_form.getChildren().setAll(LoadContent);
    }

    @FXML
    private AnchorPane home_form;

    @FXML
    private Button home_btn;

    private musicHomeController musicHome;

    public void initHomeForm() throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("music_home.fxml"));
        AnchorPane LoadContent = fxmlLoader.load();

        musicHome = fxmlLoader.getController();

        home_form.getChildren().setAll(LoadContent);
    }

    public void initSearchForm() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("music_search.fxml"));
        AnchorPane LoadContent = fxmlLoader.load();

        musicSearch = fxmlLoader.getController();

        search_form.getChildren().setAll(LoadContent);
    }

    private musicSearchController musicSearch;

    private HelloController hello;

    public void setHello(HelloController hello){
        this.hello = hello;
    }

    @FXML
    private AnchorPane sup_form;

    @FXML
    private AnchorPane search_form;

    public AnchorPane getSup_form(){
        return this.sup_form;
    }

    public void setCurrentNavigator(int currentNavigator) {
        CurrentNavigator = currentNavigator;
    }

    private dashboardController dashboard;

    public void setDashboard(dashboardController dashboard) {
        this.dashboard = dashboard;
        musicSupport.setDashboard(dashboard);
    }

    private double x1,y1;
    public void initSupportForm() throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("sup_form.fxml"));
        AnchorPane loadContent = fxmlLoader.load();

        musicSupport = fxmlLoader.getController();

        sup_form.getChildren().setAll(loadContent);

        sup_form.setOnMousePressed((MouseEvent event) ->{
            event.consume();
            x1 = event.getSceneX();
            y1 = event.getSceneY();
        });

        sup_form.setOnMouseDragged((MouseEvent event) ->{
            event.consume();

            //AnchorPane parentPane = (AnchorPane) sup_form.getParent();

            double minX = 0;
            double minY = 0;
            double maxX = 828 - sup_form.getPrefWidth();
            double maxY = 557.6 - sup_form.getPrefHeight() - 25;

            double goToX = event.getSceneX() - x1;
            double goToY = event.getSceneY() - y1;

            double newX, newY;
            if (goToY < minY) newY = minY;
            else newY = Math.min(goToY, maxY);

            if (goToX < minX) newX = minX;
            else newX = Math.min(goToX, maxX);

            sup_form.setLayoutX(newX);
            sup_form.setLayoutY(newY);
        });

    }

    private musicSettingController musicSetting;

    public void InitSettingForm() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("music_setting.fxml"));
        AnchorPane loadContent = fxmlLoader.load();

        musicSetting = fxmlLoader.getController();

        setting_form.getChildren().setAll(loadContent);
    }

    private musicArtistController musicArt;

    public void initArtForm() throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("sup_artist.fxml"));
        AnchorPane loadContent = fxmlLoader.load();

        musicArt = fxmlLoader.getController();

        art_form.getChildren().setAll(loadContent);

        art_form.setOnMousePressed((MouseEvent event) ->{
            event.consume();
            x1 = event.getSceneX();
            y1 = event.getSceneY();
        });

        art_form.setOnMouseDragged((MouseEvent event) ->{
            event.consume();

            //AnchorPane parentPane = (AnchorPane) sup_form.getParent();

            double minX = 0;
            double minY = 0;
            double maxX = 828 - art_form.getPrefWidth();
            double maxY = 557.6 - art_form.getPrefHeight() - 25;
//
            double goToX = event.getSceneX() - x1;
            double goToY = event.getSceneY() - y1;
//
            double newX, newY;
            if (goToY < minY) newY = minY;
            else newY = Math.min(goToY, maxY);

            if (goToX < minX) newX = minX;
            else newX = Math.min(goToX, maxX);

            art_form.setLayoutX(newX);
            art_form.setLayoutY(newY);
        });

        musicArt.setArt_form(art_form);
    }

    public void setUpUserID() throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt"));
            String line = reader.readLine();

            line = reader.readLine();
            userID = Integer.parseInt(line);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileWriter writer = new FileWriter("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt");

        // Ghi dữ liệu vào file
        writer.write("");
    }

    @FXML
    private AnchorPane art_form;

    @FXML
    private AnchorPane setting_form;

    @FXML
    private Button setting_btn;

    public void setArgForSearch(){
        musicSearch.setMusicSup(musicSupport);
        musicSearch.setUserID(userID);
        musicSearch.setMusicArt(musicArt);
    }

    public void setArgForArt(){
        musicArt.setMusicSup(musicSupport);
        musicSupport.setMusicArt(musicArt);
        musicArt.setUserID(userID);
        musicSupport.setUserID(userID);
    }

    public void setArgForHome(){
        musicHome.setForm(play_form,home_form,mp_btn,home_btn,CurrentNavigator);
        musicHome.setRecommend_form(rec_form);
        musicHome.setMusicSup(musicSupport);
        musicHome.setMusicPlayer(musicPlayer);
        musicHome.setUserID(userID);
    }

    public void setArgForPlayer(){
        musicPlayer.setMusicRecommender(musicRecommender);
        musicPlayer.setUserID(userID);
    }

    public void setArgForRecommender(){
        musicRecommender.setMusicPlayer(musicPlayer);
        musicRecommender.setForm(play_form,rec_form,mp_btn, Rec_btn, CurrentNavigator);
        musicRecommender.setMusicHome(musicHome);
        musicRecommender.setUserID(userID);
        musicRecommender.setMusicSup(musicSupport);
    }

    public void setArgForSetting(){
        musicSetting.setUserID(userID);
        musicSetting.setHello(hello);
        musicSetting.setMusicRec(musicRecommender);
        musicSetting.setMusicSup(musicSupport);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        defaultNav();
        try {
            initRecommenderForm();
            initMusicPlayerForm();
            initHomeForm();
            initSupportForm();
            initSearchForm();
            initArtForm();
            InitSettingForm();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            setUpUserID();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //System.out.println(userID);


        setArgForHome();
        setArgForPlayer();
        setArgForRecommender();
        setArgForSearch();
        setArgForArt();
        setArgForSetting();
        musicSupport.setMusicPlayer(musicPlayer);
        //musicRecommender.recommendSong();
        //musicRecommender.setTimeTask();
        try {
            musicHome.setRecentPlay();
            musicHome.setRecentPlay2();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
