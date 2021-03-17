import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountSummaryApplicationTest {

    @Test
    public void verifyTheNetBalanceForValidData() {
        String[] inputArray = new String[]{"ACC334455", "20/10/2018 12:00:00", "20/10/2018 19:00:00"};
        AccountSummaryApplication.main(inputArray);
        Assertions.assertEquals("25.00", AccountSummaryApplication.netBalanceAmount.toString());
        Assertions.assertEquals(1, AccountSummaryApplication.numberOfTransactions);
    }
}
