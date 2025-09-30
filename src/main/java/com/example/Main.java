package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class Main {
    private static ElpriserAPI elAPI;
    private static String zone;
    private static String callDate;
    private static boolean validZone;
    private static boolean validDate;
    private static boolean validWindow;
    private static boolean callSorted;
    static String today;
    static LocalTime todayTime;
    static LocalTime nextDayPublish = LocalTime.of(13, 0);
    static DateTimeFormatter onlyHourFormatter = DateTimeFormatter.ofPattern("HH");
    static DateTimeFormatter digitalFormatter = DateTimeFormatter.ofPattern("HH:mm");
    static DateTimeFormatter dayDate = DateTimeFormatter.ofPattern("dd");

    enum zoneChoise {SE1, SE2, SE3, SE4}

    static int window;





    public static void main(String[] args) {
        Locale.setDefault(Locale.of("sv","se"));
        elAPI = new ElpriserAPI();
        zone = null;
        callDate = null;
        validZone = false;
        validDate = false;
        callSorted = false;
        validWindow = false;
        today = LocalDate.now().toString();
        todayTime = LocalTime.now();
        window = 0;


        if (args.length == 0) {
            helpPrint();
            return;
        } else {
            for (int i = 0; i < args.length; i++) {
                try {
                    switch (args[i].toLowerCase()) {
                        case "--help" -> {
                            helpPrint();
                            return;
                        }
                        case "--sorted" -> callSorted = true;
                        case "--zone" -> {
                            if (args.length > i + 1) {
                                zoneInput(args[i + 1]);
                                i++;
                            } else {
                                System.out.println("Missing zone argument");
                            }
                        }
                        case "--charging" -> {
                            if (args.length > i + 1) {
                                charginInput(args[i + 1]);
                                i++;
                            } else {
                                System.out.println("Missing charging window, using default");
                                charginInput("default value");
                            }
                        }
                        case "--date" -> {
                            if (args.length > i + 1) {
                                dateInput(args[i + 1]);
                                i++;
                            } else {
                                System.out.println("Missing date argument");
                                dateInput("faltyDate");
                            }
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Found argument but no parameters.");
                    helpPrint();
                }
            }
        }
        if (!validZone) {
            helpPrint();
        } else if (validWindow) {
            charingWindow();
        } else if (validDate && callSorted) {
            sortedList();
        } else if (validDate && !callSorted) {
            unSortedList();
        } else if (!validDate && callSorted) {
            callDate = today;
            sortedList();
        } else if (!validDate && !callSorted) {
            callDate = today;
            unSortedList();
        } else {
            helpPrint();
        }
    }

    private static void charginInput(String arg) {
        switch (arg) {
            case "8h" -> {
                window = 8;
                validWindow = true;
            }
            case "4h" -> {
                window = 4;
                validWindow = true;
            }
            case "2h" -> {
                window = 2;
                validWindow = true;
            }
            default -> {
                window = 2;
                validWindow = true;
                System.out.println("Invalid window input, must be 2h|4h|8h. Using default of 2h");
            }
        }
    }
    //TODO Om ordningen fuckar, lägg till/ta bort .reversed() på rad 141, elPrisLista.sort
    private static void sortedList() {
        List<ElpriserAPI.Elpris> elPrisLista = getPricesForMoreDays();
        if(elPrisLista.size()==0){
            System.out.println("Inga priser funna");
        }
        priceStatistics result = getPriceStatistics(elPrisLista);
        highLowAvePrinter(result);
        elPrisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed().thenComparing(ElpriserAPI.Elpris::timeStart));//reversed för decending/ascending utan
        pricePrinter(elPrisLista);

    }


    private static void unSortedList() {
        List<ElpriserAPI.Elpris> elPrisLista = getPricesForMoreDays();
        if(elPrisLista.size()==0){
            System.out.println("Inga priser funna");
            return;
        }
        priceStatistics result = getPriceStatistics(elPrisLista);
        highLowAvePrinter(result);
        pricePrinter(elPrisLista);
    }

    private static void dateInput(String arg) {
        callDate = arg;
        try {
            callDate = LocalDate.parse(callDate, DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date, using todays date instead");
            callDate = today;
        }
        validDate = true;
    }

    private static void zoneInput(String arg) {
        arg = arg.toUpperCase();
        try {
            zone = zoneChoise.valueOf(arg).toString();
            validZone = true;
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid zone, Must choose between SE1,SE2,SE3,SE4. Type --help for more information.");
        }
    }

    private static List<ElpriserAPI.Elpris> getPricesForMoreDays() {
        String tomorrow = "";
        List<ElpriserAPI.Elpris> totalList = new ArrayList<>();
        if (!validDate) {
            List<ElpriserAPI.Elpris> todayList = getPriceList(today, zone);
            totalList.addAll(todayList);
            if(callSorted) {
                tomorrow = LocalDate.parse(today).plusDays(1).toString();
                List<ElpriserAPI.Elpris> tomorrowList = getPriceList(tomorrow, zone);
                totalList.addAll(tomorrowList);
            }
            if(totalList.size()==0){
                System.out.println("Ingen data tilgänglig");
                return Collections.emptyList();
            }
            return totalList;
        } else {
            List<ElpriserAPI.Elpris> callList = getPriceList(callDate, zone);
            totalList.addAll(callList);
            tomorrow = LocalDate.parse(callDate).plusDays(1).toString();
            List<ElpriserAPI.Elpris> tomorrowList = getPriceList(tomorrow, zone);
            totalList.addAll(tomorrowList);
            if(totalList.size()==0){
                System.out.println("Ingen data tilgänglig");
                return Collections.emptyList();
            }
            return totalList;
        }
    }

    private static void charingWindow() {
        List<ElpriserAPI.Elpris> priceList = getPricesForMoreDays();
        int length = priceList.size();
        int beguinHour =1;
        int stopHour;
        String startDate;
        String startHour;
        String endHour;
        double windowSum = 0;
        double minSum;
        double aveSum;
        //sliding window
        for (int i = 0; i < window; i++) {
            windowSum += priceList.get(i).sekPerKWh();
        }
        stopHour = window - 1;
        minSum = windowSum;
        for (int i = window; i < length; i++) {
            windowSum += priceList.get(i).sekPerKWh() - priceList.get(i - window).sekPerKWh();
            if (windowSum < minSum) {
                minSum = windowSum;
                beguinHour = i - window + 1;
                stopHour = i;
            }
        }
        aveSum = (minSum / window) * 100;
        startDate = priceList.get(beguinHour).timeStart().format(dayDate);
        startHour = priceList.get(beguinHour).timeStart().format(digitalFormatter);
        endHour = priceList.get(stopHour).timeEnd().format(digitalFormatter);
        windowPrinter(startDate,startHour, endHour, aveSum);
    }

    private static List<ElpriserAPI.Elpris>getPriceList(String callDate, String zone) {
        List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, com.example.api.ElpriserAPI.Prisklass.valueOf(zone));
        if (testPriser.size() == 0) {
            System.out.println("No data found online.");
            return Collections.emptyList();
        }
        return testPriser;
    }

    private static priceStatistics getPriceStatistics(List<ElpriserAPI.Elpris> elpriser) {
        List<ElpriserAPI.Elpris> copy = new ArrayList<>(elpriser);
        //copy.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed());
        List<hourPriceAverage> hourAverageList = new ArrayList<>();
        double avePrice;
        double sumPrice = 0;
        double hourSum;
        double hourAverage;
        double highPris = copy.getFirst().sekPerKWh();
        hourPriceAverage maxValue = null;
        hourPriceAverage minValue = null;
        String lowPrisTidStart = "";
        String lowPrisTidSlut = "";
        String highPrisTidStart = "";
        String highPrisTidSlut = "";
        //String highPrisTidStart = copy.getFirst().timeStart().format(onlyHourFormatter);
        //String highPrisTidSlut = copy.getFirst().timeEnd().format(onlyHourFormatter);
        double lowPris = copy.getLast().sekPerKWh();
        //String lowPrisTidStart = copy.getLast().timeStart().format(onlyHourFormatter);
        //String lowPrisTidSlut = copy.getLast().timeEnd().format(onlyHourFormatter);
        if(copy.size()>48){
            int indexSize = copy.size();
            double totalSum = 0;
            for (int i = 0; i < indexSize; i+=4) {
                hourSum = 0;
                for (int quarter = 0; quarter < 4; quarter++) {
                    hourSum += copy.get(i+quarter).sekPerKWh();
                }
                hourAverage = hourSum / 4;
                totalSum += hourAverage;
                hourAverageList.add(new hourPriceAverage(hourAverage,copy.get(i).timeStart(),copy.get(i+3).timeEnd()));
                //addera 4 timmar, div med 4
            }
            //addera sedan alla dessa medel till en hel, och dela det med 24.
            avePrice = totalSum / 24;

            //Kanske måste göra en record, varje 4 så sparas timmen också, sedan sortera eller hitta max/min borde rätt timme hänga med
            //minValue = Collections.min(hourPriceAverage);
        }else {
            for (int i = 0; i < copy.size(); i++) {
                sumPrice += copy.get(i).sekPerKWh();
                hourAverageList.add(new hourPriceAverage(copy.get(i).sekPerKWh(),copy.get(i).timeStart(),copy.get(i).timeEnd()));
            }
            avePrice = sumPrice / copy.size();

        }
        maxValue = Collections.max(hourAverageList);
        minValue = Collections.min(hourAverageList);
        lowPrisTidStart = minValue.priceStart.format(onlyHourFormatter);
        lowPrisTidSlut = minValue.priceEnd.format(onlyHourFormatter);
        highPrisTidStart = maxValue.priceStart.format(onlyHourFormatter);
        highPrisTidSlut = maxValue.priceEnd.format(onlyHourFormatter);


        double highPrisOre = maxValue.hourAverage * 100;
        double lowPrisOre = minValue.hourAverage * 100;
        double avePrisOre = avePrice * 100;
        return new priceStatistics(highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
    }

    private static void windowPrinter(String startDate, String startHour, String endHour, double aveSum) {
        System.out.printf("""
                Påbörja laddning den %se kl %s - %s 
                Medelpris för fönster: %.2f öre\n""", startDate, startHour, endHour, aveSum);
    }

    private static void highLowAvePrinter(priceStatistics prices) {
        System.out.printf("""
                Högsta pris:  %.2f öre/kWh  Tid: %s-%s
                Lägsta pris:  %.2f öre/kWh  Tid: %s-%s
                Medelpris: %.2f öre\n""", prices.highPrisOre, prices.highPrisTidStart, prices.highPrisTidSlut, prices.lowPrisOre, prices.lowPrisTidStart, prices.lowPrisTidSlut, prices.avePrisOre);
    }

    private static void pricePrinter(List<ElpriserAPI.Elpris> elpriser) {
        System.out.println("Priser funna för dagen: " + elpriser.size() + "\n");
        elpriser.stream().forEach(pris ->
                System.out.printf("""
                                %s-%s %.2f öre\n""",
                        pris.timeStart().format(onlyHourFormatter), pris.timeEnd().format(onlyHourFormatter), pris.sekPerKWh() * 100)
        );
    }//TODO testet gillar inte att det står något framför timeStart.


    public static void helpPrint() {
        System.out.printf("""
                -------HELP PAGE------------
                 To best usage of this program.
                input zone is required SE1|SE2|SE3|SE4 with the argument --zone to get the right area.
                Next date if you wish to see other than todays prices. Use --date with YYYY-MM-DD
                Tomorrows prices are provied after 13:00.
                
                If you wish to see the list sorted in acending price order,
                include --sorted in the arguments. Otherwise it will be sorted by the hour.
                
                To know the best time to charge you car you can
                input --charging with 2h|4h|8h representing time frame of 2, 4 or 6 hours
                
                Exampel of command
                --zone se2 --date 2025-09-20 --sorted
                --zone se4 --sorted
                --zone se3 --charging 4h
                
                
                -----------------------------""");
    }

    public record priceStatistics(double highPrisOre, String highPrisTidStart, String highPrisTidSlut,
                                  double lowPrisOre, String lowPrisTidStart, String lowPrisTidSlut, double avePrisOre) {
    }
    public record hourPriceAverage (double hourAverage, ZonedDateTime priceStart, ZonedDateTime priceEnd) implements Comparable<hourPriceAverage> {
        @Override
        public int compareTo(hourPriceAverage other) {
            return Double.compare(this.hourAverage, other.hourAverage);
        }
    }

}