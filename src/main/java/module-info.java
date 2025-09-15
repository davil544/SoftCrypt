module com.dylanspcrepairs.softcrypt {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bouncycastle.lts.prov;

    opens com.dylanspcrepairs.softcrypt to javafx.fxml;
    exports com.dylanspcrepairs.softcrypt;
}