import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Charter extends ApplicationFrame {
	public static final String READ_FILE = "D:/Programming/Projects/AsnStockPriceTracker/aggregated/20180407.csv";
	
	public static final Integer FUND_INDEX = 8;
	
	public Charter(String title) {
		super(title);
		
		JFreeChart lineChart = ChartFactory.createLineChart(
				title,
				"Date", "Price",
				createDataSet(),
				PlotOrientation.VERTICAL,
				true, true, false);
		
		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		setContentPane(chartPanel);
	}
	
	private DefaultCategoryDataset createDataSet() {
		Path filePath = FileSystems.getDefault().getPath(READ_FILE).toAbsolutePath();
		
		List<String> lines;
		try {
			lines = Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		List<String> fundNames = Arrays.asList(lines.remove(0).split(Aggregator.SEPARATOR));
		
		DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
		
		for (String line : lines) {
			String[] valuesInLine = line.split(Aggregator.SEPARATOR);
			
			LocalDate dateForLine = LocalDate.parse(valuesInLine[0], Collector.OUTGOING_DATE_FORMAT);
			
			for (int valueIndex = (FUND_INDEX != null ? FUND_INDEX : 1);
				 valueIndex < (FUND_INDEX != null ? FUND_INDEX + 1 : valuesInLine.length);
				 valueIndex++) {
				String value = valuesInLine[valueIndex];
				
				if (StringUtils.isNotBlank(value) && !Objects.equals(value, "null")) {
					dataSet.addValue(Double.parseDouble(value), fundNames.get(valueIndex), dateForLine);
				}
			}
		}
		
		return dataSet;
	}
	
	public static void main(String[] args) {
		Charter chart = new Charter("Stock prices");
		
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setVisible(true);
	}
}
