package com.example.smartsitiesbot.service;

import com.example.smartsitiesbot.config.BotConfig;
import com.example.smartsitiesbot.model.Answer;
import com.example.smartsitiesbot.model.CityStatistics;
import com.example.smartsitiesbot.model.QuizResponse;
import com.example.smartsitiesbot.model.User;
import com.example.smartsitiesbot.repositories.AnswerRepository;
import com.example.smartsitiesbot.repositories.QuizResponseRepository;
import com.example.smartsitiesbot.repositories.UserRepository;
import com.example.smartsitiesbot.model.enums.QuizState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuizResponseRepository quizResponseRepository;
    @Autowired
    private AnswerRepository answerRepository;

    private static final String HELP_TEXT = "Привіт! \uD83D\uDC4B Я - бот для оцінки розвиненості міста. " +
            "Тут декілька корисних команд, які ти можеш використовувати: \n" +
            "\n" +
            "/start - почати оцінювання розвиненості міста.\n" +
            "\n" +
            "/help - вивести цей текст допомоги.\n" +
            "\n" +
            "/statistics - щов вивести статистику\n" +
            "\n" +
            "/myanswer - вивести бал на який Ви відповіли .\n" +
            "\n" +
            "Щоб розпочати оцінку, просто натискай  /quiz  та відповідай на питання. Якщо у тебе виникли питання чи потрібна допомога, не соромся питати!\n" +
            "\n" +
            "Приємного використання! \uD83C\uDFD9\uFE0F";

    private final BotConfig config;
    private final List<String> questions = new ArrayList<>();
    private final ReplyKeyboardMarkup answers = new ReplyKeyboardMarkup();

    private void initializeQuestions() {
        // questions
        questions.add("Моє місто ефективно використовує технології для покращення громадських послуг.");
        questions.add("Транспортний потік у моєму місті є ефективним і добре керованим");
        questions.add("Моє місто надає пріоритети та інвестує в стійкі енергетичні рішення.");
        questions.add("Громадський транспорт у моєму місті доступний, надійний і недорогий.");
        questions.add("Моє місто забезпечує доступ до високошвидкісного Інтернету та цифрової інфраструктури.");
        questions.add("Моє місто використовує дані й аналітику для вирішення міських проблем.");
        questions.add("У моєму місті пріоритетом є зручність пішоходів та велосипедистів");
        questions.add("Громадські місця в моєму місті чисті, безпечні та доглянуті.");
        questions.add("Моє місто пропонує різноманітні та інклюзивні можливості для навчання та розвитку.");
        questions.add("Моє місто виховує сильне почуття спільноти та приналежності серед мешканців.");
        questions.add("Моє місто заохочує інновації та підприємництво.");
        questions.add("Моє місто є стійким і готовим до стихійних лих та інших надзвичайних ситуацій.");
        questions.add("Моє місто ефективно управляє відходами та сприяє переробці та сталому розвитку.");
        questions.add("Моє місто пропонує доступні варіанти житла для людей з різним рівнем доходу.");
        questions.add("Моє місто є лідером у розробці та впровадженні технологій розумного міста.");
        questions.add("Моє місто сприяє культурному розмаїттю та мистецькому вираженню.");
        questions.add("Моє місто активно залучає мешканців до процесів прийняття рішень.");
        questions.add("Моє місто є прозорим і підзвітним у своєму управлінні.");
        questions.add("Моє місто є привабливим місцем для життя, роботи та туризму.");
        questions.add("Чи можу я сказати про своє місто, що воно «розумне»?");
        // answers
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("1");
        row.add("2");
        row.add("3");
        row.add("4");
        row.add("5");
        keyboardRows.add(row);
        answers.setKeyboard(keyboardRows);
    }

    public TelegramBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome massage"));
        listOfCommands.add(new BotCommand("/quiz", "start test"));
        listOfCommands.add(new BotCommand("/stats", "get stats"));
        listOfCommands.add(new BotCommand("/myanswer", "get my answer"));
        listOfCommands.add(new BotCommand("/help", "info how yo use this bot"));

        initializeQuestions();

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error: Settings list " + e.getMessage());
        }

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            var quizResponse = quizResponseRepository.findById(chatId).orElse(null);

            if (quizResponse != null && quizResponse.getQuizState() != QuizState.STOP) {
                processQuizAnswer(chatId, messageText);
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/quiz":
                        startQuiz(chatId);
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_TEXT);
                        break;
                    case "/stats":
                        sendStats(chatId, getStatistics());
                        break;
                    case "/myanswer":
                        sendMyStat(chatId, getMyStat(chatId));
                        break;
                    default:
                        sendMessage(chatId, "На жаль, команду не розпізнано.");
                        break;

                }
            }
        }
    }

    private CityStatistics getMyStat(long chatId) {
        CityStatistics cityStatistics = new CityStatistics();
        QuizResponse quizResponse = quizResponseRepository.findById(chatId).orElse(null);

        assert quizResponse != null;

        int totalScore = quizResponse.getAnswerList().stream()
                .mapToInt(Answer::calculateScore)
                .sum();

        cityStatistics.setCity(quizResponse.getCity());
        cityStatistics.setTotalScore(totalScore);
        cityStatistics.setCount(1);
        cityStatistics.setAverageScore((double) totalScore / questions.size());

        return cityStatistics;
    }

    private void startQuiz(long chatId) {
        var quizResponse = Objects.requireNonNull(
                quizResponseRepository.findById(chatId).orElse(null));

        if (quizResponse.getCountOfQuestion() == questions.size()) {
            sendMessage(chatId, "Вибачте, ви вже склали тест.");
            return;
        }
        changeQuizResponseStatus(chatId, QuizState.AWAITING_CITY);
        sendMessage(chatId, "Розпочинаємо вікторину! В якому місті ти живеш?");
    }

    private void processQuizAnswer(long chatId, String answer) {

        var quizResponse = Objects.requireNonNull(
                quizResponseRepository.findById(chatId).orElse(null));
        int count = quizResponse.getCountOfQuestion();

        if (count == questions.size()) {
            changeQuizResponseStatus(chatId, QuizState.STOP);
            sendHappyMassage(chatId);
            return;
        }
        switch (quizResponse.getQuizState()) {
            case AWAITING_CITY:
                saveCityAnswer(chatId, answer);
                changeQuizResponseStatus(chatId, QuizState.AWAITING_ANSWER);
                askQuizQuestion(chatId, count);
                break;
            case AWAITING_ANSWER:
                saveQuizAnswer(chatId, answer, count);
                askQuizQuestion(chatId, count);
                break;
        }
    }

    private void askQuizQuestion(long chatId, int questionNumber) {
        if (questionNumber < questions.size()) {
            String question = questions.get(questionNumber);
            sendMessage(chatId, "Питання " + (questionNumber + 1) + ": " + question, answers);
        }
        changeCountOfQuestions(chatId, questionNumber+1);
    }

    private List<CityStatistics> getStatistics() {
        List<QuizResponse> quizResponseList = (List<QuizResponse>) quizResponseRepository.findAll();

        Map<String, Integer> cityScores = new HashMap<>();
        Map<String, Integer> cityCounts = new HashMap<>();

        int countOfAnswer = questions.size();

        for (QuizResponse quizResponse : quizResponseList) {
            String city = quizResponse.getCity();
            int totalScore = quizResponse.getAnswerList().stream()
                    .mapToInt(Answer::calculateScore)
                    .sum();

            cityScores.merge(city, totalScore, Integer::sum);
            cityCounts.merge(city, 1, Integer::sum);
        }

        return cityScores.entrySet().stream()
                .map(entry -> {
                    String city = entry.getKey();
                    int totalScore = entry.getValue();
                    int count = cityCounts.get(city);
                    double averageScore = (double) totalScore / (count * countOfAnswer);

                    return new CityStatistics(city, totalScore, count, averageScore);
                })
                .toList();
    }


    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisterAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            QuizResponse quizResponse = new QuizResponse();
            quizResponse.setUser(user);
            quizResponse.setId(chatId);
            quizResponse.setCountOfQuestion(0);
            quizResponse.setQuizState(QuizState.STOP);

            quizResponseRepository.save(quizResponse);

            log.info("User saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer =
                "Привіт! \uD83D\uDC4B Я - бот для оцінки розвиненості міста. " +
                        "З радістю допоможу тобі дізнатися, наскільки твоє місто розвинене!\n" +
                        "\n" +
                        "Давай проведемо короткий тест, щоб оцінити рівень розвитку твого міста. " +
                        "Відповідай на питання, щоб я міг надати тобі цікаву інформацію.\n" +
                        "\n" +
                        "Щоб почати, просто натисни /quiz. Почнемо! \uD83C\uDF06";

        sendMessage(chatId, answer);

        log.info("New user: " + name);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void sendMessage(long chatId, String textToSend, ReplyKeyboardMarkup answers) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setReplyMarkup(answers);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void sendHappyMassage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Вікторину завершено! Дякуємо за участь.");

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);
        message.setReplyMarkup(replyKeyboardRemove);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void sendStats(long chatId, List<CityStatistics> statistics) {
        for (CityStatistics stat : statistics) {
            String answer = stat.getCity() + " : " +
                    "\n    -   Кількість людей : " + stat.getCount() +
                    "\n    -   Загальний бал: " + stat.getTotalScore() +
                    "\n    -   Середній бал: " + stat.getAverageScore();
            sendMessage(chatId, answer);
        }
    }

    private void sendMyStat(long chatId, CityStatistics stat) {
        String answer = "🏙️ Ваша відповідь: \n\n" +
                "Місто: " + stat.getCity() +
                "\n    -   Загальний бал: " + stat.getTotalScore() +
                "\n    -   Середній бал: " + stat.getAverageScore();

        sendMessage(chatId, answer);
    }

    private void changeQuizResponseStatus(long chatId, QuizState quizState) {
        var quizResponse = Objects.requireNonNull(
                quizResponseRepository.findById(chatId).orElse(null));
        quizResponse.setQuizState(quizState);
        quizResponseRepository.save(quizResponse);
    }
    private void changeCountOfQuestions(long chatId, int count) {
        QuizResponse quizResponse = Objects.requireNonNull(quizResponseRepository.findById(chatId).orElse(null));
        quizResponse.setCountOfQuestion(count);
        quizResponseRepository.save(quizResponse);
    }

    private void saveCityAnswer(long chatId, String city) {
        QuizResponse quizResponse = quizResponseRepository.findById(chatId).orElse(null);
        if (quizResponse != null) {
            quizResponse.setCity(city);
            quizResponseRepository.save(quizResponse);
        }
    }

    private void saveQuizAnswer(long chatId, String answer, int questionNumber) {
        Answer answer1 = new Answer();
        answer1.setAnswer(answer);
        answer1.setQuestion(questions.get(questionNumber));
        answer1.setQuizResponse(quizResponseRepository.findById(chatId).orElse(null));
        answerRepository.save(answer1);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
}
