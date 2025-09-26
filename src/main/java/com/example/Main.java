package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static boolean callSorted;
    static String today;
    static LocalTime todayTime;
    static LocalTime nextDayPublish = LocalTime.of(13,0);
    static DateTimeFormatter onlyHourFormatter = DateTimeFormatter.ofPattern("HH");
    static DateTimeFormatter digitalFormatter = DateTimeFormatter.ofPattern("HH:mm");
    static DateTimeFormatter todayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
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
    //TODO Charching. anväder dagens datum. kan sträcka sig till morgondagen om det finns priser
    //todo metod som tar två dagars lista, (om det finns) med parameter för 2 4 eller 8 timmars laddning
    //todo parametern får vara hur många timmar som adderas ihop från början.
    //argument hantering, ska bara behöva zone och charging,  ignorera resten om den hittas.
    public static void main(String[] args) {
        elAPI = new ElpriserAPI();
        zone = null;
        callDate = null;
        validZone = false;
        validDate = false;
        callSorted = false;
        today = LocalDate.now().toString();
        todayTime = LocalTime.now();

        charingWindow("SE4");


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
                    System.out.println("Found argument but no parameters.");
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
            callDate = today;
            sortedList();
        } else if(!validDate && !callSorted) {
            callDate = today;
            unSortedList();
        }else{
            helpPrint();
        }
    }

    private static void charingWindow(String zone){
        //todo fixa om klockan är efter 13 så testa få priser för morgondagen.
        //TODO fixa om det inte kommer för idag men bara imorgon måste det framgå.
        String tomorrow= null;
        List<ElpriserAPI.Elpris> priceList = new ArrayList();
        List<ElpriserAPI.Elpris> todayList = getPriceList(today, zone);
        priceList.addAll(todayList);
        if(todayTime.isAfter(nextDayPublish)){
            tomorrow = LocalDate.now().plusDays(1).toString();
            List<ElpriserAPI.Elpris> tomorrowList = getPriceList(tomorrow, zone);
            priceList.addAll(tomorrowList);
        }

        int window = 4;
        int length = priceList.size();
        int beguinHour = 0;
        int stopHour = 0;
        String startHour;
        String endHour;
        double windowSum = 0;
        double minSum = 0;
        double aveSum =0;
        //gör en enum för window, så jag får med int värdet typ.
        //eller kan jag jämföra bara 2h -> 2 osv. mycket felkällor väl?
        for (int i = 0; i < window; i++){
            windowSum += priceList.get(i).sekPerKWh();
        }
        stopHour = window-1;
        minSum = windowSum;
        //sliding window närmar sig
        for(int i = window; i < length; i++){
            windowSum += priceList.get(i).sekPerKWh() - priceList.get(i - window).sekPerKWh();
            if(windowSum < minSum){
                minSum = windowSum;
                beguinHour = i-1;
                stopHour = beguinHour+window-1;
            }
        }
        //TODO skicka till utskrift metod. Fixa formatering, double utskrift atm.
        aveSum = (minSum / window)*100;
        startHour = priceList.get(beguinHour).timeStart().format(digitalFormatter);
        endHour = priceList.get(stopHour).timeEnd().format(digitalFormatter);
        //spara timmarna..? index värdet kanske.
        System.out.println("Start charging: "+startHour + " stop at "+ endHour+ " Snittpris under den tiden " + aveSum +" öre");

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
        String highPrisTidStart = copy.getFirst().timeStart().format(onlyHourFormatter);
        var highPrisTidSlut = copy.getFirst().timeEnd().format(onlyHourFormatter);
        var lowPris = copy.getLast().sekPerKWh();
        var lowPrisTidStart = copy.getLast().timeStart().format(onlyHourFormatter);
        var lowPrisTidSlut = copy.getLast().timeEnd().format(onlyHourFormatter);
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
                
                Exampel of command
                --zone se2 --date 2025-09-20 --sorted
                --zone se4 --sorted
                
                To know the best time to charge you car you can
                input --charging with time frame of 2, 4 or 6 hours
                -----------------------------""");
    }

}