import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by zhahalski on 26.07.2018.
 */
public class PedroBot extends TelegramLongPollingBot {

    public static void main(String[] args) {
        Date scanDate = null;
        String jarLocation = PedroBot.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
        jarLocation = jarLocation.replace("/TelegramBot.jar", "");
        File resistsFile = new File(jarLocation + "/resists.txt");
        File pedorsFile = new File(jarLocation + "/pedrolist.txt");
        File dateFile = new File(jarLocation + "/date.txt");
        try {
            if(!resistsFile.exists() || resistsFile.isDirectory()) {
                resistsFile.createNewFile();
            }
            if(!pedorsFile.exists() || pedorsFile.isDirectory()) {
                pedorsFile.createNewFile();
            }
            if(!dateFile.exists() || dateFile.isDirectory()) {
                dateFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ApiContextInitializer.init(); // Инициализируем апи
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            botapi.registerBot(new PedroBot());
            while (true) {
                if (scanDate == null) {
                    scanDate = new Date();
                    parseBsfgMap();
                } else {
                    if ((new Date().getTime()) - scanDate.getTime() > 300000) {
                        scanDate = new Date();
                        parseBsfgMap();
                    }
                }

            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "SuperPedroBot";
        //возвращаем юзера
    }

    public static void parseBsfgMap() throws IOException {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        BufferedReader in = null;
        try {
            String line = "";
            String mapHTML = "";
            URL myUrl = new URL("https://www.bsfg.ru/map.php?server=7#down");
            HttpURLConnection httpcon = (HttpURLConnection) myUrl.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            httpcon.connect();
            in = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "utf-8"));
            String[] territories = null;
            while ((line = in.readLine()) != null) {
                if (line.contains("<img style=")) {
                    territories = line.split("<img style=");
                }

                mapHTML += line;
            }

            List<TerritoryResist> territoryResists = new ArrayList<TerritoryResist>();
            String jarLocation = PedroBot.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            jarLocation = jarLocation.replace("/TelegramBot.jar", "");
            FileInputStream fstream = new FileInputStream(jarLocation + "/resists.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            Map<String, Integer> suspects = new HashMap<String, Integer>();
            while ((strLine = br.readLine()) != null) {
                String[] aimData = strLine.split(", ");
                TerritoryResist territoryResist = new TerritoryResist();
                territoryResist.setTerritory(aimData[0]);
                territoryResist.setRace(aimData[1]);
                if (!aimData[2].equals("Неизвестно")) {
                    territoryResist.setCaptureDate(dateTimeFormat.parse(aimData[2]));
                }
                if (!aimData[3].equals("Неизвестно")) {
                    territoryResist.setCaptureDate(dateTimeFormat.parse(aimData[3]));
                }
                territoryResists.add(territoryResist);
            }
            br.close();

            if (territories != null) {
                for (int i = 0; i < territories.length; i++) {
                    String race = StringUtils.substringBetween(territories[i], "show_tooltip('<b>", "</b>");
                    String territory = StringUtils.substringBetween(territories[i], "<br /><br />", "', event, this)");
                    if (race != null) {
                        race = race.trim();
                    }
                    if (territory != null) {
                        territory.trim();
                    } else {
                        continue;
                    }

                    boolean territoryExist = false;
                    if (territoryResists != null || !territoryResists.isEmpty()) {
                        for (TerritoryResist resist : territoryResists) {
                            if (territory.trim().equals(resist.getTerritory())) {
                                if (!race.equals(resist.getRace())) {
                                    Date now = new Date();
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(now);
                                    calendar.add(Calendar.HOUR_OF_DAY, 3);
                                    resist.setCaptureDate(now);
                                    resist.setResistEnds(calendar.getTime());
                                    resist.setRace(race);
                                    territoryExist = true;
                                    break;
                                } else {
                                    territoryExist = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        territoryResists = new ArrayList<TerritoryResist>();
                        TerritoryResist newRessist = new TerritoryResist();
                        newRessist.setRace(race);
                        newRessist.setTerritory(territory);
                        territoryResists.add(newRessist);
                    }

                    if (!territoryExist) {
                        TerritoryResist newRessist = new TerritoryResist();
                        newRessist.setRace(race);
                        newRessist.setTerritory(territory);
                        territoryResists.add(newRessist);
                    }
                }
            }

            String newResists = "";
            for (TerritoryResist resist : territoryResists) {
                String captureDate = "";
                String resistEnds = "";
                if (resist.getCaptureDate() != null) {
                    captureDate = dateTimeFormat.format(resist.getCaptureDate());
                } else {
                    captureDate = "Неизвестно";
                }

                if (resist.getResistEnds() != null) {
                    resistEnds = dateTimeFormat.format(resist.getResistEnds());
                } else {
                    resistEnds = "Неизвестно";
                }
                newResists = newResists + resist.getTerritory() + ", " + resist.getRace() + ", " + captureDate + ", " + resistEnds + "\n";
            }


            File file = new File(jarLocation + "/resists.txt");
            FileWriter writer = new FileWriter(file);
            BufferedWriter bufferWriter = new BufferedWriter(writer);
            bufferWriter.write(newResists);
            bufferWriter.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }


    }

    private String getRazByNumber(Integer raz) {
        Integer last = raz % 10;
        switch (last) {
            case 0:
                return "раз";
            case 1:
                return "раз";
            case 2:
                return "раза";
            case 3:
                return "раза";
            case 4:
                return "раза";
            case 5:
                return "раз";
            case 6:
                return "раз";
            case 7:
                return "раз";
            case 8:
                return "раз";
            case 9:
                return "раз";
            default:
                return "раз";
        }
    }

    public static String rightPadding(String str, int num) {
        return String.format("%1$-" + num + "s", str);
    }

    @Override
    public void onUpdateReceived(Update e) {
        Message msg = e.getMessage(); // Это нам понадобится
        System.out.println(msg.getChatId());
        if (!msg.getChatId().toString().equals("-1001244634572") && !msg.getChatId().toString().equals("402310012")) {
            return;
        }
        System.out.println(msg.getChatId());
        if (msg != null) {
            try {
                String jarLocation = PedroBot.class.getProtectionDomain()
                        .getCodeSource().getLocation().getPath();
                jarLocation = jarLocation.replace("/TelegramBot.jar", "");
                String txt = msg.getText();
                FileInputStream datefstream = new FileInputStream(jarLocation +"/date.txt");
                BufferedReader datebr = new BufferedReader(new InputStreamReader(datefstream));
                String datestrLine = datebr.readLine();
                String pedor = datebr.readLine();
                datebr.close();
                int random = ThreadLocalRandom.current().nextInt(0, 5);
                if (txt != null) {

                    if ((txt.toLowerCase().contains("пидор") || txt.toLowerCase().contains("педор") || txt.toLowerCase().contains("педик") || txt.toLowerCase().contains("пидарас") || txt.toLowerCase().contains("педрила") || txt.toLowerCase().contains("пидрила")) && !msg.getFrom().getUserName().equals("@SuperPedroBot")) {
                        switch (random) {
                            case 1:
                                sendMsg(msg, "Это тот самый пидор, который " + pedor + "?");
                                break;
                            case 2:
                                sendMsg(msg, pedor + " тут тебя зовут");
                                break;
                            case 3:
                                sendMsg(msg, "Не будите " + pedor);
                                break;
                            case 4:
                                sendMsg(msg, "Не будите " + pedor);
                                break;
                            case 5:
                                sendMsg(msg, "Главный по пидорству у нас " + pedor);
                                break;
                            default:
                                sendMsg(msg, "Кто-то сказал пидор? Сегодня за этим к " + pedor);
                                break;
                        }
                    }

                    if (txt.equals("/resists")) {
                        try {
                            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                            List<TerritoryResist> territoryResists = new ArrayList<TerritoryResist>();
                            FileInputStream fstream = null;

                            fstream = new FileInputStream(jarLocation + "/resists.txt");

                            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                            String strLine;
                            Map<String, Integer> suspects = new HashMap<String, Integer>();
                            while ((strLine = br.readLine()) != null) {
                                String[] aimData = strLine.split(", ");
                                TerritoryResist territoryResist = new TerritoryResist();
                                territoryResist.setTerritory(aimData[0]);
                                territoryResist.setRace(aimData[1]);
                                if (!aimData[2].equals("Неизвестно")) {
                                    territoryResist.setCaptureDate(dateTimeFormat.parse(aimData[2]));
                                }
                                if (!aimData[3].equals("Неизвестно")) {
                                    territoryResist.setCaptureDate(dateTimeFormat.parse(aimData[3]));
                                }
                                territoryResists.add(territoryResist);
                            }
                            br.close();

                            String messageString = "```";
                            for (TerritoryResist territoryResist : territoryResists) {
                                String captureDate = "";
                                String resistEnds = "";
                                if (territoryResist.getCaptureDate() != null) {
                                    captureDate = dateTimeFormat.format(territoryResist.getCaptureDate());
                                } else {
                                    captureDate = "Неизвестно";
                                }
                                if (territoryResist.getResistEnds() != null) {
                                    resistEnds = dateTimeFormat.format(territoryResist.getResistEnds());
                                } else {
                                    resistEnds = "Неизвестно";
                                }
                                messageString =
                                        messageString + territoryResist.getTerritory() + "; " + territoryResist.getRace() + "; " + captureDate + "; " + resistEnds + ";\n";
                                messageString = messageString + "___________________________________________________\n";
                            }
                            messageString = messageString + "```";
                            sendMsg(msg, messageString);
                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (txt.equals("/myRewards")) {
                        try {
                            User user = msg.getFrom();
                            FileInputStream fstream = new FileInputStream(jarLocation + "/pedrolist.txt");
                            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                            String strLine;
                            Map<String, Integer> suspects = new HashMap<String, Integer>();
                            while ((strLine = br.readLine()) != null) {
                                String[] aimData = strLine.split(": ");
                                if (user.getUserName() != null) {
                                    if (("@" + user.getUserName()).equals(aimData[0])) {
                                        sendMsg(msg, aimData[0] + " был архипидором " + aimData[1] + " " + getRazByNumber(Integer.parseInt(aimData[1])) + "\n");
                                        return;
                                    }
                                } else {
                                    if (("@" + user.getFirstName()).equals(aimData[0])) {
                                        sendMsg(msg, aimData[0] + " был архипидором " + aimData[1] + " " + getRazByNumber(Integer.parseInt(aimData[1])) + "\n");
                                        return;
                                    }
                                }
                                suspects.put(aimData[0], Integer.parseInt(aimData[1]));
                            }
                            br.close();
                            if (user.getUserName() != null) {
                                sendMsg(msg, "@" + user.getUserName() + " был архипидором 0 раз\n");
                                return;
                            } else {
                                sendMsg(msg, "@" + user.getFirstName() + " был архипидором 0 раз\n");
                                return;
                            }
                        } catch (Exception ex) {
                            sendMsg(msg, "В доступе отказано!");
                            ex.printStackTrace();
                        }
                    }
                    if (txt.equals("/help")) {
                        sendMsg(msg, "/allSuspects - список всех подозреваемых на должность пидора дня\n/pedroStats - Топ 10 пидоров дня\n/myRewards - узнать, сколько раз ты был архимагом педором\n/testSuspects - определить пидора дня\n/register - зарегестрировать кого-нибудь на участника\n/getPedro - Среди нас педор!\n/start - начало дозора\n/help - помощ для слабых");
                    }
                    if (txt.equals("/allSuspects")) {
                        try {
                            String allSuspects = "";
                            FileInputStream fstream = new FileInputStream(jarLocation + "/pedrolist.txt");
                            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                            String strLine;
                            Map<String, Integer> suspects = new HashMap<String, Integer>();
                            while ((strLine = br.readLine()) != null) {
                                String[] aimData = strLine.split(": ");
                                suspects.put(aimData[0], Integer.parseInt(aimData[1]));
                            }
                            br.close();
                            allSuspects = "Список конкурсантов: ";
                            for (Map.Entry<String, Integer> entry : suspects.entrySet()) {
                                allSuspects = allSuspects + entry.getKey() + "; ";
                            }
                            sendMsg(msg, allSuspects);
                        } catch (Exception ex) {
                            sendMsg(msg, "В доступе отказано!");
                            ex.printStackTrace();
                        }
                    }
                    if (txt.equals("/pedroStats")) {
                        try {
                            String statsString = "";
                            FileInputStream fstream = new FileInputStream(jarLocation + "/pedrolist.txt");
                            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                            String strLine;
                            Map<String, Integer> suspects = new HashMap<String, Integer>();
                            while ((strLine = br.readLine()) != null) {
                                String[] aimData = strLine.split(": ");
                                if (Integer.parseInt(aimData[1]) > 0) {
                                    suspects.put(aimData[0], Integer.parseInt(aimData[1]));
                                }
                            }
                            br.close();
                            suspects = sortByValue(suspects);
                            statsString = "Итак, среди всех педоров, 10 самых педоров архимагов были:\n";
                            int i = 0;
                            for (Map.Entry<String, Integer> entry : suspects.entrySet()) {
                                statsString = statsString + entry.getKey() + ": " + entry.getValue() + " " + getRazByNumber(entry.getValue()) + "\n";
                                i++;
                                if (i == 10) {
                                    break;
                                }
                            }
                            sendMsg(msg, statsString);
                        } catch (Exception ex) {
                            sendMsg(msg, "В доступе отказано!");
                            ex.printStackTrace();
                        }
                    }
                    if (txt.equals("/testSuspects")) {
                        try {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
                            datebr.close();
                            Date currentDate = new Date();
                            String dateOfTest = dateFormat.format(currentDate);
                            if (datestrLine != null && datestrLine.equals(dateOfTest)) {
                                sendMsg(msg, "Пшел вон! Сегодняшний педор архимаг уже выдан " + pedor + "!");
                                return;
                            }

                            FileInputStream fstream = new FileInputStream(jarLocation + "/pedrolist.txt");
                            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                            String strLine;
                            Map<String, Integer> suspects = new HashMap<String, Integer>();
                            int pedroCount = 0;
                            while ((strLine = br.readLine()) != null) {
                                String[] aimData = strLine.split(": ");
                                suspects.put(aimData[0], Integer.parseInt(aimData[1]));
                                pedroCount++;
                            }
                            br.close();

                            int randomNum = ThreadLocalRandom.current().nextInt(0, pedroCount);
                            List<String> suspectKeyList = new ArrayList<String>(suspects.keySet());
                            String suspectKey = suspectKeyList.get(randomNum);

                            sendMsg(msg, "На этот раз титул педора архимага зарабатывает " + suspectKey);
                            File dateFile = new File(jarLocation + "/date.txt");
                            FileWriter dateWriter = new FileWriter(dateFile);
                            BufferedWriter dateBufferWriter = new BufferedWriter(dateWriter);
                            dateBufferWriter.write(dateOfTest + "\n");
                            dateBufferWriter.write(suspectKey);
                            dateBufferWriter.close();

                            File file = new File(jarLocation + "/pedrolist.txt");
                            FileWriter writer = new FileWriter(file);
                            BufferedWriter bufferWriter = new BufferedWriter(writer);
                            for (Map.Entry<String, Integer> entry : suspects.entrySet()) {
                                if (entry.getKey().equals(suspectKey)) {
                                    Integer entryValue = entry.getValue() + 1;
                                    bufferWriter.write(entry.getKey() + ": " + entryValue + "\n");
                                } else {
                                    bufferWriter.write(entry.getKey() + ": " + entry.getValue() + "\n");
                                }
                            }
                            bufferWriter.close();
                        } catch (Exception ex) {
                            sendMsg(msg, "Невероятно, данные зашкаливают, невозможно определить педора, нужно произвести повторное тестирование!");
                            ex.printStackTrace();
                        }
                    }
                    if (txt.equals("/register")) {
                        User user = null;
                        if (msg.getReplyToMessage() != null) {
                            user = msg.getReplyToMessage().getFrom();
                        } else {
                            sendMsg(msg, "Цель не найдена!");
                        }
                        if (user != null) {
                            try {
                                Boolean aimAlreadyFlag = false;
                                FileInputStream fstream = new FileInputStream(jarLocation + "/pedrolist.txt");
                                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                                String strLine;
                                while ((strLine = br.readLine()) != null) {
                                    String[] aimData = strLine.split(": ");
                                    if (aimData[0].equals("@" + user.getUserName())) {
                                        aimAlreadyFlag = true;
                                    }
                                }
                                br.close();

                                String nameForBot = "@" + user.getUserName();
                                if (!aimAlreadyFlag) {
                                    File file = new File(jarLocation + "/pedrolist.txt");
                                    FileWriter writer = new FileWriter(file, true);
                                    BufferedWriter bufferWriter = new BufferedWriter(writer);
                                    if (user.getUserName() == null) {
                                        nameForBot = "@" + user.getFirstName();
                                        System.out.print(user.getId());
                                    }
                                    bufferWriter.write(nameForBot + ": 0\n");
                                    bufferWriter.close();
//                                writer.write("@" + user.getUserName() + ": 0\n");
//                                writer.flush();
//                                writer.close();
                                    System.out.println(nameForBot);
                                    sendMsg(msg, "Цель захвачена! " + nameForBot);
                                } else {
                                    sendMsg(msg, "Цель уже захвачена!");
                                }
                            } catch (Exception ex) {
                                sendMsg(msg, "Сбой системы захвата!");
                                ex.printStackTrace();
                            }
                        }
                    }
                    if (txt.equals("/getPedro")) {
                        //User user = msg.getReplyToMessage().getFrom();
                        //System.out.println(user.getUserName());
                        switch (random) {
                            case 1:
                                sendMsg(msg, "Решением верховного суда архипидоров, я приговариваю тебя к пидорству!");
                                break;
                            case 2:
                                sendMsg(msg, "Ты на столько пидор, что даже пидор дня позавидует!");
                                break;
                            case 3:
                                sendMsg(msg, "Ты главное не нагибайся в его присутсвии, а то, ХОП, и ты пидор!");
                                break;
                            case 4:
                                sendMsg(msg, "Педор педора видит из педора.");
                                break;
                            default:
                                sendMsg(msg, "Внимание!!! Среди нас педор!!!");
                                break;
                        }
                    }
                    if (txt.equals("/start")) {
                        sendMsg(msg, "Педоровый дозор приступил к работе!");
                    }
                }
            } catch (Exception exep) {
                exep.printStackTrace();
            }
        }
    }

    @Override
    public String getBotToken() {
        return "657775467:AAFyqwverY38rMycXXb3XEmJmA62tqFvB10";
        //Токен бота
    }

    @SuppressWarnings("deprecation") // Означает то, что в новых версиях метод уберут или заменят
    private void sendMsg(Message msg, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(msg.getChatId()); // Боту может писать не один человек, и поэтому чтобы отправить сообщение, грубо говоря нужно узнать куда его отправлять
        s.setText(text);
        s.setParseMode("Markdown");
        try { //Чтобы не крашнулась программа при вылете Exception
            sendMessage(s);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return (Comparator<Map.Entry<K, V>> & Serializable)
                (c1, c2) -> c2.getValue().compareTo(c1.getValue());
    }

    public static class TerritoryResist {
        String territory;
        String race;
        Date captureDate;
        Date resistEnds;

        public TerritoryResist() {
        }

        public String getTerritory() {
            return territory;
        }

        public void setTerritory(String territory) {
            this.territory = territory;
        }

        public String getRace() {
            return race;
        }

        public void setRace(String race) {
            this.race = race;
        }

        public Date getCaptureDate() {
            return captureDate;
        }

        public void setCaptureDate(Date captureDate) {
            this.captureDate = captureDate;
        }

        public Date getResistEnds() {
            return resistEnds;
        }

        public void setResistEnds(Date resistEnds) {
            this.resistEnds = resistEnds;
        }
    }
}


