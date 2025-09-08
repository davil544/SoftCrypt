module com.dylanspcrepairs.softcrypt {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.dylanspcrepairs.softcrypt to javafx.fxml;
    exports com.dylanspcrepairs.softcrypt;
}
