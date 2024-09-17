package com.example.mrs;

import com.example.mrs.dataModel.ArtistData;
import com.example.mrs.dataModel.SongData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
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
import javafx.scene.control.ScrollPane;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class musicSupportController implements Initializable {
    @FXML
    private VBox artist_section;

    @FXML
    private VBox bonus_section;

    @FXML
    private VBox recommend_section;

    @FXML
    private Label track_artist;

    @FXML
    private ImageView track_img;

    @FXML
    private Label track_name;

    @FXML
    private Label track_year;

    musicArtistController musicArt;

    public void setMusicArt(musicArtistController musicArt) {
        this.musicArt = musicArt;
    }

    public double getX(){
        return main.getParent().getLayoutX();
    }

    public double getY(){
        return main.getParent().getLayoutY();
    }

    private final String pythonDirectory = "D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\pythonProgramm";

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

    public List<SongData> runRecommendSong(List<SongData> vectors) throws InterruptedException, IOException, ParseException {
        //if(!check()) return new ArrayList<>();
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
                    if (songList.size() >= 5) break;
                    if (!line.isEmpty()) {
                        switch (i % 5) {
                            case 0 -> {
                                year = line;
                                //System.out.println("year: " + year);
                            }
                            case 1 -> {
                                //byte[] songBytes = "If I Ain\xe2\x80\x99t Got You - Live".getBytes(StandardCharsets.UTF_8);
                                //String decodedSong = new String(songBytes, StandardCharsets.UTF_8);
                                //System.out.println(decodedSong);
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

    private List<ArtistData> artistList = new ArrayList<>();
    private List<ArtistData> relativeList = new ArrayList<>();

    public List<List<SongData>> runFindSongRelative(SongData vector) throws InterruptedException, IOException, ParseException {
        if (vector == null) return new ArrayList<>();
        StringBuilder cmd = new StringBuilder("\"");
        cmd.append(vector.getName());
        cmd.append("\"");
        //cmd.delete(cmd.length() - 2, cmd.length());
        //cmd.append("]\"");
//        System.out.println(cmd);
        String pythonScript = "getSongArtist.py";
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
        List<List<SongData>> songList = new ArrayList<>();
        artistList.clear();
        relativeList.clear();

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread outputThread = new Thread(() -> {
            try {
                String line;
                int i = 0;
                String name = "", artist= "", cover_url= "", external_url = "",year= "", id = "", follower = "";
                int count = 0, c2 = 0;
                List<SongData> currentList = new ArrayList<>();
                while ((line = outputReader.readLine()) != null) {
                    // Process the output as needed
                    if (!line.isEmpty()) {
                        if (line.equals("2!")) {
                            c2++;
                            i = 0;
                            continue;
                        }
                        if (c2 == 0) {
                            if (line.equals("1!")) {
                                i = 0;
                                //System.out.println("New Artist:");
                                if (count != 0) {
                                    songList.add(currentList);
                                }
                                count++;
                                currentList = new ArrayList<>();
                                continue;
                            }
                            switch (i % 5) {
                                case 0 -> {
                                    year = line;
                                    //System.out.println("year: " + year);
                                }
                                case 1 -> {
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
                                    currentList.add(song.get());
                                }
                            }
//                            System.out.print(i);
//                            System.out.println(" : " + line);
                            i++;
                        }else{
                            switch (i % 5) {
                                case 0 -> {
                                    name = line;
                                    //System.out.println("name: " + line);
                                }
                                case 1 -> {
                                    cover_url = line;
                                    //System.out.println("cover_url:" + line);
                                }
                                case 2 -> {
                                    id = line;
                                    //System.out.println("id: " + id);
                                }
                                case 3 -> {
                                    //System.out.println("url:" + line);
                                    external_url = line;
                                }
                                case 4 -> {
                                    follower = line;
                                    //System.out.println("follower: " + follower);
                                    if (c2 == 1) {
                                        artistList.add(new ArtistData(name, cover_url,
                                                id, external_url, Integer.parseInt(follower)));
                                    }
                                    else{
                                        relativeList.add(new ArtistData(name, cover_url,
                                                id, external_url, Integer.parseInt(follower)));
                                    }
                                }
                            }
                            i++;
                        }
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
    private List<SongData> recommendList;

    List<SongData> counter = new ArrayList<>();

    public List<SongData> getCounter() {
        return counter;
    }

    public void addCounter(SongData song){
        counter.add(song);
    }

    public void setRecommendSection(SongData song){
        recommend_section.getChildren().clear();
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setProgress(-1);
        indicator.setPrefSize(120,120);

        recommend_section.getChildren().add(indicator);
        Runnable task = () -> {
            List<SongData> vectors  = new ArrayList<>();
            vectors.add(song);
            try {
                recommendList = runRecommendSong(vectors);
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
                            addCounter(song);
                            Init(item);
                        }
                    });

                    ContextMenu contextMenu = new ContextMenu();
                    //MenuItem item1 = new MenuItem("Play");
                    MenuItem item2 = new MenuItem("Go to Spotify Song");
                    MenuItem item3 = new MenuItem("Copy Song Link");


//                    playItems = musicPlayer.getPlayItems();
//                    SongData chosen = null;
//                    int check = 0;
//                    for (SongData newItem : playItems){
//                        if(song.getName().equals(newItem.getName())) {
//                            check = 1;
//                            chosen = newItem;
//                            break;
//                        }
//                    }
//
//                    if (check == 1){
//                        SongData finalThisItem = chosen;
//                        item1.setOnAction(event -> {
//                            musicPlayer.setNewMedia(finalThisItem);
//                            dashboard.mpBtn();
//                            hide();
//                        });
//                    }

                    item2.setOnAction(event -> {
                        addCounter(song);
                        toUri(recommendList.get(recommend_section.getChildren().indexOf(component)).getPath());
                    });

                    item3.setOnAction(event -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        addCounter(song);
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

                    Label name = tmp1;
                    Label artist = tmp2;

                    component.setOnMouseEntered(mouseEvent -> {
                        component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                        name.setTextFill(Color.WHITE);
                        artist.setTextFill(Color.WHITE);
                    });

                    component.setOnMouseExited(mouseEvent -> {
                        component.setStyle("-fx-background-color: transparent");
                        name.setTextFill(Color.BLACK);
                        artist.setTextFill(Color.BLACK);
                    });
                    if (count == 5) break;
                }
            });
        };
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        globalExecutor = executorService;
        executorService.submit(task);
        executorService.shutdown();
    }

    ExecutorService globalExecutor;

    public void toUri(String url){
        try {
            URI uri = new URI(url);
            Desktop desktop = Desktop.getDesktop();

            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
                //System.out.println("URL opened successfully.");
            } else {
                //System.out.println("Desktop browsing is not supported.");
            }
        } catch (Exception e) {
            //System.out.println("Error occurred while opening the URL: " + e.getMessage());
        }
    }

    private List<String> artists = new ArrayList<>();
    private List<List<SongData>> songPerArtist = new ArrayList<>();

    @FXML
    private Button artist_btn;

    @FXML
    private ImageView artist_img;

    public void setContextMenu(Button btn, ArtistData artistData){
        ContextMenu contextMenu1 = new ContextMenu();

        MenuItem menuItem1 = new MenuItem("Find artist");

        menuItem1.setOnAction(event -> {
            musicArt.unHide();
            musicArt.Init(artistData);
        });

        contextMenu1.getItems().add(menuItem1);

        btn.setContextMenu(contextMenu1);
    }

    public void setImgShape(ImageView img){
        Circle circle = new Circle();
        circle.setCenterX(img.getX() + img.getFitHeight()/2);
        circle.setCenterY(img.getY() + img.getFitWidth()/2);
        circle.setRadius(img.getFitHeight()/2 - 1);

        circle.setStroke(Color.BLACK);
        img.setClip(circle);
    }

    public void setImgShapeRectangle(ImageView img){
        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
        rectangle.setHeight(img.getFitHeight());
        rectangle.setWidth(img.getFitWidth());
        rectangle.setArcHeight(5);
        rectangle.setArcWidth(5);

        rectangle.setLayoutX(img.getX());
        rectangle.setLayoutY(img.getY());

        img.setClip(rectangle);
    }

    public void setArtistSection(SongData song){
        artist_section.getChildren().clear();
        bonus_section.getChildren().clear();
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setProgress(-1);
        indicator.setPrefSize(120,120);

        artist_section.getChildren().add(indicator);

        Runnable task = () ->{
            try {
                artists = List.of(song.getArtists().split(", "));
                songPerArtist = runFindSongRelative(song);
//                for (ArtistData item : artistList)
//                    System.out.println(item.getName());
//                for (ArtistData item : relativeList)
//                    System.out.println(item.getName());
            } catch (InterruptedException | ParseException | IOException e) {
                throw new RuntimeException(e);
            }

            Platform.runLater(() ->{
                artist1.setText(artists.get(0));
                artist_section.getChildren().clear();
                setContextMenu(artist_btn, artistList.get(0));
                setImgShape(artist_img);
                artist_img.setImage(new Image(artistList.get(0).getCover_url()));
                int count = 0;
                for (SongData item: songPerArtist.get(0)){
                    HBox component = createHBox(item);
                    count++;
                    artist_section.getChildren().add(component);

                    component.setOnMouseClicked(mouseEvent -> {
                        if (mouseEvent.getClickCount() == 2){
                            if (!globalExecutor.isShutdown()) globalExecutor.shutdownNow();
                            Init(songPerArtist.get(1).get(artist_section.getChildren().indexOf(component)));
                        }
                    });

                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem item1 = new MenuItem("Play");
                    MenuItem item2 = new MenuItem("Go to Spotify Song");
                    MenuItem item3 = new MenuItem("Copy Song Link");

                    item1.setOnAction(event -> {

                    });

                    item2.setOnAction(event -> {
                        toUri(songPerArtist.get(1).get(artist_section.getChildren().indexOf(component)).getPath());
                    });

                    item3.setOnAction(event -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();

                        ClipboardContent content = new ClipboardContent();
                        content.putString(item.getPath());
                        clipboard.setContent(content);
                    });

                    contextMenu.getItems().addAll(item1,item2,item3);

                    tmp.setContextMenu(contextMenu);
                    tmp.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            contextMenu.show(tmp, event.getScreenX(), event.getScreenY());
                        }
                    });

                    Label name = tmp1;
                    Label artist = tmp2;

                    component.setOnMouseEntered(mouseEvent -> {
                        component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                        name.setTextFill(Color.WHITE);
                        artist.setTextFill(Color.WHITE);
                    });

                    component.setOnMouseExited(mouseEvent -> {
                        component.setStyle("-fx-border-color: TRANSPARENT");
                        name.setTextFill(Color.BLACK);
                        artist.setTextFill(Color.BLACK);
                    });
                    if (count == 5) break;
                }

//                System.out.println(songPerArtist.size());
                for (int n = 1 ; n < songPerArtist.size() ; n++){
                    ArtistData artistData = artistList.get(n);
                    List<SongData> items = songPerArtist.get(n);
//                    System.out.println(artists.get(n) + "----------------------------------------");
//                    for (SongData item:items){
//                        System.out.println(item.getName());
//                    }
                    HBox mainArtist = new HBox();
                    mainArtist.setAlignment(Pos.CENTER_LEFT);

                    ImageView imgView = new ImageView(new Image(artistData.getCover_url()));
                    imgView.setFitWidth(40);
                    imgView.setFitHeight(40);

                    setImgShape(imgView);

                    Button artistBtn = new Button("...");
                    setContextMenu(artistBtn, artistData);
                    artistBtn.setStyle("-fx-background-color: transparent");
                    mainArtist.setPrefWidth(500);

                    VBox perArtist = new VBox();
                    Label get = new Label("Popular Release");
                    get.setFont(Font.font("System",12));

                    Label artistName = new Label(artistData.getName());
                    artistName.setFont(Font.font("System", 18));

                    mainArtist.setSpacing(10);

                    VBox nameHolder= new VBox(get, artistName);
                    nameHolder.setPrefWidth(500);

                    HBox main = new HBox();
                    main.setSpacing(5);
                    int kay = n;
                    bonus_section.getChildren().add(perArtist);
                    mainArtist.getChildren().addAll(imgView,nameHolder, artistBtn);
                    perArtist.getChildren().addAll(mainArtist,main);
                    perArtist.setSpacing(10);
                    //mainArtist.getChildren().addAll(imgView,perArtist,artistBtn);

                    for (SongData item: items){
                        //System.out.println(count);
                        VBox component = createVBox(item);
                        count++;
                        main.getChildren().add(component);

                        component.setOnMouseClicked(mouseEvent -> {
                            if (mouseEvent.getClickCount() == 2){
                                if (!globalExecutor.isShutdown()) globalExecutor.shutdownNow();
                                Init(songPerArtist.get(kay).get(main.getChildren().indexOf(component)));
                            }
                        });

                        ContextMenu contextMenu = new ContextMenu();
                        MenuItem item1 = new MenuItem("Play");
                        MenuItem item2 = new MenuItem("Go to Spotify Song");
                        MenuItem item3 = new MenuItem("Copy Song Link");

                        item1.setOnAction(event -> {

                        });

                        item2.setOnAction(event -> {
                            toUri(songPerArtist.get(kay).get(main.getChildren().indexOf(component)).getPath());
                        });

                        item3.setOnAction(event -> {
                            Clipboard clipboard = Clipboard.getSystemClipboard();

                            ClipboardContent content = new ClipboardContent();
                            content.putString(item.getPath());
                            clipboard.setContent(content);
                        });

                        contextMenu.getItems().addAll(item1,item2,item3);

                        tmp.setContextMenu(contextMenu);
                        tmp.setOnMouseClicked(event -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                contextMenu.show(tmp, event.getScreenX(), event.getScreenY());
                            }
                        });

                        Label name = tmp3;
                        Label artist = tmp4;

                        component.setOnMouseEntered(mouseEvent -> {
                            component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                            name.setTextFill(Color.WHITE);
                            artist.setTextFill(Color.WHITE);
                        });

                        component.setOnMouseExited(mouseEvent -> {
                            component.setStyle("-fx-background-color: TRANSPARENT");
                            name.setTextFill(Color.BLACK);
                            artist.setTextFill(Color.BLACK);
                        });
                    }
                }

                VBox relativeMain = new VBox();
                HBox relative = new HBox();
                Label fan = new Label("Fan also like");
                fan.setFont(Font.font("System", FontWeight.MEDIUM, 18));

                relativeMain.getChildren().addAll(fan, relative);
                relativeMain.setSpacing(10);
                relative.setSpacing(10);
                relative.setPadding(new Insets(0,0,0,10));

                bonus_section.getChildren().add(relativeMain);

                for (ArtistData artistData: relativeList){
                    VBox component = createVBox(artistData);
                    count++;
                    relative.getChildren().add(component);

                    component.setOnMouseClicked(mouseEvent -> {
                        if (mouseEvent.getClickCount() == 2){
                            musicArt.unHide();
                            musicArt.Init(artistData);
                        }
                    });

                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem item1 = new MenuItem("Play");
                    MenuItem item2 = new MenuItem("Go to Spotify Song");
                    MenuItem item3 = new MenuItem("Copy Song Link");

                    item1.setOnAction(event -> {

                    });

                    item2.setOnAction(event -> {
                        toUri(artistData.getExternal_url());
                    });

                    item3.setOnAction(event -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();

                        ClipboardContent content = new ClipboardContent();
                        content.putString(artistData.getExternal_url());
                        clipboard.setContent(content);
                    });

                    contextMenu.getItems().addAll(item1,item2,item3);

                    Label name = tmp3;
                    Label artist = tmp4;

                    component.setOnMouseEntered(mouseEvent -> {
                        component.setStyle("-fx-background-color: #bcbcbc; \n -fx-background-radius: 4px 4px 4px 4px;");
                        name.setTextFill(Color.WHITE);
                        artist.setTextFill(Color.WHITE);
                    });

                    component.setOnMouseExited(mouseEvent -> {
                        component.setStyle("-fx-background-color: TRANSPARENT");
                        name.setTextFill(Color.BLACK);
                        artist.setTextFill(Color.BLACK);
                    });
                }
            });
        };

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
    }
    @FXML
    private Label artist1;



    public Label tmp3, tmp4;

    public VBox createVBox(ArtistData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(120);
        vbox.setPrefHeight(150);
        vbox.setAlignment(Pos.CENTER);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(110);
        imageView.setFitWidth(110);
        setImgShape(imageView);

        Label name = new Label(song.getName());
        Label artist = new Label("Artist");

        name.setFont(Font.font("System", FontWeight.BOLD, 12));

        name.setTooltip(new Tooltip(song.getName()));
        //artist.setTooltip(new Tooltip(song.getArtists()));
        vbox.getChildren().add(imageView);
        vbox.getChildren().add(name);
        vbox.getChildren().add(artist);
        tmp3 = name;
        tmp4 = artist;
        return vbox;
    }

    public VBox createVBox(SongData song){
        VBox vbox = new VBox();
        vbox.setPrefWidth(120);
        vbox.setPrefHeight(150);
        vbox.setAlignment(Pos.CENTER);

        Image img = new Image(song.getCover_url());
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(110);
        imageView.setFitWidth(110);
        setImgShapeRectangle(imageView);

        Label name = new Label(song.getName());
        Label artist = new Label(song.getArtists());

        name.setFont(Font.font("System", FontWeight.BOLD, 12));

        name.setTooltip(new Tooltip(song.getName()));
        artist.setTooltip(new Tooltip(song.getArtists()));
        vbox.getChildren().add(imageView);
        vbox.getChildren().add(name);
        vbox.getChildren().add(artist);
        tmp3 = name;
        tmp4 = artist;
        return vbox;
    }

    ResultSet result;

    private List<SongData> playItems = new ArrayList<>();
    private List<String> external = new ArrayList<>();

    public void getLocalSongData(){
        String sql = "SELECT * FROM local_song_data";
        connect = Database.connectDB();
        try{
            assert connect != null;
            prepare = connect.prepareStatement(sql);
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
    }
    @FXML
    private Button play_btn;

    @FXML
    private Button navigate_btn;

    private musicPlayerController musicPlayer;

    public void setMusicPlayer(musicPlayerController musicPlayer) {
        this.musicPlayer = musicPlayer;
    }

    public dashboardController dashboard;

    public void setDashboard(dashboardController dashboard) {
        this.dashboard = dashboard;
    }

    SongData currentChoice = null;

    public void setMainPaneColor(Image image){
        //Image image = new Image("path_to_your_image.jpg");

        // Create a HashMap to store color frequencies
        //Map<Color, Integer> colorFrequencies = new HashMap<>();

        // Read the pixel colors and count their frequencies
        PixelReader pixelReader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        List<Color> colorList = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                colorList.add(color);
            }
        }

        // Find the color with the highest frequency
        //Color mostFrequentColor = Color.BLACK; // Default color if no pixels are present
        Random random = new Random();
        Color mostFrequentColor = colorList.get(random.nextInt(colorList.size()));
        String colorCode = String.format("#%02X%02X%02X",
                (int) (mostFrequentColor.getRed() * 255),
                (int) (mostFrequentColor.getGreen() * 255),
                (int) (mostFrequentColor.getBlue() * 255));
        System.out.println(colorCode);
        mainPane.setStyle("-fx-background-color:" +colorCode);
    }

    public void Init(SongData song){
        if (currentChoice!= null && currentChoice.equals(song)) return;
        currentChoice = song;
        try{
            insertIntoDB(song);
        }catch (Exception e){
            e.printStackTrace(System.out);
        }

        playItems = musicPlayer.getPlayItems();
        SongData chosen = null;
        int check = 0;
        for (SongData item : playItems){
            if(song.getName().equals(item.getName())) {
                check = 1;
                chosen = item;
                break;
            }
        }

        if (check == 1){
            SongData finalThisItem = chosen;
            play_btn.setOnAction(event -> {
                musicPlayer.setNewMedia(finalThisItem);
                dashboard.mpBtn();
                hide();
            });
        }

        navigate_btn.setOnAction(event -> {
            toUri(song.getPath());
        });
        scroll.setVvalue(0);
        musicArt.hide();
        main.getParent().setLayoutX(musicArt.getX());
        main.getParent().setLayoutY(musicArt.getY());

        Image img = new Image(song.getCover_url());
        track_img.setImage(img);
        setMainPaneColor(img);
        track_name.setText(song.getName());
        track_artist.setText(song.getArtists());
        track_year.setText(song.getYear());

        track_name.setTooltip(new Tooltip(song.getName()));
        track_artist.setTooltip(new Tooltip(song.getArtists()));
        track_year.setTooltip(new Tooltip(song.getYear()));
        //recommend_section.getChildren().clear();

        setRecommendSection(song);
        setArtistSection(song);
    }

    Connection connect;
    PreparedStatement prepare;

    private int userID;

    public void setUserID(int userID) {
        this.userID = userID;
    }

    @FXML
    private HBox mainPane;

    public void insertIntoDB(SongData song) throws SQLException {
        String name = song.getName().replace("'","\\'");
        name = name.replace("\"","\\\"");

        String sql = "DELETE FROM searchdata WHERE user = '" + userID + "' " +
                "AND name = ? AND type = 'song'";

        connect = Database.connectDB();
        assert connect != null;

        prepare = connect.prepareStatement(sql);
        prepare.setString(1,name.replace("\\\\","\\"));
//        System.out.println(prepare);
        prepare.executeUpdate();

        sql = "INSERT INTO searchdata (name, cover_url, external_url, user, type, artist_id, year_follower) " +
                "VALUES (?,?,?,?,'song',?,?)";

        prepare = connect.prepareStatement(sql);
        prepare.setString(1, name);
        prepare.setString(2, song.getCover_url());
        prepare.setString(3, song.getPath());
        prepare.setString(4, String.valueOf(userID));
        //prepare.setString(5, "'artist'");
        prepare.setString(5, song.getArtists());
        prepare.setString(6, song.getYear());

//        System.out.println(prepare);
        prepare.executeUpdate();
    }

    public HBox createHBox(SongData song){
        HBox main = new HBox();
        main.setSpacing(5);
        ImageView img = new ImageView(new Image(song.getCover_url()));
        img.setFitHeight(50);
        img.setFitWidth(50);

        setImgShapeRectangle(img);

        VBox metaData = new VBox();
        Label name = new Label(song.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label artist = new Label(song.getArtists());
        artist.setFont(Font.font("System",14));

        tmp1 = name;
        tmp2 = artist;

        metaData.getChildren().addAll(name,artist);
        main.getChildren().addAll(img,metaData);
        Button button = new Button("...");
        button.setStyle("-fx-background-color: transparent; \n -fx-cursor: hand");
        main.getChildren().add(button);
        tmp = button;

        return main;
    }

    private Button tmp;
    private Label tmp1, tmp2;

    @FXML
    private AnchorPane main;

    public void hide(){
        main.getParent().setVisible(false);
    }

    public void unHide(){
        main.getParent().setVisible(true);
    }

    @FXML
    private ScrollPane scroll;

    //TODO add color to pane
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //setImgShapeRectangle(track_img);
        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();
        rectangle.setHeight(track_img.getFitHeight());
        rectangle.setWidth(track_img.getFitWidth());
        rectangle.setArcHeight(20);
        rectangle.setArcWidth(20);

        rectangle.setLayoutX(track_img.getX());
        rectangle.setLayoutY(track_img.getY());

        track_img.setClip(rectangle);
        //getLocalSongData();
    }
}
