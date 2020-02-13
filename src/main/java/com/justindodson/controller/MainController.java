package com.justindodson.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class MainController implements Initializable {

    public MainController() {
    }

    public void initialize(URL location, ResourceBundle resources) {
        SerialPort[] commPorts = SerialPort.getCommPorts();
        for(SerialPort port : commPorts) {
            ports.getItems().add(port.getSystemPortName());
        }
        displayChart.setAnimated(false);
        displayChart.getXAxis().setLabel("Time");
        displayChart.getYAxis().setLabel("Measurements");
        displayChart.setTitle("Air Data");
        displayChart.getData().clear();

        borderPane.setCenter(displayChart);
//
    }
    private SerialPort activePort;
    private XYChart.Series<String, Number> temperature = new XYChart.Series<>();
    private XYChart.Series<String, Number> humidity = new XYChart.Series<>();
    private CategoryAxis xAxis = new CategoryAxis();
    private NumberAxis yAxis = new NumberAxis();
    private XYChart<String, Number> displayChart = new AreaChart<>(xAxis, yAxis);

    @FXML
    private ComboBox ports;
    @FXML
    private Button serialConnect;
    @FXML
    private Button readBtn;
    @FXML
    private BorderPane borderPane;

    @FXML
    public void connect(ActionEvent event) {

        if(serialConnect.getText().equalsIgnoreCase("connect")) {

            activePort = SerialPort.getCommPort(ports.getValue().toString());
            activePort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

            if (activePort.openPort()) {
                temperature.setName("Temperature (F)");
                humidity.setName("Humidity (%)");
                serialConnect.setText("Disconnect");
                ports.setDisable(true);
            }

               Thread thread = new Thread(() -> {
                   final Scanner inputStream = new Scanner(activePort.getInputStream());
                   AtomicInteger counter = new AtomicInteger();
                   while (inputStream.hasNext()) {
                       final Date now = new Date();
                       final SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
                       String tempString = null;
                       String humString = null;
                       String line1 = inputStream.nextLine();
                       String line2 = inputStream.nextLine();
                       String line3 = inputStream.nextLine();

                       if (line1.equalsIgnoreCase(" ")) {
                           tempString = line2;
                           humString = line3;
                       } else if (line2.equalsIgnoreCase(" ")) {
                           tempString = line3;
                           humString = line1;
                       } else if (line3.equalsIgnoreCase(" ")) {
                           tempString = line1;
                           humString = line2;
                       }

                       String finalTempString = tempString;
                       String finalHumString = humString;
                       Platform.runLater(() -> {

                           try {
                               System.out.println(displayChart.getData().size());
                               if(displayChart.getData().size() > 20) {
                                   temperature.getData().remove(0);
                                   humidity.getData().remove(0);
                               }
                               Double temp = Double.parseDouble(finalTempString);
                               Double hum = Double.parseDouble(finalHumString);
                               temperature.getData().add(new XYChart.Data<>(formatter.format(now), temp));
                               humidity.getData().add(new XYChart.Data<>(formatter.format(now), hum));
                               displayChart.getData().addAll(temperature, humidity);

                           }catch(Exception e) {};
                       });
                   }
                   inputStream.close();
               });
                thread.start();

        }
        else{
            activePort.closePort();
            serialConnect.setText("Connect");
            ports.setDisable(false);
        }
    }
}
