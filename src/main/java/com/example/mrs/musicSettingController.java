package com.example.mrs;

import com.example.mrs.dataModel.SongData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.glyphfont.FontAwesome;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class musicSettingController {

    @FXML
    private Button change_passBtn, connectSpotify;

    @FXML
    private TextField description;

    @FXML
    private PasswordField pass_new;

    @FXML
    private PasswordField pass_new1;

    @FXML
    private PasswordField pass_old;

    @FXML
    private Button send_feedbacks;

    @FXML
    private ToggleButton show_passBtn;

    @FXML
    private Button signOut;

    @FXML
    private TextField subject;

    @FXML
    private TextField text_new;

    @FXML
    private TextField text_new1;

    @FXML
    private TextField text_old;

    public musicSettingController() throws IOException {
    }

    public void toggleShowButton(){
        if (show_passBtn.isSelected()){
            pass_new.setVisible(false);
            pass_old.setVisible(false);
            pass_new1.setVisible(false);

            text_new.setVisible(true);
            text_old.setVisible(true);
            text_new1.setVisible(true);

            text_old.setText(pass_old.getText());
            text_new.setText(pass_new.getText());
            text_new1.setText(pass_new1.getText());
        } else {
            pass_new.setVisible(true);
            pass_old.setVisible(true);
            pass_new1.setVisible(true);

            text_new.setVisible(false);
            text_old.setVisible(false);
            text_new1.setVisible(false);

            pass_old.setText(text_old.getText());
            pass_new.setText(text_new.getText());
            pass_new1.setText(text_new1.getText());
        }
    }

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
        String sql = "SELECT * FROM user_data WHERE userID = '" + userID + "'";

        connect = Database.connectDB();
        try {
            assert connect != null;
            prepare = connect.prepareStatement(sql);
            result = prepare.executeQuery();
            result.next();

            if (result.getString("password").equals("-1")){
                pass_new.setEditable(false);
                pass_old.setEditable(false);
                pass_new1.setEditable(false);

                text_new.setEditable(false);
                text_old.setEditable(false);
                text_new1.setEditable(false);

                show_passBtn.setDisable(true);
                change_passBtn.setDisable(true);
            }
            if (!result.getString("spotify_email").equals("-1")){
                connectSpotify.setDisable(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Connection connect;

    PreparedStatement prepare;

    ResultSet result;

    @FXML
    public VBox updateSuccess;

    public void setChange_passBtn(){
        String sql = "SELECT password FROM user_data WHERE userID = '" + userID + "'";

        String old,New, renew;
        if (show_passBtn.isSelected()){
            old = text_old.getText();
            New = text_new.getText();
            renew = text_new1.getText();
        } else {
            old = pass_old.getText();
            New = pass_new.getText();
            renew = pass_new1.getText();
        }

        if (!New.equals(renew)){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Your re-enter password did not match the new password");
            alert.showAndWait();
            return;
        }

        connect = Database.connectDB();
        try{
            assert connect != null;
            prepare = connect.prepareStatement(sql);
            result = prepare.executeQuery();

            result.next();
            String pass = result.getString("password");

            if (!pass.equals(old)){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("You have entered wrong password");
                alert.showAndWait();
                return;
            }

            sql = "UPDATE user_data SET password = ? WHERE userID = ?";
            prepare = connect.prepareStatement(sql);
            prepare.setString(1, New);
            prepare.setString(2,String.valueOf(userID));
            prepare.executeUpdate();

            text_new.setText("");
            pass_new.setText("");
            text_old.setText("");
            pass_old.setText("");
            text_new1.setText("");
            pass_new1.setText("");

            showSuccess();
        }catch (SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void showSuccess(){
        updateSuccess.setVisible(true);

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updateSuccess.setVisible(false);
            }
        };
        // Schedule the task to run after a delay (in milliseconds)
        long delay = 3000;
        timer.schedule(task, delay);
    }
    public void sendFeedbacks(){
        if (subject.getText().isEmpty() || description.getText().isEmpty()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Please fill in all blank fields");
            alert.showAndWait();
            return;
        }
        String sql = "INSERT INTO feedbacks (subject, description, user) VALUES (?,?,?)";
        connect = Database.connectDB();

        try{
            assert connect != null;
            prepare = connect.prepareStatement(sql);
            prepare.setString(1,subject.getText());
            prepare.setString(2,description.getText());
            prepare.setString(3,String.valueOf(userID));
            prepare.executeUpdate();
            subject.setText("");
            description.setText("");
            showSuccess();
        }catch (SQLException e){
            e.printStackTrace(System.out);
        }
    }

    private final String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";

    public void connectToSpotify() throws InterruptedException, IOException {
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

        //System.out.println("Python script executed with exit code: " + exitCode);

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
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("Successfully connect to Spotify");
                    alert.showAndWait();
                }
            }
            FileWriter writer = new FileWriter("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt");
            // Ghi dữ liệu vào file
            writer.write("");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    private HelloController hello;

    public void setHello(HelloController hello) {
        this.hello = hello;
    }

    private double x,y;

    public void signOut(){
        try{
        Alert alert = new Alert (Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Message");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> option = alert.showAndWait();

        assert option.orElse(null) != null;
        if (option.orElse(null).equals(ButtonType.OK))
        {
            signOut.getScene().getWindow().hide();

            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("hello-view.fxml")));
            Stage stage = new Stage();
            Scene scene = new Scene(root);

            scene.setOnMousePressed((MouseEvent event) -> {
                x = event.getSceneX();
                y = event.getSceneY();
            });

            scene.setOnMouseDragged((MouseEvent event) ->{
                stage.setX(event.getScreenX() - x);
                stage.setY(event.getScreenY() - y);
                stage.setOpacity(.8);
            });

            scene.setOnMouseReleased((MouseEvent event) -> stage.setOpacity(1));


            stage.initStyle(StageStyle.TRANSPARENT);

            stage.setScene(scene);
            stage.show();
        }
    } catch (Exception e) {e.printStackTrace(System.out);}

        String cacheFilePath = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm\\.cache";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFilePath))) {
            // Write data to the cache file
            writer.write("{\"access_token\": \"\"}");

            //System.out.println("Data written to cache file.");
        } catch (IOException e) {
            e.printStackTrace(System.out);
            //System.out.println("Failed to write data to cache file.");
        }

        List<SongData> items = musicSup.getCounter();
        items.addAll(musicRec.getCounter());

        Connection connection = Database.connectDB();
        try {
            for (SongData item: items) {
                String sql = "INSERT INTO findHistoryTraces VALUES (name, year, artist, userID) " +
                        "VALUES (?,?,?,?)";
                assert connection != null;
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, item.getName());
                preparedStatement.setString(2, item.getYear());
                preparedStatement.setString(3, item.getArtists());
                preparedStatement.setString(4, String.valueOf(userID));
                preparedStatement.executeUpdate();
            }
        }catch (SQLException e){
            e.printStackTrace(System.out);
        }
    }
    musicSupportController musicSup;

    public void setMusicSup(musicSupportController musicSup) {
        this.musicSup = musicSup;
    }

    musicRecommenderController musicRec;

    public void setMusicRec(musicRecommenderController musicRec) {
        this.musicRec = musicRec;
    }
}
