package com.dylanspcrepairs.softcrypt;

import static com.dylanspcrepairs.softcrypt.App.parseDir;
import static com.dylanspcrepairs.softcrypt.App.showAlert;
import static com.dylanspcrepairs.softcrypt.Crypto.decryptFile;
import static com.dylanspcrepairs.softcrypt.Crypto.encryptFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class PrimaryController {
    @FXML
    private ProgressBar encProgressBar, decProgressBar;
    
    @FXML
    private Label lblEncFileName, lblDecFileName;
    
    @FXML
    private TextField txtEncOutputDir, txtEncKey, txtDecOutputDir, txtDecKey;
    
    private File selectedFileEnc = null, selectedFileDec = null;
    
    private final String select = "Select a file";
    
    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }
    
    @FXML
    private void passwordVisibilityToggle() {
        // TODO: Make this work somehow
        txtEncKey.setVisible(true);
    }
    
    private void initAllTabs(){
        // Resets encrypt tab
        txtEncOutputDir.setPromptText("");
        if (txtEncOutputDir.getText().equals(parseDir(lblEncFileName.getText()))){
            txtEncOutputDir.setText("");
        }
        lblEncFileName.setText(select);
        
        // Resets decrypt tab
        txtDecOutputDir.setPromptText("");
        if (txtDecOutputDir.getText().equals(parseDir(lblDecFileName.getText()))){
            txtDecOutputDir.setText("");
        }
        lblDecFileName.setText(select);
        
        // Nulls out file selection
        selectedFileEnc = null;
        selectedFileDec = null;
    }
    
    @FXML
    private void encrypt() throws Exception {
        encProgressBar.setProgress(0);
        String password = txtEncKey.getText();
        if (selectedFileEnc != null){
            encProgressBar.setProgress(0.1);
            System.out.println("toPath for Selected File: " + selectedFileEnc.toPath());
        
            encProgressBar.setProgress(0.2);
            Path inputFile = selectedFileEnc.toPath();
        
            encProgressBar.setProgress(0.3);
            System.out.println("Passkey Entered: " + password);
        
            // TODO: Add check to see if file exists already, verify content if so
            String outputDir = txtEncOutputDir.getText();
            if (outputDir.equals("")){
                outputDir = txtEncOutputDir.getPromptText();
            }
            System.out.println("Output Directory: " + outputDir);
            try{
                encProgressBar.setProgress(0.5);
                boolean successful = encryptFile(selectedFileEnc, outputDir , txtEncKey.getText().toCharArray());
                if (successful){
                    encProgressBar.setProgress(1.0);
                    showAlert(AlertType.INFORMATION, "Success","Encryption Successful!", "You can find your new file at " + outputDir + File.separator + selectedFileEnc.getName() + ".enc");
                }
                else {
                    encProgressBar.setProgress(0.0);
                    showAlert(AlertType.INFORMATION, "Error","Encryption Failed", "Something went wrong, please try again!");
                }
            }
            catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                showAlert(AlertType.ERROR, "Error","Encryption Failed", ex.getMessage());
            }
        }
        else {
            showAlert(AlertType.INFORMATION, "Error","Dude", "No File Selected");
        }
    }
    
    @FXML
    private void decrypt() throws Exception {
        decProgressBar.setProgress(0);
        String password = txtDecKey.getText();

        if (selectedFileDec != null){
            decProgressBar.setProgress(0.1);
            System.out.println("Encrypted file to decrypt: " + selectedFileDec.toPath());

            decProgressBar.setProgress(0.3);
            Path encryptedFile = selectedFileDec.toPath();

            decProgressBar.setProgress(0.5);
            System.out.println("Passkey Entered: " + password);
            
            String outputDir = txtDecOutputDir.getText();
            if (outputDir.equals("")){
                outputDir = txtDecOutputDir.getPromptText();
            }
            System.out.println("Output Directory: " + outputDir);
            
            try{
                boolean successful = decryptFile(selectedFileDec, outputDir, txtDecKey.getText().toCharArray());
                if (successful){
                    // File extension is stripped since we can't access the output filename from here
                    // Checks if ".enc" is at the end of the filename, strips it out if so for status updates
                    String outputFile = selectedFileDec.getName();
                    if (outputFile.endsWith(".enc")){
                        int lastDotEncIndex = outputFile.lastIndexOf(".enc");

                        if (lastDotEncIndex != -1 && lastDotEncIndex == outputFile.length() - 4){
                            outputFile = outputFile.substring(0, lastDotEncIndex);
                        }
                    }
                    
                    encProgressBar.setProgress(1.0);
                    showAlert(AlertType.INFORMATION, "Success","Decryption Successful!", "You can find your new file at " + outputDir + File.separator + outputFile);
                }
                else{
                    encProgressBar.setProgress(0.0);
                    showAlert(AlertType.INFORMATION, "Error","Encryption Failed", "Something went wrong, please try again!");
                }
            }
            catch (Exception ex){
                System.out.println("Error: " + ex.getMessage());
                showAlert(AlertType.ERROR, "Error","Deryption Failed", ex.getMessage());
            }

            decProgressBar.setProgress(1.0);
        }
        else{
            showAlert(AlertType.INFORMATION, "Error", "Dude", "No File Selected");
        }
    }
    
    @FXML
    private void handleEncFileSelection(ActionEvent event){
        handleFileSelection(event, "Encrypt");
    }
    
    @FXML
    private void handleDecFileSelection(ActionEvent event){
        handleFileSelection(event, "Decrypt");
    }
    
    private int handleFileSelection(ActionEvent event, String action) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(select);

        // Gets the Stage from the button's scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        if (action.equalsIgnoreCase("Encrypt")){
            // Erases decryption key in other tab if present
            txtDecKey.setText("");
            
            // Sets the file in the encrypt ui
            selectedFileEnc = fileChooser.showOpenDialog(stage);
            if (selectedFileEnc != null){
                System.out.println("Selected file: " + selectedFileEnc.getAbsolutePath());
                lblEncFileName.setText(selectedFileEnc.getAbsolutePath());
                txtEncOutputDir.setPromptText(selectedFileEnc.getParent());
                if (txtEncOutputDir.getText().equals("")){
                    txtEncOutputDir.setText(selectedFileEnc.getParent());
                }
            
                System.out.println("File Path: " + parseDir(lblEncFileName.getText()));
            }
            else {
                System.out.println("File selection cancelled.");
                initAllTabs();
            }
        }
        else if (action.equalsIgnoreCase("Decrypt")){
            // Erases encryption key in other tab if present
            txtEncKey.setText("");
            
            // set file in decrypt ui here
            selectedFileDec = fileChooser.showOpenDialog(stage);
            if (selectedFileDec != null){
                System.out.println("Selected file: " + selectedFileDec.getAbsolutePath());
                lblDecFileName.setText(selectedFileDec.getAbsolutePath());
                txtDecOutputDir.setPromptText(selectedFileDec.getParent());
                if (txtDecOutputDir.getText().equals("")){
                    txtDecOutputDir.setText(selectedFileDec.getParent());
                }
            
                System.out.println("File Path: " + parseDir(lblDecFileName.getText()));

            }
            else {
                System.out.println("File selection cancelled.");
                initAllTabs();
            }
        }
        else {
            initAllTabs();
            showAlert(AlertType.ERROR, "Error", "Something went wrong!", "Please report this to the developer");
            return 1;
        }
        
        return 0;
    }
    
    @FXML
    private void selectEncDir(ActionEvent event){
        handleDirSelection(event, "Encrypt");
    }
    
    @FXML
    private void selectDecDir(ActionEvent event){
        handleDirSelection(event, "Decrypt");
    }
    
    private void handleDirSelection(ActionEvent event, String action){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a directory");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null){
            System.out.println("Selected directory: " + selectedDirectory.getAbsolutePath());
            if (action.equalsIgnoreCase("Encrypt")){
                txtEncOutputDir.setText(selectedDirectory.getAbsolutePath());
            }
            else {
                txtDecOutputDir.setText(selectedDirectory.getAbsolutePath());
            }
        }
        else {
            System.out.println("Folder selection cancelled.");
        }
    }
}