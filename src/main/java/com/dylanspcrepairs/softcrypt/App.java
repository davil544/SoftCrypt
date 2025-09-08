package com.dylanspcrepairs.softcrypt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import javafx.scene.control.Alert;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setTitle("SoftCrypt");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
    
    // This regex operation drops everything after / or \ to get a directory path from a string
    // This may cause issues in the root directory on Linux, tweak it if so
    protected static String parseDir(String msg){
        return msg.replaceAll("[\\\\/][^\\\\/]*$", "");
    }
    
    protected static void showAlert(Alert.AlertType alertType, String title, String heading, String msg){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(heading);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}