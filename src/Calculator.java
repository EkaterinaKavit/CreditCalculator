import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Calculator {

    private static final List<LocalDate> HOLIDAYS = List.of(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 2),
            LocalDate.of(2025, 1, 3),
            LocalDate.of(2025, 1, 4),
            LocalDate.of(2025, 1, 5),
            LocalDate.of(2025, 1, 6),
            LocalDate.of(2025, 1, 7),
            LocalDate.of(2025, 2, 23),
            LocalDate.of(2025, 3, 8),
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 9),
            LocalDate.of(2025, 6, 12),
            LocalDate.of(2025, 11, 4),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 1, 3),
            LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 6),
            LocalDate.of(2026, 1, 7));


    public static void main(String[] args) {
        double sum_of_credit = 1000000; //сумма кредита
        int period_in_months = 12;  //период в месяцах
        double year_rate = 25.0; //годовая ставка
        int day_of_month = 2; //дата оплаты в месяце
        String type_of_credit = "annuitet"; //or "diff"  тип платежа
        int number_of_month = 6; //номер месяца взятия кредита
        int year =2025; // год взятия кредита
        LocalDate issue_date = LocalDate.of(year,number_of_month,day_of_month);
        List<Payment> graph_for_printing= calculateGraph(sum_of_credit,period_in_months,year_rate,day_of_month,type_of_credit,issue_date);
        printGraph(graph_for_printing);

    }

    static List<Payment> calculateGraph(double sum_of_credit,int period_in_months, double year_rate, int day_of_month,String type_of_credit, LocalDate issue_date){
        List<Payment> graph = new ArrayList<>();
        double monthly_rate =year_rate/100/12;
        double remaining_payment = sum_of_credit;
        LocalDate currentDay = getNextPaymentDay(issue_date, day_of_month);

        if (type_of_credit.equalsIgnoreCase("annuitet")){
            double annuityPayment = calculateAnnuitetPayment(sum_of_credit,period_in_months,monthly_rate);
            for (int i=0; i<period_in_months; i++){
                double interest = remaining_payment*monthly_rate; //часть, идущая на погашение процентов
                double principal_part_of_payment = annuityPayment - interest; // часть, идущая на погшение основного долга
                remaining_payment -= principal_part_of_payment;// остаток основного долга

                if (remaining_payment<0){
                    remaining_payment=0;
                }

                graph.add(new Payment(currentDay,annuityPayment,interest,principal_part_of_payment,remaining_payment));
                currentDay = getNextPaymentDay(currentDay,day_of_month);
            }
        } else if (type_of_credit.equalsIgnoreCase("diff")){
            double principal_part_of_payment = sum_of_credit/period_in_months;
            for (int i=0; i<period_in_months; i++){
                double interest = remaining_payment*monthly_rate;
                double payment = principal_part_of_payment+interest;
                remaining_payment-=principal_part_of_payment;
                if (remaining_payment<0){remaining_payment=0;}
                graph.add(new Payment(currentDay,payment,interest,principal_part_of_payment,remaining_payment));
                currentDay = getNextPaymentDay(currentDay, day_of_month);
            }
        }
        return graph;


    }

    //прибавить месяц и скорректировать день оплаты
    private static LocalDate getNextPaymentDay(LocalDate previous_date, int day_of_month){

        YearMonth nextMonth = YearMonth.from(previous_date.plusMonths(1));;
        int actualDay = Math.min(day_of_month,nextMonth.lengthOfMonth());
        LocalDate nextPaymentDay = LocalDate.of(nextMonth.getYear(),nextMonth.getMonth(),actualDay);
        return checkIfHolidayOrNot(nextPaymentDay);
    }

   //проверка праздник или выходной, и тогда перенос до рабочего дня
    private static LocalDate checkIfHolidayOrNot(LocalDate date){

        while (date.getDayOfWeek().getValue()>=6 || HolidayChecker.isHolidayOrNot(date)){
            System.out.println("Перенос даты: " + date + " -> " + date.plusDays(1));
            date=date.plusDays(1);
        }
        return date;

    }

    //расчет аннуитетного платежа
    private static double calculateAnnuitetPayment(double sum_of_credit, int period_in_months, double monthly_rate){
        if (monthly_rate==0) return sum_of_credit/period_in_months;
        double pow = Math.pow(1 + monthly_rate, period_in_months);
        return sum_of_credit * (monthly_rate * pow) / (pow - 1);

    }



    static class Payment{
        LocalDate date;
        double payment; // платеж
        double interest; //процент в платеже
        double principal_part_of_payment; // часть в платеже на погашение основного долга
        double remaining_payment; //остаток основного долга


        public Payment(LocalDate date, double payment, double interest, double principal_part_of_payment, double remaining_payment) {
            this.date = date;
            this.payment = payment;
            this.interest = interest;
            this.principal_part_of_payment = principal_part_of_payment;
            this.remaining_payment = remaining_payment;
        }
    }

    static void printGraph(List<Payment> graph) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        System.out.printf("%-12s %-15s %-15s %-15s %-15s%n",
                "Дата", "Платёж", "Проценты", "Основной долг", "Остаток");
        for (Payment e : graph) {
            System.out.printf("%-12s %-15.2f %-15.2f %-15.2f %-15.2f%n",
                    e.date.format(formatter), e.payment,e.interest, e.principal_part_of_payment, e.remaining_payment);
        }
    }

    static class HolidayChecker{
        private static final HttpClient httpClient = HttpClient.newHttpClient(); //класс из пакета java.net.http,
                                                                                 // умеющий отправлять  HTTP-запросы
        public static boolean isHolidayOrNot(LocalDate date){
            try{
                int year = date.getYear();
                int month = date.getMonthValue();
                int day = date.getDayOfMonth();
                String url = "https://isdayoff.ru/api/getdata?year="+ year + "&month=" + month + "&day=" + day;; //формируем строку запроса
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build(); // создаем строитель запроса,
                //указываем адрес, указываем метод GET, так как нам надо получить информацию, создаем обьект запроса

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body().trim();
                return "1".equals(body);
            }
            catch (Exception e){
                System.err.println("Ошибка при обращении к isdayoff.ru: " + e.getMessage());
                return false;
            }
        }

    }
}
