package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class Main {
    public static ElpriserAPI elAPI;
    public static String zone;
    public static String callDate;
    public static boolean validZone;
    public static boolean validDate;
    public static boolean callSorted;
    static DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");
    enum zoneChoise {SE1, SE2, SE3, SE4}


    public record highestLowPrice(double highPrisOre, String highPrisTidStart, String highPrisTidSlut,
                                  double lowPrisOre, String lowPrisTidStart, String lowPrisTidSlut, double avePrisOre) {
    }

    //TODO Om ordningen fuckar, lägg till/ta bort .reversed()
    private static void sortedList() {
        List<ElpriserAPI.Elpris> elPrisLista = getPriceList(callDate, zone);
        if (elPrisLista.size() == 0) {
            return;
        }
        highestLowPrice result = getPriceHighLow(elPrisLista);
        highLowAvePrinter(result);
        elPrisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));//reversed för decending/ascending utan
        pricePrinter(elPrisLista);
    }

    private static void unSortedList() {
        List<ElpriserAPI.Elpris> elPrisLista = getPriceList(callDate, zone);
        if (elPrisLista.size() == 0) {
            return;
        }
        highestLowPrice result = getPriceHighLow(elPrisLista);
        highLowAvePrinter(result);
        pricePrinter(elPrisLista);
    }

    private static void dateInput(String arg) {
        callDate = arg;
        try {
            callDate = LocalDate.parse(callDate, DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date, using todays date instead");
            callDate = LocalDate.now().toString();
        }
        validDate = true;
    }

    private static void zoneInput(String arg) {
        arg = arg.toUpperCase();
        try {
            zone = zoneChoise.valueOf(arg).toString();
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid zone, Must choose between SE1,SE2,SE3,SE4. Type --help for more information.");
            return;
        }
        validZone = true;
    }

    //TODO Sliding window, då sparas ba 2 värden, och när vi flyttar ett steg frammåt så tas den bort som lämnades.
    public static void main(String[] args) {
        elAPI = new ElpriserAPI();
        zone = null;
        callDate = null;
        validZone = false;
        validDate = false;
        callSorted = false;


        if (args.length == 0) {
            helpPrint();
            return;
        } else {
            for (int i = 0; i < args.length; i++) {
                try{
                switch (args[i].toLowerCase()) {
                    case "--help" -> {
                        helpPrint();
                        return;
                    }
                    case "--sorted" -> callSorted = true;
                    case "--zone" -> zoneInput(args[i + 1]);
                    case "--date" -> dateInput(args[i + 1]);
                }}catch(ArrayIndexOutOfBoundsException e){
                    System.out.println("Found --date argument but no date.");
                }
            }
        }
        if (!validZone) {
            helpPrint();
        } else if (validDate && callSorted) {
            sortedList();
        } else if (validDate && !callSorted) {
            unSortedList();
        } else if (!validDate && callSorted) {
            callDate = LocalDate.now().toString();
            sortedList();
        } else if(!validDate && !callSorted) {
            callDate = LocalDate.now().toString();
            unSortedList();
        }else{
            helpPrint();
        }
    }

    private static List getPriceList(String callDate, String zone) {
        List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, com.example.api.ElpriserAPI.Prisklass.valueOf(zone));
        if (testPriser.size() == 0) {
            System.out.println("No data found online.");
            return Collections.emptyList();
        }
        return testPriser;
    }

    private static highestLowPrice getPriceHighLow(List<ElpriserAPI.Elpris> elpriser) {
        List<ElpriserAPI.Elpris> copy = new ArrayList<>(elpriser);
        copy.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed());
        double avePrice;
        double sumPrice = 0;
        double highPris = copy.getFirst().sekPerKWh();
        String highPrisTidStart = copy.getFirst().timeStart().format(hourFormatter);
        var highPrisTidSlut = copy.getFirst().timeEnd().format(hourFormatter);
        var lowPris = copy.getLast().sekPerKWh();
        var lowPrisTidStart = copy.getLast().timeStart().format(hourFormatter);
        var lowPrisTidSlut = copy.getLast().timeEnd().format(hourFormatter);
        for (int i = 0; i < copy.size(); i++) {
            sumPrice += copy.get(i).sekPerKWh();
        }
        //fixa tiden så att start och slut tiden är tex 01-02 istället för 01:00-02:00
        //skriv ut i öre istället för kr.
        avePrice = sumPrice / copy.size();
        double highPrisOre = highPris * 100;
        double lowPrisOre = lowPris * 100;
        double avePrisOre = avePrice * 100;
        return new highestLowPrice(highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
    }

    private static void highLowAvePrinter(highestLowPrice prices) {
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
                        pris.timeStart().format(hourFormatter), pris.timeEnd().format(hourFormatter), pris.sekPerKWh() * 100)
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
                
                Exampel of command
                --zone se2 --date 2025-09-20 --sorted
                --zone se4 --sorted
                
                To know the best time to charge you car you can
                input --charging with time frame of 2, 4 or 6 hours
                -----------------------------""");
    }

}