package org.technosystem.client.api.brokers.pieces;

import java.util.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import com.dukascopy.api.*;
import java.awt.FlowLayout;
import java.util.Map;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Container ;
import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Timer;
import java.awt.Font;
import java.awt.event.WindowEvent;

import com.dukascopy.api.IIndicators.AppliedPrice;

@RequiresFullAccess
public class RSIScoreView  extends JFrame implements IStrategy {
    
	private static final long serialVersionUID = 1L;

	class DataView{
        private Period m_Period;
        private String m_label;
        private Map<String, Double> m_CCYs = new HashMap<>();
        private List<JTextField> m_NameList = new ArrayList<>();
        private List<JTextField> m_ValueList = new ArrayList<>();
        
        DataView(Period period, String label){
            m_Period = period;
            m_label = label;
        }
        
        void setCCYs(HashMap<String, Double> CCYs){
            m_CCYs.clear();
            m_CCYs.putAll(CCYs);
        }
        
        Map<String, Double> getCCYs(){
            return m_CCYs;
        }
        
        void setNameList(List<JTextField> NameList){
            m_NameList.clear();
            m_NameList.addAll(NameList);
        }
        
        List<JTextField> getNameList(){
            return m_NameList;
        }
        
        void setValueList(List<JTextField> ValueList){
            m_ValueList.clear();
            m_ValueList.addAll(ValueList);
        }
        
        List<JTextField> getValueList(){
            return m_ValueList;
        }
        
        Period getPeriod(){
            return m_Period;
        }
        
        String getLabel(){
            return m_label;
        }
    }
    
    private IConsole console;
    private IHistory history;
    private IIndicators indicators;
    
    private List<Instrument> m_Instruments = null;
    private List<DataView> m_DataViews = null;
    private Timer timer = null;
    
    private void createFrame(DataView dataView, JPanel parentPanel) { 
        setTitle(this.getClass().getSimpleName());
        
        JPanel panelView = new JPanel(new BorderLayout());             
        //Create the panel and populate it.
        JPanel panel = new JPanel(new SpringLayout());
        List<JTextField> NameList = new ArrayList<>();
        List<JTextField> ValueList = new ArrayList<>();
        for (String key : dataView.getCCYs().keySet()){
            JTextField nameField = new JTextField(key);
            panel.add(nameField);
            NameList.add(nameField);
            
            JTextField valueField = new JTextField("0.000");
            panel.add(valueField);
            ValueList.add(valueField);
        }
        
        NameList.get(0).setBackground(new Color(146, 205, 0));
        NameList.get(1).setBackground(new Color(146, 205, 0));
        ValueList.get(0).setBackground(new Color(146, 205, 0));
        ValueList.get(1).setBackground(new Color(146, 205, 0));
 
        NameList.get(ValueList.size()-2).setBackground(new Color(255, 113, 126));
        NameList.get(ValueList.size()-1).setBackground(new Color(255, 113, 126));
        ValueList.get(ValueList.size()-2).setBackground(new Color(255, 113, 126));
        ValueList.get(ValueList.size()-1).setBackground(new Color(255, 113, 126));
        
        dataView.setNameList(NameList);
        dataView.setValueList(ValueList);
        
        //Lay out the panel.
        makeGrid(panel,
                 dataView.getCCYs().keySet().size(), 2, //rows, cols
                 5, 5, //initialX, initialY
                 5, 5);//xPad, yPad
 
        //Set up the content pane.
        panel.setOpaque(true); //content panes must be opaque
        JLabel label = new JLabel(dataView.getLabel(), SwingConstants.CENTER);
        label.setFont(new Font(label.getFont().getFontName(), Font.BOLD, label.getFont().getSize()));
        panelView.add(label, BorderLayout.PAGE_START);
        panelView.add(panel, BorderLayout.CENTER);
        
        parentPanel.add(panelView);
        
        //Display the window.
        pack();
        setVisible(true);
    }


