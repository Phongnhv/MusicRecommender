package com.example.mrs;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HelloController {
    public void Close(){
        System.exit(0);
    }

    @FXML
    private Button loginBtn;

    @FXML
    private TextField Signup_username;

    @FXML
    private PasswordField Signup_password;

    @FXML
    private PasswordField Signup_reenter;

    @FXML
    private PasswordField password;

    @FXML
    private TextField username;


    @FXML
    private AnchorPane right_form1;

    @FXML
    private AnchorPane right_form2;

    @FXML
    private Button SignupBtn2;

    //Prepare Connection to connect database
    private Connection connect;
    private PreparedStatement prepare;
    private ResultSet result;

    private int userID;

    public int getUserID(){
        return userID;
    }

    //Drag and drop
    private double x;
    private double y;

    public void loginAdmin() {
        //username.setText("phongnhvp@gmail.com");
        //password.setText("3");
        Alert alert;
        String sql = "SELECT * FROM user_data WHERE username = ? AND password = ? AND password != ''";
        connect = Database.connectDB();
        try{
            assert connect != null;
            prepare = connect.prepareStatement(sql);
            prepare.setString(1, username.getText());
            prepare.setString(2, password.getText());

            result = prepare.executeQuery();

            if (username.getText().isEmpty() || password.getText().isEmpty())
            {
                alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error!");
                alert.setHeaderText(null);
                alert.setContentText("Please fill in the blank fields");
                alert.showAndWait();
            } else{
                if (result.next()){
                    userID = result.getInt("userID");
                    System.out.println(userID);

                    if (result.getString("spotify_email").equals("-1")){
                        alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Information Message!");
                        alert.setHeaderText(null);
                        alert.setContentText("Login Successfully");

                        toDashBoard();
                    } else {
                        LoginWithSpotify();
                    }

                }
                else{
                    alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error!");
                    alert.setHeaderText(null);
                    alert.setContentText("Wrong Username or Password");
                    alert.showAndWait();
                }
            }
        } catch(Exception e){e.printStackTrace(System.out);
        }
    }

    //TODO req new token for login locally

    private HelloController hello;

    public void setHello(HelloController hello){
        this.hello = hello;
    }

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

    public void toDashBoard() throws IOException {
        // Ghi dữ liệu vào file
        String filePath = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt";

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("1\n" + userID);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
        Parent root = fxmlLoader.load();
        dashboardController dashboard = fxmlLoader.getController();
        dashboard.setHello(hello);

        // Set the userID before calling the initialize() method
        dashboard.setUserID(userID);
        dashboard.setDashboard(dashboard);
        loginBtn.getScene().getWindow().hide();

        Stage stage = new Stage();
        Scene scene = new Scene(root);

        //AnchorPane sub_form = dashboard.getSup_form();

        root.setOnMousePressed((MouseEvent event) ->{
            x = event.getSceneX();
            y = event.getSceneY();
        });

        scene.setOnMouseDragged((MouseEvent event) ->{
            //if (isMouseInside) return;
            stage.setX(event.getScreenX() - x);
            stage.setY(event.getScreenY() - y);
        });


        stage.initStyle(StageStyle.TRANSPARENT);

        stage.setScene(scene);
        stage.show();
    }

    public Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void LoginWithSpotify() throws InterruptedException, IOException {
        String pythonScript = "getUserData.py";

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));

        Process process = processBuilder.start();

        process.waitFor();

        BufferedReader reader = new BufferedReader(new FileReader("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt"));
        String line = reader.readLine();
        //System.out.println(line);
        if (line == null){
            return;
        }
        int number = Integer.parseInt(line);

        //System.out.println(line);
        if (number == 0){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("You haven't registered yet");
            alert.showAndWait();
        } else {
            line = reader.readLine();
            userID = Integer.parseInt(line);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Successfully login");
            alert.showAndWait();

            toDashBoard();
        }
    }

    public void MoveToSignUp()
    {
        right_form1.setVisible(false);
        right_form2.setVisible(true);
    }

    public void MoveToLogin(){
        right_form2.setVisible(false);
        right_form1.setVisible(true);
    }

    private final String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";
    //private final String outputDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt";
    public void SignUpWithSpotify() throws IOException, InterruptedException {
        String pythonScript = "signUp.py";

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(pythonScript);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonDirectory));

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        System.out.println("Python script executed with exit code: " + exitCode);

        try {
            BufferedReader reader = new BufferedReader(new FileReader("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt"));
            String line = reader.readLine();
            int number = Integer.parseInt(line);
            if (number == 0){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("This email is already been registered");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(null);
                alert.setContentText("Successfully registered");
                alert.showAndWait();

                MoveToLogin();
            }

            FileWriter writer = new FileWriter("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt");

            // Ghi dữ liệu vào file
            //writer.write("");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

    }
    public void SignUpUser()
    {
        Alert alert;
        String sql = "INSERT INTO user_data (username,password,spotify_email) VALUES(?,?,'')";
        connect = Database.connectDB();

        try{
            if (Signup_password.getText().isEmpty()
                    ||Signup_username.getText().isEmpty()
                    ||Signup_reenter.getText().isEmpty()){
                alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error Message!");
                alert.setHeaderText(null);
                alert.setContentText("Please fill all blank fields");
                alert.showAndWait();
            }
            else if (Signup_reenter.getText().equals(Signup_password.getText()))
            {
                String sqlcheck = "SELECT username FROM user_data WHERE username = ?";
                PreparedStatement preparecheck;
                preparecheck = connect.prepareStatement(sqlcheck);
                preparecheck.setString(1,Signup_username.getText());

                result = preparecheck.executeQuery();
                if (result.next())
                {
                    alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error Message!");
                    alert.setHeaderText(null);
                    alert.setContentText("This username has already been used");
                    alert.showAndWait();
                }
                else {
                    prepare = connect.prepareStatement(sql);
                    prepare.setString(1, Signup_username.getText());
                    prepare.setString(2, Signup_password.getText());
                    prepare.executeUpdate();

                    alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("INFORMATION MESSAGE");
                    alert.setHeaderText(null);
                    alert.setContentText("You have successfully sign up \nProceed to Login?");
                    Optional<ButtonType> option =  alert.showAndWait();

                    assert option.orElse(null) != null;
                    if (option.orElse(null).equals(ButtonType.OK)){
                        MoveToLogin();
                    }
                }
            }
            else{
                alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("ERROR MESSAGE");
                alert.setHeaderText(null);
                alert.setContentText("Your re-enter password doesn't match the previous one");
                alert.showAndWait();
            }
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
    }
}