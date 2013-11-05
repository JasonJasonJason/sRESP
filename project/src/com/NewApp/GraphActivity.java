package com.NewApp;

import android.os.Bundle;

import android.app.Activity;
import android.view.Menu;
import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;
import java.util.Arrays;

public class GraphActivity extends Activity {
    private XYPlot plot;
    //private BarPlot plott;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
 
        // fun little snippet that prevents users from taking screenshots
        // on ICS+ devices :-)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                                 WindowManager.LayoutParams.FLAG_SECURE);
 
        setContentView(R.layout.graph);
        
 
        // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        plot.setRangeBoundaries(-6,  6, BoundaryMode.FIXED);
        // Create a couple arrays of y-values to plot:
        Number[] series1Numbers = {-3.8, -3.3, -1.5, 0.4, 2, 3.3, 3.8, 3.3, 2, 0.4, -1.5, -3.3,
        		-3.8, -3.3, -1.5, 0.4, 2, 3.3, 3.8, 3.3, 2, 0.4, -1.5, -3.3,
        		-3.8, -3.3, -1.5, 0.4, 2, 3.3, 3.8, 3.3, 2, 0.4, -1.5, -3.3};
 
        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "John Doe");
 
 
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        
        PointLabelFormatter labelFormat = new PointLabelFormatter();
        labelFormat.vOffset = 1000;
        
        
        series1Format.setPointLabelFormatter(labelFormat);
        series1Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
 
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
 
    }

}
