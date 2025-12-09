import java.sql.Date;

public class FinesService {

       public static double calculateFine(Date dueDate,
                                       Date returnDate,
                                       double fineRate,
                                       int gracePeriodDays) {

        if (dueDate == null || returnDate == null) {
            return 0.0;
        }

        long diffMillis = returnDate.getTime() - dueDate.getTime();
        long daysLate = diffMillis / (1000L * 60 * 60 * 24);

        if (daysLate <= gracePeriodDays) {
            return 0.0;
        }

        long chargeableDays = daysLate - gracePeriodDays;
        return chargeableDays * fineRate;
    }
}
