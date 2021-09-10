module TextChat {
    requires transitive javafx.controls;
    requires java.sql;
    exports roomChat.server;
    exports roomChat.user;
}