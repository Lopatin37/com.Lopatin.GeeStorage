import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;


import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class Client extends Application {
    private TextField loginTf;
    private TextField passwordTf;

    private Button getLogPassBtn;
    private Button downLoad;
    private Button upload;
    private Button delete;

    private static ObservableList<String> listOfFilesServer;
    private static ObservableList<String> listOfFilesClient;

    private ListView<String> listViewFilesServer;
    private ListView<String> listViewFilesClient;

    MultipleSelectionModel<String> lvServerSelectionModel;
    MultipleSelectionModel<String> lvClientSelectionModel;


    private String fileNameStr;
    private String loginStr;
    private String passwordStr;

    public static boolean isAuthorized;

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void init() throws Exception {
        isAuthorized = false;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        networkStarter.await();



        primaryStage.setTitle("GeeStorage");
        FlowPane rootNode = new FlowPane(10,10);
        rootNode.setAlignment(Pos.TOP_CENTER);
        Scene myScene = new Scene(rootNode, 500, 400);
        primaryStage.setScene(myScene);
        loginTf = new TextField();
        passwordTf = new TextField();
        getLogPassBtn = new Button("Log in");
        downLoad = new Button("DownLoad");
        upload = new Button("Upload");
        delete = new Button("Delete");

        listOfFilesServer = FXCollections.observableArrayList();
        listOfFilesClient = FXCollections.observableArrayList();

        listViewFilesServer = new ListView<>(listOfFilesServer);
        listViewFilesServer.setPrefSize(250,200);

        listViewFilesClient = new ListView<>(listOfFilesClient);
        listViewFilesClient.setPrefSize(250,200);

        lvServerSelectionModel = listViewFilesServer.getSelectionModel();
        lvClientSelectionModel = listViewFilesClient.getSelectionModel();

        lvServerSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                fileNameStr = null;
                fileNameStr = newValue;
            }
        });

        lvClientSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                fileNameStr = null;
                fileNameStr = newValue;
            }
        });


        loginTf.setPrefWidth(200);
        passwordTf.setPrefWidth(200);
        loginTf.setPromptText("Login");
        passwordTf.setPromptText("Password");
        primaryStage.setResizable(false);
        getLogPassBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(loginTf.getText().isEmpty() || passwordTf.getText().isEmpty()) {
                    return;
                }
                loginStr = loginTf.getText();
                passwordStr = passwordTf.getText();
                Operations.authorization(loginStr,passwordStr,Network.getInstance().getCurrentChannel(),future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        System.out.println("Файл успешно передан");
                    }
                });
                try{
                    Thread.sleep(3000);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(Client.isAuthorized){
                    rootNode.getChildren().removeAll(loginTf, passwordTf, getLogPassBtn);
                    rootNode.getChildren().addAll(listViewFilesClient,listViewFilesServer, downLoad, upload, delete);
                }

            }
        });
        downLoad.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(fileNameStr == null) return;
                try {
                    InboundHandler.fileName = fileNameStr;
                    Operations.download(lvServerSelectionModel.getSelectedItem(), loginStr,
                            Network.getInstance().getCurrentChannel(), future -> {
                                if (!future.isSuccess()) {
                                    future.cause().printStackTrace();
                                }
                                if (future.isSuccess()) {
                                    System.out.println("Файл успешно передан");
                                }
                            });
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        upload.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

            }
        });
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

            }
        });
        rootNode.getChildren().addAll(loginTf, passwordTf, getLogPassBtn);

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {

    }

    public static void setListViewFilesServer(String name) {
        if(!listOfFilesServer.contains(name)) {
            listOfFilesServer.add(name);
        }
    }
//
//
//        final int dataSize = 1024 * 1024 * 2;
//
//        try (Socket socket = new Socket("localhost", 8080);
//             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
//             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
//
//            dataOutputStream.write((byte)10);
//            byte[] logPasBytes = "client1 12345".getBytes();
//            dataOutputStream.write(logPasBytes.length);
//            System.out.println("Sent Log and pas length " + logPasBytes.length);
//            dataOutputStream.write(logPasBytes);
//            byte answer = dataInputStream.readByte();
//            if(answer)
//
//            dataOutputStream.write((byte)30);
//            byte[] loginBytes = "client1".getBytes();
//            dataOutputStream.write(loginBytes.length);
//            dataOutputStream.write(loginBytes);
//            byte answer = dataInputStream.readByte();
//            if(answer != (byte)25) {
//                System.out.println("Invalid byte");
//                return;
//            }
//            byte[] fileNameBytes = "text.txt".getBytes();
//            dataOutputStream.write(fileNameBytes.length);
//            dataOutputStream.write(fileNameBytes);
//            byte[] bytes = new byte[8];
//            byte[] frame = new byte[dataSize];
//            dataInputStream.read(bytes);
//            long fileSize = ((bytes[0] << 56) + ((bytes[1] & 0xFF) << 48) + ((bytes[2] & 0xFF) << 40) + ((bytes[3] & 0xFF) << 32) + ((bytes[4] & 0xFF) << 24) +
//                            ((bytes[5] & 0xFF) << 16) + ((bytes[6] & 0xFF) << 8) + (bytes[7] & 0xFF));
//            System.out.println("Files size is: " + fileSize);
//
//
//            BufferedOutputStream write = new BufferedOutputStream(new FileOutputStream("clients\\client1\\_" + new String(fileNameBytes)), 1024 * 1024 * 2);
//
//            long parts = fileSize/dataSize;
//            while(parts > 0) {
//                dataInputStream.read(frame,0,frame.length);
//                write.write(frame);
//                write.flush();
//                parts--;
//            }
//            if(fileSize%dataSize != 0) {
//                frame = new byte[new Long(fileSize%dataSize).intValue()];
//                dataInputStream.read(frame);
//                write.write(frame);
//                write.flush();
//                System.out.println(new String(frame));
//
//            }
//            write.close();
//            System.out.println("File received");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