    public void onStart(IContext context) throws JFException {
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.indicators = context.getIndicators();
        
        m_Instruments = new ArrayList<>();
        m_DataViews = new ArrayList<>();
        timer = new Timer();
        
        m_Instruments.add(Instrument.AUDCAD);
        m_Instruments.add(Instrument.AUDCHF);
        m_Instruments.add(Instrument.AUDJPY);
        m_Instruments.add(Instrument.AUDNZD);
        m_Instruments.add(Instrument.AUDUSD);
        m_Instruments.add(Instrument.CADCHF);
        m_Instruments.add(Instrument.CADJPY);
        m_Instruments.add(Instrument.CHFJPY);
        m_Instruments.add(Instrument.EURAUD);
        m_Instruments.add(Instrument.EURCAD);
        m_Instruments.add(Instrument.EURCHF);
        m_Instruments.add(Instrument.EURGBP);
        m_Instruments.add(Instrument.EURJPY);
        m_Instruments.add(Instrument.EURNZD);
        m_Instruments.add(Instrument.EURUSD);
        m_Instruments.add(Instrument.GBPAUD);
        m_Instruments.add(Instrument.GBPCAD);
        m_Instruments.add(Instrument.GBPCHF);
        m_Instruments.add(Instrument.GBPJPY);
        m_Instruments.add(Instrument.GBPNZD);
        m_Instruments.add(Instrument.GBPUSD);
        m_Instruments.add(Instrument.NZDCAD);
        m_Instruments.add(Instrument.NZDCHF);
        m_Instruments.add(Instrument.NZDJPY);
        m_Instruments.add(Instrument.NZDUSD);
        m_Instruments.add(Instrument.USDCAD);
        m_Instruments.add(Instrument.USDCHF);
        m_Instruments.add(Instrument.USDJPY);
        
        HashMap<String, Double> CCYs = new HashMap<>();
        for(Instrument instrument:m_Instruments){
            CCYs.put(instrument.getPrimaryJFCurrency().getSymbol(), 0.0);
            CCYs.put(instrument.getSecondaryJFCurrency().getSymbol(), 0.0);
        }
        
        m_DataViews.add(new DataView(Period.FIVE_MINS, "5 Minutes"));
        m_DataViews.add(new DataView(Period.ONE_HOUR, "1 Hour"));
        m_DataViews.add(new DataView(Period.FOUR_HOURS, "4 Hours"));
        m_DataViews.add(new DataView(Period.DAILY, "24 Hours"));
        for(DataView dataView:m_DataViews){
            dataView.setCCYs(CCYs);
        }
        
        final JPanel compsToExperiment = new JPanel();
        FlowLayout experimentLayout = new FlowLayout(FlowLayout.CENTER, 20, 5);
        compsToExperiment.setLayout(experimentLayout);
        experimentLayout.setAlignment(FlowLayout.TRAILING);
        
        for(DataView dataView:m_DataViews){
            createFrame(dataView, compsToExperiment);
        }        
        
        getContentPane().add(compsToExperiment, BorderLayout.CENTER);
        
        pack();
        setVisible(true);
        
        timer.scheduleAtFixedRate(new TimerTask() {
          @Override
          public void run(){
               synchronized(m_DataViews){
                  try{
                      for(DataView dataView:m_DataViews){
                          refresh(dataView.getPeriod(), dataView);
                      } 
                  }catch(Exception e){print("error: " + e);}
            }
          }
        }, 10*1000, 5*1000);
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        timer.cancel();
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        m_Instruments = null;
        m_DataViews = null;
        timer = null;
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    }
    
