import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class Charter extends ApplicationFrame {
	
	public static final String ROW_KEY1 = "rowkey1";
	public static final String ROW_KEY2 = "rowkey2";
	
	public Charter(String title) {
		super(title);
		
		JFreeChart lineChart = ChartFactory.createLineChart(
				title,
				"Date", "Price",
				createDataset(),
				PlotOrientation.VERTICAL,
				true, true, false);
		
		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		setContentPane(chartPanel);
	}
	
	private DefaultCategoryDataset createDataset() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		dataset.addValue(15, ROW_KEY1, "1970");
		dataset.addValue(30, ROW_KEY1, "1980");
		dataset.addValue(60, ROW_KEY1, "1990");
		dataset.addValue(120, ROW_KEY1, "2000");
		dataset.addValue(240, ROW_KEY1, "2010");
		dataset.addValue(300, ROW_KEY1, "2014");
		
		dataset.addValue(30, ROW_KEY2, "1970");
		dataset.addValue(60, ROW_KEY2, "1980");
		dataset.addValue(120, ROW_KEY2, "1990");
		dataset.addValue(240, ROW_KEY2, "2000");
		dataset.addValue(300, ROW_KEY2, "2010");
		dataset.addValue(400, ROW_KEY2, "2014");
		
		return dataset;
	}
	
	public static void main(String[] args) {
		Charter chart = new Charter("Stock prices");
		
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setVisible(true);
	}
}
