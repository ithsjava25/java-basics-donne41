package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class Main {
    public static final ElpriserAPI elAPI = new ElpriserAPI();
    public static String zone = null;
    public static String callDate = null;
    public static boolean validInput = false;
    public static boolean validZone = false;
    public static boolean validDate = false;
    public static boolean validArg = false;
    public static boolean callSorted = false;
    enum zoneChoise { SE1,SE2,SE3,SE4}
    static double avePrice = 0;
    static double sumPrice = 0;


    private static void sortedList() {
        //TODO get a sorted list from the zone, date acending in price.
        List<ElpriserAPI.Elpris> elPrisLista = getPriceList(callDate, zone);
        elPrisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));
        pricePrinter(elPrisLista);
    }
    private static void unSortedList(){
        List<ElpriserAPI.Elpris> elPrisLista = getPriceList(callDate, zone);
        elPrisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));
        getPriceHighLow(elPrisLista);
        elPrisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::timeStart));
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
        ;
        validDate = true;
        //todo set the calldate and call back to get list.
    }

    private static void zoneInput(String arg) {
        try {
            zone = zoneChoise.valueOf(arg).toString();
        }catch (IllegalArgumentException e) {
            System.out.println("Invalid zone, Must choose between SE1,SE2,SE3,SE4. Type --help for more information.");
            return;
        }
        validZone = true;
    }


    public static void main(String[] args) {
        //TODO Fixa en for each, en som gör en boolean om det finns --argument,
        //TODO Och en sträng som samtidigt kollar input värdet, tex datumet, elzonen.
        //TODO --sorting ska ta datum och presentera dagen alla priser i fallade ordning.
        //APIn fattade inte LocalDate format, tvunget med parse toString.
        String[] validArgs = {"--help", "--zone", "--date", "--sorted"};
        //List<String> validZone = Arrays.asList("SE1", "SE2", "SE3", "SE4");
        enum zone {SE1, SE2, SE3, SE4}
        String inputZone = null;
        //String validDate = "202";

        if (args.length == 0) {
            System.out.println("Start argument är tom, startar manuell inmatning." +
                    "usage is with a zone and optional date to see sorted list of highest,lowset and average price.");
            helpPrint();
            return;
        } else {

            for (int i = 0; i < args.length; i++) {
                switch (args[i].toLowerCase()) {
                    case "--help" -> {
                        helpPrint();
                        return;
                    }
                    case "--zone" -> zoneInput(args[i + 1]);
                    case "--date" -> dateInput(args[i + 1]);
                    case "--sorted" -> callSorted = true;
                }

            }
        }
        if (validZone && validDate && callSorted) {
            sortedList();
            return;

        } else if (validZone && validDate != callSorted) {
            unSortedList();
        }else {
            helpPrint();
            return;
        }


    }

    //todo två metoder, en som är sorted för hela dagen, stigade. en som presenterar högsta lägsta.
    //todo borde kunna återanvända en av metoderna.
    private static List getPriceList(String callDate, String zone) {
        List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, ElpriserAPI.Prisklass.valueOf(zone));
        if (testPriser.size() == 0) {
            callDate = LocalDate.now().toString();
            testPriser = elAPI.getPriser(callDate, ElpriserAPI.Prisklass.valueOf(zone));

       }
        return testPriser;
    }

    private static void getPriceHighLow(List<ElpriserAPI.Elpris> elpriser) {
        double highPris = elpriser.getLast().sekPerKWh();
        String highPrisTidStart = elpriser.getLast().timeStart().toLocalTime().toString().substring(0, 2);
        var highPrisTidSlut = elpriser.getLast().timeEnd().toLocalTime().toString().substring(0, 2);
        var lowPris = elpriser.getFirst().sekPerKWh();
        var lowPrisTidStart = elpriser.getFirst().timeStart().toLocalTime().toString().substring(0, 2);
        var lowPrisTidSlut = elpriser.getFirst().timeEnd().toLocalTime().toString().substring(0, 2);
        for (int i = 0; i < elpriser.size(); i++) {
            sumPrice += elpriser.get(i).sekPerKWh();
        }
        //fixa tiden så att start och slut tiden är tex 01-02 istället för 01:00-02:00
        //skriv ut i öre istället för kr.
        avePrice = sumPrice / elpriser.size();
        double highPrisOre = highPris * 100;
        double lowPrisOre = lowPris * 100;
        double avePrisOre = avePrice * 100;
        highLowAvePrinter(highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
    }

    private static void highLowAvePrinter(double highPrisOre, String highPrisTidStart, String highPrisTidSlut, double lowPrisOre, String lowPrisTidStart, String lowPrisTidSlut, double avePrisOre) {
        System.out.printf("""
                Högsta pris:  %.4f öre/kWh  Tid: %s-%s
                Lägsta pris:  %.4f öre/kWh  Tid: %s-%s
                Medelpris:    %.4f öre/kWh \n""", highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
    }

    private static void pricePrinter(List<ElpriserAPI.Elpris> elpriser) {
        System.out.println("Priser funna för dagen: " + elpriser.size() + "\n");
        getPriceHighLow(elpriser);
        elpriser.stream().forEach(pris ->
                System.out.printf("""
                                Pris: %.4f SEK/kWh Tidstart: %s   Tidslut: %s \n""",
                        pris.sekPerKWh(), pris.timeStart().toLocalTime(), pris.timeEnd().toLocalTime())
        );
    }




    public static void helpPrint() {
        System.out.printf("""
                -------HELP PAGE------------
                 To best use this program.
                 input --zone is required SE1|SE2|SE3|SE4 to get the right area.
                Next --date if you wish to see other than todays prices.
                Tomorrows prices are provied after 13:00.
                
                If you wish to see the list sorted in acending price order,
                include --sorted in the arguments. Otherwise it will be sorted by the hour.
                
                To know the best time to charge you car you can
                input --charging with time frame of 2, 4 or 6 hours
                -----------------------------""");
    }

}


