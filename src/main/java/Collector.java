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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Collector {
	/** The URL for the page containing the stock prices. */
	private static final String LIST_PAGE_URL = "https://www.asnbank.nl/particulier/beleggen/koersen.html";
	private static final String TABLE_HEADER_CONTENT = "Koers (laatste 3 beschikbare)";
	private static final Pattern LIST_PAGE_FUND_NAME_PATTERN = Pattern.compile("title=\"([\\w\\s&;-]+)\"");
	
	private static final String[] FUND_PAGE_URLS = new String[]{
		"https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-aandelenfonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-obligatiefonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-mixfonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-milieu-waterfonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-small-midcapfonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-groenprojectenfonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-novib-microkredietfonds.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-mixfonds-zeer-defensief.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-mixfonds-defensief.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-mixfonds-neutraal.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-mixfonds-offensief.html"
			, "https://www.asnbank.nl/beleggen/beleggingsrekening/beleggingsfondsen/asn-duurzaam-mixfonds-zeer-offensief.html"
	};
	private static final String FUND_NAME_HTML = "<div class=\"product-usp__text\">";
	private static final String PRICE = "Koers";
	
	private static final String WRITE_DIRECTORY = "D:/Programming/Projects/AsnStockPriceTracker/output";
	private static final String SEPARATOR = ";";
	private static final String EXTENSION = ".csv";
	
	private static final Pattern INCOMING_DATE_PATTERN = Pattern.compile("\\d\\d-\\d\\d-\\d\\d\\d\\d");
	private static final DateTimeFormatter INCOMING_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	public static final DateTimeFormatter OUTGOING_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	private static final Pattern INCOMING_PRICE_PATTERN = Pattern.compile("[\\d]+,[\\d]+");
	
	private List<LocalDate> dates;
	private List<String> fundNames;
	private List<List<Double>> prices;
	
	private static final Logger log = LoggerFactory.getLogger(Collector.class);
	
	public Collector() {
		dates = new ArrayList<>();
		fundNames = new ArrayList<>();
		prices = new ArrayList<>();
	}
	
	private void readListPage() {
		URL url;
		InputStream inputStream;
		BufferedReader reader;
		try {
			// Open an input stream for the page.
			url = new URL(LIST_PAGE_URL);
			log.trace("Opening stream...");
			inputStream = url.openStream();
			log.trace("Creating reader...");
			reader = new BufferedReader(new InputStreamReader(inputStream));
			
			// Find the start of the table using the (static) table header.
			String currentLine;
			log.trace("Finding table header...");
			do {
				currentLine = StringUtils.strip(reader.readLine());
			} while (currentLine != null && !StringUtils.contains(currentLine, TABLE_HEADER_CONTENT));
			
			if (currentLine == null) {
				log.error("Finding table header failed.");
				return;
			}
			
			// The next three lines of HTML contain the other three cells of the table header, which contain the three dates for which prices are available.
			log.trace("Reading dates...");
			for (int line = 0; line < 3; line++) {
				// Strip the HTML tags from the line and parse the date from the remaining string.
				currentLine = StringUtils.strip(reader.readLine());
				Matcher matcher = INCOMING_DATE_PATTERN.matcher(currentLine);
				if (!matcher.find()) {
					log.error("No price found in line '{}'. Stopping.", currentLine);
					return;
				}
				
				LocalDate date = LocalDate.parse(matcher.group(), INCOMING_DATE_FORMAT);
				dates.add(date);
			}
			
			// Skip three lines.
			reader.readLine();
			reader.readLine();
			reader.readLine();
			
			// Handle all twelve funds.
			while (true) {
				// Skip one line (the <tr>).
				reader.readLine();
				
				currentLine = StringUtils.strip(reader.readLine());
				// currentLine now has the form of a basic <a/> tag within a <td/> tag. The fund name is both the link text, and the 'title' attribute of the <a/> tag. Use a reular
				// expression to read this last instance of the fund name.
				Matcher matcher = LIST_PAGE_FUND_NAME_PATTERN.matcher(currentLine);
				if (!matcher.find()) {
					log.info("No fund name found in line '{}'. Stopping after {} funds successfully read.", currentLine, fundNames.size());
					return;
				}
				
				String fundName = matcher.group(1);
				// Replace the ampersand with a plus sign to prevent inopportune Unicode escaping.
				fundName = fundName.replace(" &amp;", " +");
				
				// Read all three prices.
				log.trace("Reading prices for {}.", fundName);
				List<Double> pricesForFund = new ArrayList<>();
				for (int dateIndex = 0; dateIndex < 3; dateIndex++) {
					currentLine = StringUtils.strip(reader.readLine());
					matcher = INCOMING_PRICE_PATTERN.matcher(currentLine);
					if (!matcher.find()) {
						log.warn("No price found in line '{}'. Skipping.", currentLine);
						pricesForFund.add(null);
						continue;
					}
					
					String priceString = matcher.group();
					Double price;
					try {
						// Read the stock price on this line. It uses the Dutch number format (',' as decimal separator), so correct for that.
						price = Double.parseDouble(priceString.replace(',', '.'));
					} catch (NumberFormatException exception) {
						// For some reason the price could not be read. Register a null value and log a warning.
						price = null;
						log.error("Error reading price for fund '{}' on {}: '{}' is not a number.", fundName, dates.get(dateIndex).format(OUTGOING_DATE_FORMAT), priceString);
						return;
					}
					
					pricesForFund.add(price);
				}
				
				fundNames.add(fundName);
				prices.add(pricesForFund);
				
				// Skip one line (the </tr>).
				reader.readLine();
			}
		} catch (IOException exception) {
			log.error("Exception occurred while reading page:", exception);
		}
	}
	
	private void readFundPages() {
		for (String fundPageUrl : FUND_PAGE_URLS) {
			log.trace("Reading information from {}", fundPageUrl);
			try {
				// Open an input stream for the page.
				URL url = new URL(fundPageUrl);
				log.trace("Opening stream...");
				InputStream inputStream = url.openStream();
				log.trace("Creating reader...");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				
				// Find the fund name.
				log.trace("Finding fund name...");
				String currentLine;
				do {
					currentLine = StringUtils.strip(reader.readLine());
				} while (currentLine != null && !StringUtils.equals(currentLine, FUND_NAME_HTML));
				
				if (currentLine == null) {
					log.warn("Finding fund name on page at {} failed, skipping.", fundPageUrl);
					continue;
				}
				
				String fundName = StringUtils.substringBetween(StringUtils.strip(reader.readLine()), "<h1>", "</h1>").replace(" &amp;", " +");
				
				// Find the line that contains the date and price; this line begins with 'Koers'.
				do {
					currentLine = StringUtils.strip(reader.readLine());
				} while (currentLine != null && !StringUtils.startsWith(currentLine, PRICE));
				
				if (currentLine == null) {
					log.warn("Finding price for {} failed, skipping.", fundName);
					continue;
				}
				
				// currentLine will now have the following format: Koers dd-MM-yyyy € 999,99</h2><p>...
				String[] currentLineSplit = currentLine.split(" ");
				String dateString = currentLineSplit[1];
				log.trace("Reading date from string '{}'.", dateString);
				LocalDate date = LocalDate.parse(dateString, INCOMING_DATE_FORMAT);
				String priceString = StringUtils.substringBefore(currentLineSplit[3], "</h2>");
				log.trace("Reading price from string '{}'.", priceString);
				Double price;
				try {
					// Read the stock price on this line. It uses the Dutch number format (',' as decimal separator), so correct for that.
					price = Double.parseDouble(priceString.replace(',', '.'));
				} catch (NumberFormatException exception) {
					// For some reason the price could not be read. Register a null value and log a warning.
					price = null;
					log.warn("Error reading price for fund '{}' on {}: '{}' is not a number.", fundName, date.format(OUTGOING_DATE_FORMAT), priceString);
				}
				
				if (dates.isEmpty()) {
					dates.add(date);
				}
				
				fundNames.add(fundName);
				prices.add(Collections.singletonList(price));
			} catch (IOException exception) {
				log.error("Exception occurred while reading fund page at " + fundPageUrl, exception);
			}
		}
	}
	
	private void write() {
		Path writePath = FileSystems.getDefault().getPath(WRITE_DIRECTORY, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + EXTENSION).toAbsolutePath();
		
		List<String> fileContents = new ArrayList<>();
		fileContents.add("Fund name" + SEPARATOR + StringUtils.join(dates.stream().map(date -> date.format(OUTGOING_DATE_FORMAT)).collect(Collectors.toList()), SEPARATOR));
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
		Collector collector = new Collector();
		log.info("Reading...");
		collector.readListPage();
		if (collector.prices.isEmpty()) {
			// The collector failed to read the prices from the list page. Try the alternative: read prices from the individual fund pages.
			collector.readFundPages();
		}
		log.info("Reading complete. Writing...");
		collector.write();
		log.info("Writing complete");
	}
}