    public void refresh(Period period, DataView dataView) throws JFException {

        Map<Instrument, Double> CCYScoreIni = new HashMap<>();
        for (Instrument key : m_Instruments){
            double[] data = ((double[])indicators.calculateIndicator(key, period, new OfferSide[]{OfferSide.BID}, "RSI", 
                new AppliedPrice[]{AppliedPrice.CLOSE }, new Object[] { 5 }, Filter.WEEKENDS, 1, history.getStartTimeOfCurrentBar(key, period), 0)[0]);                        
            double keyClose0 = data[0];   
            
            CCYScoreIni.put(key, keyClose0);
        }

        for (String key : dataView.getCCYs().keySet()){
           dataView.getCCYs().put(key, 0.0);
        }
        
        for (Instrument key : CCYScoreIni.keySet()){
            Double keyScore = CCYScoreIni.get(key);
            String ccy0 = key.getPrimaryJFCurrency().getSymbol();
            dataView.getCCYs().put(ccy0, dataView.getCCYs().get(ccy0)+keyScore/7.0);
            
            String ccy1 = key.getSecondaryJFCurrency().getSymbol();
            dataView.getCCYs().put(ccy1, dataView.getCCYs().get(ccy1)+(100.0-keyScore)/7.0);
        }

        SorteComparator vc = new SorteComparator(dataView.getCCYs()); 
        TreeMap<String, Double> CCYsSorted = new TreeMap<String, Double>(vc);
        CCYsSorted.putAll(dataView.getCCYs());

        for(int k = 0; k < dataView.getCCYs().keySet().size(); k++){
            Map.Entry<String, Double> maxData = CCYsSorted.pollLastEntry();
            dataView.getNameList().get(k).setText(maxData.getKey());
            dataView.getValueList().get(k).setText(""+Math.round(maxData.getValue()*100.0)/100.0);
        }
    }
    
    static class SorteComparator implements Comparator<String> {
        Map<String, Double> base;

        SorteComparator(Map<String, Double> base) {
            this.base = base;
        }

        @Override
        public int compare(String a, String b) {
            Double x = base.get(a);
            Double y = base.get(b);
            if (x.equals(y)) {
                return a.compareTo(b);
            }
            return x.compareTo(y);
        }
    }
    
    private void print(String s)
    {
        console.getOut().println(s);
    }

    public static void makeGrid(Container parent,
                                int rows, int cols,
                                int initialX, int initialY,
                                int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeGrid must use SpringLayout.");
            return;
        }

        Spring xPadSpring = Spring.constant(xPad);
        Spring yPadSpring = Spring.constant(yPad);
        Spring initialXSpring = Spring.constant(initialX);
        Spring initialYSpring = Spring.constant(initialY);
        int max = rows * cols;

        //Calculate Springs that are the max of the width/height so that all
        //cells have the same size.
        Spring maxWidthSpring = layout.getConstraints(parent.getComponent(0)).
                                    getWidth();
        Spring maxHeightSpring = layout.getConstraints(parent.getComponent(0)).
                                    getHeight();
        for (int i = 1; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                                            parent.getComponent(i));

            maxWidthSpring = Spring.max(maxWidthSpring, cons.getWidth());
            maxHeightSpring = Spring.max(maxHeightSpring, cons.getHeight());
        }

        //Apply the new width/height Spring. This forces all the
        //components to have the same size.
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                                            parent.getComponent(i));

            cons.setWidth(maxWidthSpring);
            cons.setHeight(maxHeightSpring);
        }

        //Then adjust the x/y constraints of all the cells so that they
        //are aligned in a grid.
        SpringLayout.Constraints lastCons = null;
        SpringLayout.Constraints lastRowCons = null;
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                                                 parent.getComponent(i));
            if (i % cols == 0) { //start of new row
                lastRowCons = lastCons;
                cons.setX(initialXSpring);
            } else { //x position depends on previous component
                cons.setX(Spring.sum(lastCons.getConstraint(SpringLayout.EAST),
                                     xPadSpring));
            }

            if (i / cols == 0) { //first row
                cons.setY(initialYSpring);
            } else { //y position depends on previous row
                cons.setY(Spring.sum(lastRowCons.getConstraint(SpringLayout.SOUTH),
                                     yPadSpring));
            }
            lastCons = cons;
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH,
                            Spring.sum(
                                Spring.constant(yPad),
                                lastCons.getConstraint(SpringLayout.SOUTH)));
        pCons.setConstraint(SpringLayout.EAST,
                            Spring.sum(
                                Spring.constant(xPad),
                                lastCons.getConstraint(SpringLayout.EAST)));
    }
}