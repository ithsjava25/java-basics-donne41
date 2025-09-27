package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
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
    static String today;
    static LocalTime todayTime;
    static LocalTime nextDayPublish = LocalTime.of(13, 0);
    static DateTimeFormatter onlyHourFormatter = DateTimeFormatter.ofPattern("HH");
    static DateTimeFormatter digitalFormatter = DateTimeFormatter.ofPattern("HH:mm");
    enum zoneChoise {SE1, SE2, SE3, SE4}
    static int window;



    //TODO Om ordningen fuckar, lägg till/ta bort .reversed() på rad 42, elPrisLista.sort
    private static void sortedList() {
        List<ElpriserAPI.Elpris> elPrisLista = getPriceList(callDate, zone);
        if (elPrisLista.size() == 0) {
            return;
        }
        priceStatistics result = getPriceStatistics(elPrisLista);
        highLowAvePrinter(result);
        elPrisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));//reversed för decending/ascending utan
        pricePrinter(elPrisLista);
    }

    private static void unSortedList() {
        List<ElpriserAPI.Elpris> elPrisLista = getPriceList(callDate, zone);
        if (elPrisLista.size() == 0) {
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
            return;
        }
    }

    public static void main(String[] args) {
        elAPI = new ElpriserAPI();
        zone = null;
        callDate = null;
        validZone = false;
        validDate = false;
        boolean callSorted = false;
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
                        case "--zone" -> zoneInput(args[i + 1]);
                        case "--charging" -> charginInput(args[i + 1]);
                        case "--date" -> dateInput(args[i + 1]);
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
                System.out.println("Invalid window input, must be 2h|4h|8h. Using default window of 2h");
            }
        }
    }
    private static List getPricesForMoreDays(){
        String tomorrow = "";
        List<ElpriserAPI.Elpris> totalList = new ArrayList();
        if(!validDate){
            List<ElpriserAPI.Elpris> todayList = getPriceList(today, zone);
            totalList.addAll(todayList);
            if (todayTime.isAfter(nextDayPublish)){
                tomorrow = LocalDate.parse(today).plusDays(1).toString();
                List<ElpriserAPI.Elpris> tomorrowList = getPriceList(tomorrow, zone);
                if (tomorrowList.size() < window) {
                    totalList.addAll(tomorrowList);
                } else {
                    for (int i = 0; i < window; i++) {
                        totalList.add(tomorrowList.get(i));
                    }
                }
            }
            return totalList;
        }else{
            List<ElpriserAPI.Elpris> callList = getPriceList(callDate, zone);
            totalList.addAll(callList);
            if (todayTime.isAfter(nextDayPublish)) {
                tomorrow = LocalDate.parse(callDate).plusDays(1).toString();
                List<ElpriserAPI.Elpris> tomorrowList = getPriceList(tomorrow, zone);
                if (tomorrowList.size() < window) {
                    totalList.addAll(tomorrowList);
                } else {
                    for (int i = 0; i < window; i++) {
                        totalList.add(tomorrowList.get(i));
                    }
                }
            }
            return totalList;
        }
    }

    private static void charingWindow() {
        List<ElpriserAPI.Elpris>priceList = getPricesForMoreDays();
        int length = priceList.size();
        int beguinHour = 0;
        int stopHour;
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
        startHour = priceList.get(beguinHour).timeStart().format(digitalFormatter);
        endHour = priceList.get(stopHour).timeEnd().format(digitalFormatter);
        windowPrinter(startHour, endHour, aveSum);
    }

    private static void windowPrinter(String startHour, String endHour, double aveSum) {
        System.out.printf("""
                Påbörja laddning kl %s - %s 
                Medelpris för fönster: %.2f öre\n""", startHour, endHour, aveSum);
    }

    private static List getPriceList(String callDate, String zone) {
        List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, com.example.api.ElpriserAPI.Prisklass.valueOf(zone));
        if (testPriser.size() == 0) {
            System.out.println("No data found online.");
            return Collections.emptyList();
        }
        return testPriser;
    }

    private static priceStatistics getPriceStatistics(List<ElpriserAPI.Elpris> elpriser) {
        List<ElpriserAPI.Elpris> copy = new ArrayList<>(elpriser);
        copy.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed());
        double avePrice;
        double sumPrice = 0;
        double highPris = copy.getFirst().sekPerKWh();
        String highPrisTidStart = copy.getFirst().timeStart().format(onlyHourFormatter);
        String highPrisTidSlut = copy.getFirst().timeEnd().format(onlyHourFormatter);
        double lowPris = copy.getLast().sekPerKWh();
        String lowPrisTidStart = copy.getLast().timeStart().format(onlyHourFormatter);
        String lowPrisTidSlut = copy.getLast().timeEnd().format(onlyHourFormatter);
        for (int i = 0; i < copy.size(); i++) {
            sumPrice += copy.get(i).sekPerKWh();
        }
        avePrice = sumPrice / copy.size();
        double highPrisOre = highPris * 100;
        double lowPrisOre = lowPris * 100;
        double avePrisOre = avePrice * 100;
        return new priceStatistics(highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
    }

    private static void highLowAvePrinter(priceStatistics prices) {
        System.out.printf("""
                Högsta pris:  %.2f öre/kWh  Tid: %s-%s
                Lägsta pris:  %.2f öre/kWh  Tid: %s-%s
                Medelpris:    %.2f öre/kWh \n""", prices.highPrisOre, prices.highPrisTidStart, prices.highPrisTidSlut, prices.lowPrisOre, prices.lowPrisTidStart, prices.lowPrisTidSlut, prices.avePrisOre);
    }

    private static void pricePrinter(List<ElpriserAPI.Elpris> elpriser) {
        System.out.println("Priser funna för dagen: " + elpriser.size() + "\n");
        elpriser.stream().forEach(pris ->
                System.out.printf("""
                                %s-%s %.2f öre\n""",
                        pris.timeStart().format(onlyHourFormatter), pris.timeEnd().format(onlyHourFormatter), pris.sekPerKWh() * 100)
        );
    }


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

}