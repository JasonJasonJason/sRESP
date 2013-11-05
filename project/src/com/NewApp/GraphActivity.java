/*
 * Copyright 2012 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.NewApp ;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.androidplot.LineRegion;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.TextOrientationType;
import com.androidplot.ui.widget.TextLabelWidget;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.*;
import com.androidplot.xy.BarRenderer.BarRenderStyle;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;

/**
 * The simplest possible example of using AndroidPlot to plot some data.
 */
public class GraphActivity extends Activity
{
    private XYPlot plot;
    
    //Only need 2 series, one for user one for target
    private XYSeries series1;
    private XYSeries series2;
    private int SeriesSize = 1;

    // Create a couple arrays of y-values to plot:
    //initially set both to ideal (6)
    Number[] series1Numbers = {0, 14.7, 0, 0};
    Number[] series2Numbers = {0, 0, 6, 0};

    private MyBarFormatter formatter1, formatter2;

    private TextLabelWidget label;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph);
        
        formatter1 = new MyBarFormatter(Color.argb(400, 100, 150, 200), Color.LTGRAY);
        formatter2 = new MyBarFormatter(Color.argb(200, 55, 163, 77), Color.LTGRAY);

        // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(1);
        plot.setRangeBoundaries(0, 20, BoundaryMode.FIXED);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);  //remove the domain values
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.TRANSPARENT);
        plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
        plot.getGraphWidget().setGridPadding(0, 20, 10, 0);
        plot.getGraphWidget().setGridBackgroundPaint(null);
        plot.getGraphWidget().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        //plot.getGraphWidget().setDomainLabelWidth(200);

        plot.setTicksPerDomainLabel(5);      
    
        updatePlot();
        
        Timer timer = new Timer();
        timer.schedule(new firstTask(), 0, 500);

    }

    //I don't think this is right, but we can change it to simply redraw the plot with the new data
    //I think this adds new bars to the graph
    private void updatePlot() {
    	
    	// Remove all current series from each plot
        Iterator<XYSeries> iterator1 = plot.getSeriesSet().iterator();
        while(iterator1.hasNext()) { 
        	XYSeries setElement = iterator1.next();
        	plot.removeSeries(setElement);
        }
        
        float respirationRate = ((sRESPApplication) getApplication()).getRespirationRate();

        if(respirationRate > 20){
        	series1Numbers[1] = 20;
        	formatter1 = new MyBarFormatter(Color.argb(200, 189, 47, 47), Color.LTGRAY);
        	plot.redraw();
        }
        else
        	series1Numbers[1] = respirationRate;
        
        // Setup our Series with the selected number of elements
        series1 = new SimpleXYSeries(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, " You");
        series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, " Ideal");
        
        plot.addSeries(series1, formatter1);
        plot.addSeries(series2, formatter2);

        // Setup the BarRenderer with our selected options
        MyBarRenderer renderer = ((MyBarRenderer)plot.getRenderer(MyBarRenderer.class));
        renderer.setBarWidth(60);	//width of the bar plots
        renderer.setBarRenderStyle(BarRenderStyle.OVERLAID);
	        
        plot.redraw();	
    }  
    
    class MyBarFormatter extends BarFormatter {
        public MyBarFormatter(int fillColor, int borderColor) {
            super(fillColor, borderColor);
        }

        @Override
        public Class<? extends SeriesRenderer> getRendererClass() {
            return MyBarRenderer.class;
        }

        @Override
        public SeriesRenderer getRendererInstance(XYPlot plot) {
            return new MyBarRenderer(plot);
        }
    }

    class MyBarRenderer extends BarRenderer<MyBarFormatter> {

        public MyBarRenderer(XYPlot plot) {
            super(plot);
        }
    }
    
    
    class firstTask extends TimerTask {

        @Override
        public void run() {
            updatePlot();
        }
   };
}