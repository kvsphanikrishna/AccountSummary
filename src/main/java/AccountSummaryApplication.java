import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import model.Transaction;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Account Summary Application, to load the transactions from CSV file and apply filters as per requirement.
 */
public class AccountSummaryApplication {

    private static final Logger log = Logger.getLogger(AccountSummaryApplication.class.getName());

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String PAYMENT_TRANSACTION_TYPE = "PAYMENT";

    private static final String TRANSACTIONS_CSV_PATH = "src/main/resources/transactions.csv";

    // Note: Declared these variables here, just to perform assertions in Junit test cases. Not a recommended way for development purpose.
    // Since we dont want to complicate things, i am declaring them here, modifying them while running junit and asserting them to confirm the status of the test.
    public static BigDecimal netBalanceAmount = BigDecimal.ZERO;
    public static int numberOfTransactions = 0;

    /**
     * Get the Balance Summary for user's search criteria & logs the result in console output.
     * @param args - Expects accountId, fromDate & toDate.
     */
    public static void main(String[] args) {

        // Read the user input as Java Arguments for filtering the transactions.
        // Assuming that the filter params are passed in the order of AccountId, FromDate & ToDate.
        String accountId = args[0];
        String fromDate = args[1];
        String toDate = args[2];

        // Load all transactions from input file (for convenience, stored in classpath)
        List<Transaction> transactions = getAllTransactions();
        if (transactions == null || transactions.isEmpty()) {
            // In case of any issues, the exception details are logged and a message is displayed as below.
            log.log(Level.SEVERE, "Unable to Process request");
            return;
        }

        // Converting the dates in string format to Java formatted Dates, for calculations further.
        LocalDateTime fromDateTime = LocalDateTime.parse(fromDate, formatter);
        LocalDateTime toDateTime = LocalDateTime.parse(toDate, formatter);

        // For the given accountId, identifying the transactions that match the given accountId and preparing a map of lists
        // separated by Transaction type. (Either PAYMENT or REVERSAL)
        Map<Boolean, List<Transaction>> partitionedTransactions = transactions.stream()
                .filter(t -> accountId.equals(t.getFromAccountId())) // match the accountID
                .collect(Collectors.partitioningBy(transType -> transType.getTransactionType().equals(PAYMENT_TRANSACTION_TYPE))); // Checking for the condition if transaction type is "Payment".

        List<Transaction> totalPaymentTransactions = partitionedTransactions.get(true); // All PAYMENT type transactions that match the above condition are stored in this list.
        List<Transaction> totalReversalTransactions = partitionedTransactions.get(false);// All remaining transactions, in this case "REVERSAL" type transactions are stored in this list.

        // Now from the transactions that match the accountId & PAYMENT type, we are now applying another filter
        // based on the provided fromDate & toDate.
        // The result will be only those PAYMENT type transactions that match the user criteria.
        List<Transaction> filteredPaymentTransactions = totalPaymentTransactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(fromDateTime) && t.getCreatedAt().isBefore(toDateTime))
                .collect(Collectors.toList());

        // Adding the amounts from all transactions in the filtered payment list. Using BigDecimal for Amount based calculations.
        BigDecimal netPaymentAmount = filteredPaymentTransactions.stream().map(t -> new BigDecimal(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Adding the amounts from all transactions in the total reversal list. Using BigDecimal for Amount based calculations.
        BigDecimal netReversalAmount = totalReversalTransactions.stream().map(t -> new BigDecimal(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // the net difference between the amounts, will provide the relative balance for the user criteria.
        netBalanceAmount = netPaymentAmount.subtract(netReversalAmount);

        log.info("Relative Balance for the period is : " + netBalanceAmount.toString());

        // The difference in the lists, would provide the transactions that are considered for this calculation.
        numberOfTransactions = filteredPaymentTransactions.size() - totalReversalTransactions.size();
        log.info("Number of transactions included is : " + numberOfTransactions);
    }

    /**
     * Get the transactions list by processing the transactions csv file.
     * Using OpenCsv library to process the CSV file to java POJO's.
     */
    private static List<Transaction> getAllTransactions() {
        try(FileReader fileReader = new FileReader(TRANSACTIONS_CSV_PATH)) {
            CsvToBeanBuilder<Transaction> transactionsBeanBuilder = new CsvToBeanBuilder<Transaction>(fileReader)
                    .withType(Transaction.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS);
            CsvToBean<Transaction> transactionsBean = transactionsBeanBuilder.build();
            return transactionsBean.parse();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Exception encountered", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
