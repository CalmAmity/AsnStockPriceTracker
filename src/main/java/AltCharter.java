import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AltCharter extends Application {
	public static final String READ_FILE = "D:/Programming/Projects/AsnStockPriceTracker/aggregated/20180407.csv";
	
	public static final Integer FUND_INDEX = 6;

	@Override
	public void start(Stage primaryStage) throws Exception {
		//Defining X axis
		NumberAxis xAxis = new NumberAxis(1498428000, 1523106562, 999999999);
		xAxis.setLabel("Time");

		//Defining y axis
		NumberAxis yAxis = new NumberAxis(29, 30, 1);
		yAxis.setLabel("Price");
		
		
		
		LineChart<Number, Number> linechart = new LineChart<>(xAxis, yAxis);
		
		
		linechart.setCreateSymbols(false);
		
		XYChart.Series<Number, Number> dataSet = createDataSet();
		
		
		//Setting the data to Line chart
		linechart.getData().add(dataSet);
		
		
		
		
		Group root = new Group(linechart);
		
		
		Scene scene = new Scene(root ,600, 300);
		
		
		
		primaryStage.setTitle("Stock price");
		
		
		
		primaryStage.setScene(scene);
		
		primaryStage.setHeight(450);
		
		
		primaryStage.show();
	}
	
	private XYChart.Series<Number, Number> createDataSet() {
		Path filePath = FileSystems.getDefault().getPath(READ_FILE).toAbsolutePath();
		
		List<String> lines;
		try {
			lines = Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		List<String> fundNames = Arrays.asList(lines.remove(0).split(Aggregator.SEPARATOR));
		
		XYChart.Series<Number, Number> series = new XYChart.Series<>();
		series.setName(fundNames.get(FUND_INDEX));
		
		for (String line : lines) {
			String[] valuesInLine = line.split(Aggregator.SEPARATOR);
			
			LocalDate dateForLine = LocalDate.parse(valuesInLine[0], Collector.OUTGOING_DATE_FORMAT);
			
			for (int valueIndex = (FUND_INDEX != null ? FUND_INDEX : 1);
				 valueIndex < (FUND_INDEX != null ? FUND_INDEX + 1 : valuesInLine.length);
				 valueIndex++) {
				String value = valuesInLine[valueIndex];
				
				if (StringUtils.isNotBlank(value) && !Objects.equals(value, "null")) {
					series.getData().add(new XYChart.Data<>(
							dateForLine.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
							, Double.parseDouble(value)));
				}
			}
		}
		
		return series;
	}
	
	public static void main(String args[]){
		launch(args);
	}
}
