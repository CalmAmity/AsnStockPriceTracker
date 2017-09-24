import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Aggregator {
	public static final String READ_DIRECTORY = "D:/Programming/Projects/AsnStockPriceTracker/output";
	public static final String WRITE_DIRECTORY = "D:/Programming/Projects/AsnStockPriceTracker/aggregated";
	private static final String SEPARATOR = ";";
	private static final String EXTENSION = ".csv";
	
	private static final Logger log = LoggerFactory.getLogger(Aggregator.class);
	
	private Map<String, Map<LocalDate, Double>> values;
	
	private void aggregate() throws IOException {
		values = new HashMap<>();
		
		Path writePath = FileSystems.getDefault().getPath(READ_DIRECTORY).toAbsolutePath();
		Stream<Path> files = Files.list(writePath);
		files.forEach((path -> {
			try {
				this.readFile(path);
			} catch (IOException exception) {
				log.error("Exception occurred wile reading file " + path.toString(), exception);
			}
		}));
		
		writeFile();
	}
	
	private void readFile(Path filePath) throws IOException {
		log.debug("Reading file " + filePath);
		
		List<String> lines = Files.readAllLines(filePath);
		
		// Read the dates on the first line.
		String[] dates = lines.remove(0).split(SEPARATOR);
		
		lines.forEach(line -> {
			if (StringUtils.isBlank(line)) {
				return;
			}
			String[] valuesInLine = line.split(SEPARATOR);
			
			String fundName = valuesInLine[0];
			
			Map<LocalDate, Double> valuesForFund = values.computeIfAbsent(fundName, (f) -> new HashMap<>());
			
			for (int valueIndex = 1; valueIndex < valuesInLine.length; valueIndex++) {
				LocalDate date = LocalDate.parse(dates[valueIndex], Collector.OUTGOING_DATE_FORMAT);
				if (StringUtils.isNotBlank(valuesInLine[valueIndex])) {
					valuesForFund.put(date, Double.parseDouble(valuesInLine[valueIndex]));
				}
			}
		});
	}
	
	private void writeFile() {
		Path writePath = FileSystems.getDefault().getPath(WRITE_DIRECTORY, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + EXTENSION).toAbsolutePath();
		List<String> fileContents = new ArrayList<>();
		
		List<String> fundNames = new ArrayList<>(values.keySet());
		
		String headerLine = "Date" + SEPARATOR + String.join(SEPARATOR, fundNames);
		
		fileContents.add(headerLine);
		
		LocalDate earliestDate = null, latestDate = null;
		
		for (Map<LocalDate, Double> valuesForFund : values.values()) {
			for (LocalDate date : valuesForFund.keySet()) {
				if (earliestDate == null || date.isBefore(earliestDate)) {
					earliestDate = date;
				}
				
				if (latestDate == null || date.isAfter(latestDate)) {
					latestDate = date;
				}
			}
		}
		
		LocalDate currentDate = earliestDate;
		
		while (!currentDate.isAfter(latestDate)) {
			StringBuilder line = new StringBuilder(currentDate.format(Collector.OUTGOING_DATE_FORMAT));
			
			for (String fundName : fundNames) {
				line.append(SEPARATOR).append(values.get(fundName).get(currentDate));
			}
			
			fileContents.add(line.toString());
			
			currentDate = currentDate.plusDays(1);
		}
		
		try {
			log.debug("Writing to " + writePath);
			Files.write(writePath, fileContents);
		} catch (IOException exception) {
			log.error("Exception occurred while writing file:", exception);
		}
	}
	
	public static void main(String[] args) {
		Aggregator aggregator = new Aggregator();
		log.info("Aggregating...");
		try {
			aggregator.aggregate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
