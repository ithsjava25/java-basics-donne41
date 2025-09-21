package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elAPI = new ElpriserAPI();

//        System.out.print(elAPI.getPriser(datum, ElpriserAPI.Prisklass.SE4));
        //klassen fattade inte LocalDate format, tvunget med parse toString.

        //dela upp input, hittar, help, eller ska det vara som standard
        //elzone är obligatoriskt
        //inget datum, använda dagens datum.
        //parse datum så det kan användas.
        //testa om regex separerar daturmen
        //sotera en lista
        //Presentera fint
        //bättre input hantering


        List<String> validZone = Arrays.asList("SE1", "SE2", "SE3", "SE4");
        String validDate = "202";
        String help;
        String zone = null;
        LocalDate askDate = null;
        LocalDate callDate = null;
        boolean valid = false;
        double sumPris = 0;
        double avePris = 0;

        for (String arg : args) {
            if (validZone.contains(arg.toUpperCase())) {
                zone = arg;
                zone = zone.toUpperCase();
                System.out.println(zone);
                valid = true;
            }
            if (arg.contains(validDate)) {
                try {
                    callDate = LocalDate.parse(arg, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    callDate = LocalDate.parse(java.time.LocalDate.now().toString());
                    //falskt datum, använd dagens datum.
                }
            }
            if (valid == true && callDate == null) {
                callDate = LocalDate.parse(java.time.LocalDate.now().toString());
            }
        }
        if (valid) {
            List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, ElpriserAPI.Prisklass.valueOf(zone));
            if (testPriser == null) {
                System.out.println("Kunde inte hitta priser");
                System.out.println("startar manuell input.");
                manuellInput();

            } else {
                testPriser.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));
                var highPris = testPriser.getLast().sekPerKWh();
                var highPrisTidStart = testPriser.getLast().timeStart().toLocalTime().toString().substring(0, 2);
                var highPrisTidSlut = testPriser.getLast().timeEnd().toLocalTime().toString().substring(0, 2);
                var lowPris = testPriser.getFirst().sekPerKWh();
                var lowPrisTidStart = testPriser.getFirst().timeStart().toLocalTime().toString().substring(0, 2);
                var lowPrisTidSlut = testPriser.getFirst().timeEnd().toLocalTime().toString().substring(0, 2);
                for (int i = 0; i < testPriser.size(); i++) {
                    sumPris += testPriser.get(i).sekPerKWh();
                }
                //fixa tiden så att start och slut tiden är tex 01-02 istället för 01:00-02:00
                //skriv ut i öre istället för kr.
                avePris = sumPris / testPriser.size();
                double highPrisOre = highPris * 100;
                double lowPrisOre = lowPris * 100;
                double avePrisOre = avePris * 100;
                System.out.printf("""
                        Högsta pris:  %.4f öre/kWh  Tid: %s-%s
                        Lägsta pris:  %.4f öre/kWh  Tid: %s-%s
                        Medelpris:    %.4f öre/kWh \n""", highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);

            }
        }
    }

    public static void manuellInput() {
        Scanner sc = new Scanner(System.in);
        ElpriserAPI elAPI = new ElpriserAPI();
        boolean validInput = false;
        String zone = null;
        String callDate = null;
        double avePris = 0;
        double sumPris = 0;
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
            if (validInput == true && callDate == null) {
                callDate = LocalDate.now().toString();
            }
        } while (!validInput);

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
                sumPris += testPriser.get(i).sekPerKWh();
            }
            //fixa tiden så att start och slut tiden är tex 01-02 istället för 01:00-02:00
            //skriv ut i öre istället för kr.
            avePris = sumPris / testPriser.size();
            double highPrisOre = highPris * 100;
            double lowPrisOre = lowPris * 100;
            double avePrisOre = avePris * 100;
            System.out.printf("""
                    Högsta pris:  %.4f öre/kWh  Tid: %s-%s
                    Lägsta pris:  %.4f öre/kWh  Tid: %s-%s
                    Medelpris:    %.4f öre/kWh  \n""", highPrisOre, highPrisTidStart, highPrisTidSlut, lowPrisOre, lowPrisTidStart, lowPrisTidSlut, avePrisOre);

        }

//            System.out.println("Dagens priser" + testPriser.size());
//            testPriser.stream().forEach(pris ->
//                    System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
//                            pris.timeStart().toLocalTime(), pris.sekPerKWh()));
//        }
    }


    public static void helpPrint() {
        System.out.printf("""
                THIS IS THE HELP SCREEN TO BE
                NOT YET FINISHED AS SEEN HERE """);
    }
}

