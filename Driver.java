import java.awt.*;
import java.awt.event.*;
import java.util.Scanner;
import javax.swing.*;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.fazecast.jSerialComm.*;
import java.beans.PropertyChangeListener;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;

/**
 *
 * @author Samuel
 */

public abstract class Driver extends JPanel implements PropertyChangeListener {

    public static SerialPort userPort;
    public static int time1 = 0, time2 = 0;
    public static double voltage, salinity, pressure, pressBaseVal = 0;
    public static boolean graphingBoo, readPressBase = false, toggleBase = false;

    public static void main(String[] args) {

        //create the Frame
        JFrame myFrame = new JFrame();
        myFrame.setTitle("Salinity and Pressure Graph");
        myFrame.setSize(1200, 800);
        myFrame.setLayout(new BorderLayout());
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //create a drop-down box for COM ports
        JComboBox<String> portList = new JComboBox<>();
        //button to connect to the selected port
        JButton connectButton = new JButton("Connect");
        //start reading the serial port
        JButton startButton = new JButton("Start");
         //stop graphing 
        JButton stopButton = new JButton("Stop");
        //reset the graph
        JButton resetButton = new JButton("Reset");
        //reads a base pressure
        JButton pressButton = new JButton("Pressure Base: OFF");
        
        //create top buttonPanel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(portList);
        buttonPanel.add(connectButton);
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(pressButton);
        myFrame.add(buttonPanel, BorderLayout.NORTH);

        //create graph and label axis
        XYSeries series1 = new XYSeries("Salinity");//create series and datasets
        XYSeries series2 = new XYSeries("Pressure");
        XYSeriesCollection dataset1 = new XYSeriesCollection(series1);
        XYSeriesCollection dataset2 = new XYSeriesCollection(series2);
        
        //create plot
        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset1);
        plot.setDataset(1, dataset2);
        
        //creates dual renderers for two axis
        plot.setRenderer(0, new XYSplineRenderer());
        XYSplineRenderer splinerenderer = new XYSplineRenderer();
        splinerenderer.setSeriesFillPaint(0, Color.BLUE);
        plot.setRenderer(1, splinerenderer);
        plot.setRangeAxis(0, new NumberAxis("Salinity(%wt)"));
        plot.setRangeAxis(1, new NumberAxis("Pressure(psi)"));
        plot.setDomainAxis(new NumberAxis("Time(seconds)"));
        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 1);  
        
        JFreeChart chart = new JFreeChart("Salinity and Pressure", plot);
        myFrame.add(new ChartPanel(chart), BorderLayout.CENTER);
        
         //load available ports into drop-down menu portName--------------------
        SerialPort[] portNames = SerialPort.getCommPorts();
        for (int i = 0; i < portNames.length; i++) {
            portList.addItem(portNames[i].getSystemPortName());
        }
        
         //configure the connectButton------------------------------------------
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (connectButton.getText().equals("Connect")) {
                    // Attempt to connect to the serial port
                    userPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    userPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    // Change the button to disconnect if the port was opened sucessfully
                    if (userPort.openPort()) {
                        connectButton.setText("Disconnect");
                        portList.setEnabled(false);
                    }
                } else {
                    // disconnect from the serial port
                    userPort.closePort();
                    portList.setEnabled(true);
                    connectButton.setText("Connect");
                }
            }
        });//end connectButton

        //configure startButton to begin reading the serial port----------------
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Create a new thread that listens for incoming data from the Arduino and graphs it
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        Scanner scanner = new Scanner(userPort.getInputStream());
                        graphingBoo = true;
                        while (scanner.hasNextDouble() && graphingBoo) {
                            try {
                                //read next serial line, convert to double
                                String line = scanner.nextLine();
                                voltage = Double.parseDouble(line);
                                //ensures reading is valid
                                if (voltage >= 0 && voltage <= 1023) {//salinity reading
                                    salinity = getSalinity(voltage);
                                    series1.add(time1++, salinity);//adds new reading to series
                                    myFrame.repaint();//graphs new values
                                } else if (voltage > 1023 && voltage < 11123) {//pressure reading
                                    if (readPressBase == true) {//sets new basePressure
                                        pressBaseVal = getPressure(voltage, pressBaseVal);
                                        readPressBase = false; }
                                    pressure = getPressure(voltage, pressBaseVal);
                                    series2.add(time2++, pressure);
                                    myFrame.repaint();
                                }
                            } catch (Exception e) {
                            }
                        }//end while
                        scanner.close();
                    }//end run
                };//end thread
                thread.start();
            }
        });//end startButton
        
        //configure stopButton to stop graphing data----------------------------
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                graphingBoo = false;
            }
        });//end stopButton
        
        //configure resetButton to nullify and reset the graph------------------
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                time1 = 0;
                time2 = 0;
                series1.clear();
                series2.clear();
            }
        });//end resetButton
        
        //toggles between having an active pressure base------------------------
        pressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (toggleBase == false) {//base is currently off
                    readPressBase = true;//sets new pressure base
                    pressButton.setText("Pressure Base: ON");
                    toggleBase = true;
                } else {//base is currently on
                    toggleBase = false;
                    pressBaseVal = 0;//nulllifies pressure base
                    pressButton.setText("Pressure Base: OFF");
                }
            }
        });//end pressButton 
        
        // show the window
        myFrame.setVisible(true);
    }//end main
    
    //convert arduino reading into %wt salt
    public static double getSalinity(double Vin) {
       if (Vin < .01) {//0
           Vin = 0;
       } else if (Vin <885) {//0 to .5
           Vin = expLow(Vin);
       } else if (Vin > 885 && Vin < 900) {//.5 to 1
           Vin = expMid(Vin);
       }else if (Vin < 932) {//1 to 3
           Vin = avg(expMid(Vin), expHigh(Vin));
       } else {//>3
           Vin = superHigh(Vin);
       }
       return Vin;
    }//end getSalinity   
    
    public static double expLow(double Vin) {
        Vin = Vin * 0.0135;
        Vin = java.lang.Math.pow(2.718, Vin);
        Vin = Vin * 4E-6;
        return Vin;
    }//end expLow
    
     public static double expMid(double Vin) {
        Vin = Vin * 0.0363;
        Vin = java.lang.Math.pow(2.718, Vin);
        Vin = Vin * 7E-15;
        return Vin;
    }//end expMid
    
    public static double expHigh(double Vin) {
        Vin = Vin * 0.039;
        Vin = java.lang.Math.pow(2.718, Vin);
        Vin = Vin * 5E-16;
        return Vin;
    }//end expHigh
    
    public static double superHigh(double Vin) {
        Vin = Vin * 0.02155;
        Vin = java.lang.Math.pow(2.718, Vin);
        Vin = Vin * 6E-9;
        return Vin;
    }//end superHigh
    
    public static double avg(double in1, double in2) {
        in1 = (in1 + in2) / 2;
        return in1;
    }//end avg
    
    //convert arduino reading into psi
    public static double getPressure(double Vin, double base) {
        System.out.println("Pressure = " + Vin);
        Vin -= 10071.8;//-10000 to adjust for arduino alteration to send data
        Vin = Vin/1.6;
        Vin -= base;
        System.out.println("Pressure Base = " + base);
        return Vin;
    }//end getPressure
}//end driver
