import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Reader {
	/** The URL for the page containing the stock prices. */
	private static final String PAGE_URL = "https://www.asnbank.nl/particulier/beleggen/koersen.html";
	private static final String TABLE_HEADER_PREFIX = "<th> Koers (laatste 3 beschikbare)";
	private static final String WRITE_DIRECTORY = "output/";
	private static final String SEPARATOR = ";";
	
	private static final DateTimeFormatter incomingDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	private static final DateTimeFormatter outgoingDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
	private List<LocalDate> dates;
	private List<String> fundNames;
	private List<List<Double>> prices;
	
	private static final Logger log = LoggerFactory.getLogger(Reader.class);
	
	public Reader() {
		dates = new ArrayList<>();
		fundNames = new ArrayList<>();
		prices = new ArrayList<>();
	}
	
	private void read() {
		URL url;
		InputStream inputStream;
		BufferedReader reader;
		try {
			// Open an input stream for the page.
			url = new URL(PAGE_URL);
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			
			// Find the start of the table using the (static) table header.
			String currentLine;
			while (!StringUtils.startsWith(StringUtils.strip(reader.readLine()), TABLE_HEADER_PREFIX)) {}
			
			// The next three lines of HTML contain the other three cells of the table header, which contain the three dates for which prices are available.
			for (int line = 0; line < 3; line++) {
				// Strip the HTML tags from the line and parse the date from the remaining string.
				currentLine = StringUtils.strip(reader.readLine());
				LocalDate date = LocalDate.parse(StringUtils.substringBetween(currentLine, "<th>", "</th>"), incomingDateFormat);
				dates.add(date);
			}
			
			// Skip three lines.
			for (int line = 0; line < 3; line++) {
				reader.readLine();
			}
			
			// Handle all twelve funds.
			while (true) {
				// Skip two lines.
				reader.readLine();
				reader.readLine();
				currentLine = StringUtils.strip(reader.readLine());
				
				// currentLine now has the form of a basic <a/> tag.
				String fundName = StringUtils.removeEnd(
						// Find the fund name after the end of the opening <a> tag...
						StringUtils.substringAfter(currentLine, ".html\">"),
						// ...and remove the closing <a> tag, if present.
						"</a>");
				
				if (StringUtils.isBlank(fundName)) {
					// No further funds are present on the page.
					break;
				}
				
				List<Double> pricesForFund = new ArrayList<>();
				
				// Read all three prices.
				for (int dateIndex = 0; dateIndex < 3; dateIndex++) {
					// Skip two lines.
					reader.readLine();
					reader.readLine();
					currentLine = StringUtils.strip(reader.readLine()).replace(',', '.');
					Double price;
					try {
						// Read the stock price on this line. It uses the Dutch number format (',' as decimal separator), so correct for that.
						price = Double.parseDouble(currentLine);
					} catch (NumberFormatException exception) {
						// For some reason the price could not be read. Register a null value and log a warning.
						price = null;
						log.warn("Error reading price for fund '{}' on {}: '{}' is not a number.", fundName, dates.get(dateIndex).format(outgoingDateFormat), currentLine);
					}
					
					pricesForFund.add(price);
				}
				
				fundNames.add(fundName);
				prices.add(pricesForFund);
				
				// Skip two lines.
				reader.readLine();
				reader.readLine();
			}
		} catch (IOException exception) {
			log.error("Exception occurred while reading page:", exception);
		}
	}
	
	private void write() {
		Path writePath = FileSystems.getDefault().getPath(WRITE_DIRECTORY, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))).toAbsolutePath();
		
		List<String> fileContents = new ArrayList<>();
		fileContents.add("Fund name" + SEPARATOR + StringUtils.join(dates.stream().map(date -> date.format(outgoingDateFormat)).collect(Collectors.toList()), SEPARATOR));
		for (int fundIndex = 0; fundIndex < fundNames.size(); fundIndex++) {
			fileContents.add(fundNames.get(fundIndex) + SEPARATOR + StringUtils.join(prices.get(fundIndex), SEPARATOR));
		}
		
		try {
			log.debug("Writing to " + writePath);
			Files.write(writePath, fileContents);
		} catch (IOException exception) {
			log.error("Exception occurred while writing file:", exception);
		}
	}
	
	public static void main(String[] args) {
		Reader reader = new Reader();
		log.info("Reading...");
		reader.read();
		log.info("Writing...");
		reader.write();
	}
}