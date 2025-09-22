package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class Main {
    public static final ElpriserAPI elAPI = new ElpriserAPI();
    public static Scanner sc = new Scanner(System.in);
    public static String zone = null;
    public static String callDate = null;
    public static boolean validInput = false;
    static double avePrice = 0;
    static double sumPrice = 0;


    public static void main(String[] args) {

        //APIn fattade inte LocalDate format, tvunget med parse toString.
        if(args.length == 0){
            System.out.println("Start argument är tom, startar manuell inmatning");
            manuellInput();
        }


        List<String> validZone = Arrays.asList("SE1", "SE2", "SE3", "SE4");
        String validDate = "202";
        String help;

        for (String arg : args) {
            if (validZone.contains(arg.toUpperCase())) {
                zone = arg;
                zone = zone.toUpperCase();
                System.out.println(zone);
                validInput = true;
            }
            if (arg.contains(validDate)) {
                try {
                    //nödvändig om det redan är rätt datum? extra koll om det är rätt skrivet annars throw
                    callDate = LocalDate.parse(arg, DateTimeFormatter.ISO_LOCAL_DATE).toString();
                } catch (DateTimeParseException e) {
                    System.out.println("Felaktig datuminmatning, använder dagens datum istället, skriv YYYY-MM-DD");
                }
            }
        }
        if (validInput && callDate == null) {
            callDate = LocalDate.now().toString();
            getPriceList(callDate,zone);
        }else if (validInput && callDate != null){
            getPriceList(callDate,zone);
        }else {
            manuellInput();
        }

    }

    public static void manuellInput() {

        validInput = false;
        zone = null;
        callDate = null;
        do {
            String[] input = sc.nextLine().toUpperCase().split(" ");
            for (int i = 0; i < input.length; i++) {
                input[i] = input[i].toUpperCase();
                if (input.length <= 0) {
                    System.out.println("Type --help for more information");
                    break;
                } else if (input[i].equals("--HELP")) {
                    helpPrint();
                } else if (input[i].equals("--ZONE")) {
                    zone = input[i + 1];
                    i++;
                    validInput = true;
                } else if (input[i].equals("--DATE")) {
                    try {
                        callDate = LocalDate.parse(input[i + 1], DateTimeFormatter.ISO_LOCAL_DATE).toString();
                    } catch (DateTimeParseException e) {
                        callDate = LocalDate.now().toString();
                    }
                } else {
                    System.out.println("Zone not included as requierd. type --help for more information");
                }
            }
            if (validInput && callDate == null) {
                callDate = LocalDate.now().toString();
            }
        } while (!validInput);

        getPriceList(callDate, zone);

    }

    private static void getPriceList(String callDate, String zone) {
        List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, ElpriserAPI.Prisklass.valueOf(zone));
        if (testPriser == null) {
            System.out.println("Kunde inte hitta priser");
        } else {
            testPriser.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));
            var highPris = testPriser.getLast().sekPerKWh();
            var highPrisTidStart = testPriser.getLast().timeStart().toLocalTime().toString().substring(0, 2);
            var highPrisTidSlut = testPriser.getLast().timeEnd().toLocalTime().toString().substring(0, 2);
            var lowPris = testPriser.getFirst().sekPerKWh();
            var lowPrisTidStart = testPriser.getFirst().timeStart().toLocalTime().toString().substring(0, 2);
            var lowPrisTidSlut = testPriser.getFirst().timeEnd().toLocalTime().toString().substring(0, 2);
            for (int i = 0; i < testPriser.size(); i++) {
                sumPrice += testPriser.get(i).sekPerKWh();
            }
            //fixa tiden så att start och slut tiden är tex 01-02 istället för 01:00-02:00
            //skriv ut i öre istället för kr.
            avePrice = sumPrice / testPriser.size();
            double highPrisOre = highPris * 100;
            double lowPrisOre = lowPris * 100;
            double avePrisOre = avePrice * 100;
            highLowAvePrinter(highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
            manuellInput();
        }
    }

    private static void highLowAvePrinter(double highPrisOre, String highPrisTidStart, String highPrisTidSlut, double lowPrisOre, String lowPrisTidStart, String lowPrisTidSlut, double avePrisOre) {
        System.out.printf("""
                Högsta pris:  %.4f öre/kWh  Tid: %s-%s
                Lägsta pris:  %.4f öre/kWh  Tid: %s-%s
                Medelpris:    %.4f öre/kWh \n""", highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);
    }


    public static void helpPrint() {
        System.out.printf("""
                THIS IS THE HELP SCREEN TO BE
                NOT YET FINISHED AS SEEN HERE """);
    }
}

