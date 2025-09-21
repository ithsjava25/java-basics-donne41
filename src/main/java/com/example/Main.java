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
        LocalDate idag = LocalDate.parse(LocalDate.now().toString());
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


        List<String> validInput = Arrays.asList("SE1", "SE2", "SE3", "SE4");
        String help;
        String zone = null;
        LocalDate askDate = null;
        LocalDate callDate = null;
        boolean valid = false;

        for (String arg : args) {
            if (validInput.contains(arg.toUpperCase())) {
                zone = arg.toUpperCase();
                zone = zone.toUpperCase();
                System.out.println(zone);
                valid = true;
            }
            try {
                callDate = LocalDate.parse(arg, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                callDate = LocalDate.parse(java.time.LocalDate.now().toString());
                //inget datum, använd dagens datum.
            }
        }
        if (valid) {
            System.out.println("PRINTINT ARGS AND TODAY" + idag);
            List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, ElpriserAPI.Prisklass.valueOf(zone));
            if (testPriser == null) {
                System.out.println("Kunde inte hitta priser");
            } else {
                System.out.println("Dagens priser" + testPriser.size());
                testPriser.stream().limit(4).forEach(pris ->
                        System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                                pris.timeStart().toLocalTime(), pris.sekPerKWh()));
            }
        } else {
            System.out.println("Fann inte någon elzone i argument, startar manuell input.");
            manuellInput();
        }
//        for (int i = 0; i<inputArray.length; i++){
//            String s = inputArray[i];
//            try {
//                askDate = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
//            } catch (DateTimeParseException e) {
//                // inte ett datum, hoppa vidare
//            }
//            if (s.equals("--help")||s.isBlank()) {
//                System.out.printf("""
//                    To use this program, input --help for this information screen.
//                    Input a elzone SE1,SE2,SE3 or SE4 and a date to see
//                    Average price for the day
//                    Highest price and what hour
//                    lowest price and what hour.
//                    if date is not included, current day is used.""");
//            }else if(s.equals("--ZONE")){
//                ElpriserAPI.Prisklass.valueOf(inputArray[i]);
//
//                askDate = LocalDate.parse(input);
//                List<ElpriserAPI.Elpris>priser = elAPI.getPriser(askDate, ElpriserAPI.Prisklass.valueOf(inputArray[i + 1]));
//                System.out.print(priser);
//            }else if(s.equals("--DATE")){
//                break;
//            }
//
//        }
//        if(inputArray[0].equals("--help")) {
//            System.out.println("Help executed, with date: " + datum);
//        }

    }

    public static void manuellInput() {
        Scanner sc = new Scanner(System.in);
        ElpriserAPI elAPI = new ElpriserAPI();
        boolean validInput = false;
        String zone = null;
        String callDate = null;
        double avePris = 0;
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
                    validInput = true;
                } else if (input[i].equals("--DATE")) {
                    try {
                        callDate = LocalDate.parse(input[i + 1], DateTimeFormatter.ISO_LOCAL_DATE).toString();
                    } catch (DateTimeParseException e) {
                        callDate = LocalDate.now().toString();
                    }
                } else if (validInput == true && callDate == null) {
                    callDate = LocalDate.now().toString();
                } else {
                    System.out.println("Zone not included as requierd. type --help for more information");
                }
            }
        } while (!validInput);

        List<ElpriserAPI.Elpris> testPriser = elAPI.getPriser(callDate, ElpriserAPI.Prisklass.valueOf(zone));
        if (testPriser == null) {
            System.out.println("Kunde inte hitta priser");
        } else {
            testPriser.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));
            var highPris = testPriser.getLast().sekPerKWh();
            var highPrisTid = testPriser.getLast().timeStart().toLocalTime();
            var lowPris = testPriser.getFirst().sekPerKWh();
            var lowPrisTid = testPriser.getFirst().timeStart().toLocalTime();
            for (int i = 0; i < testPriser.size(); i++){
                double sumPris = testPriser.get(i).sekPerKWh();
                avePris = sumPris/testPriser.size();
            }
            System.out.printf("""
                Highest price: %.4f krTid: %s 
                Lowest price:  %.4f krTid: %s
                average price: %.4f kr""", highPris, highPrisTid, lowPris, lowPrisTid, avePris);


//            System.out.println("Dagens priser" + testPriser.size());
//            testPriser.stream().forEach(pris ->
//                    System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
//                            pris.timeStart().toLocalTime(), pris.sekPerKWh()));
//        }
        }
    }

    public static void helpPrint() {
        System.out.printf("""
                THIS IS THE HELP SCREEN TO BE
                NOT YET FINISHED AS SEEN HERE """);
    }
}
