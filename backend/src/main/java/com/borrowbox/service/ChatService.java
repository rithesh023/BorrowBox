package com.borrowbox.service;

import com.borrowbox.dto.ChatIntent;
import com.borrowbox.dto.ChatRequest;
import com.borrowbox.dto.ChatResponse;
import com.borrowbox.dto.ItemResponse;
import com.borrowbox.entity.Category;
import com.borrowbox.repository.CategoryRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Pattern PRICE = Pattern.compile(
            "(?:under|below|less than|up to|upto|max(?:imum)?|within|budget(?: of)?|₹|rs\\.?\\s*)\\s*₹?\\s*(\\d+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DAYS = Pattern.compile("(\\d{1,2})\\s*(?:days?|nights?)", Pattern.CASE_INSENSITIVE);
    private static final int RESULT_LIMIT = 5;

    private static final Map<String, List<String>> CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("Books", List.of("book", "textbook", "novel", "text book", "study material")),
            Map.entry("Electronics", List.of("electronic", "gadget", "phone", "mobile", "tablet", "ipad", "headphone", "earphone", "speaker", "charger", "power bank", "keyboard", "computer")),
            Map.entry("Cameras", List.of("camera", "dslr", "photography")),
            Map.entry("Laptops", List.of("laptop", "notebook", "macbook")),
            Map.entry("Calculators", List.of("calculator", "scientific calc", "casio fx")),
            Map.entry("Sports", List.of("sport", "badminton", "racket", "racquet", "shuttle", "cricket", "football", "soccer", "tennis", "basketball", "volleyball")),
            Map.entry("Project Equipment", List.of("project kit", "tripod", "microphone", "mic", "arduino", "projector")),
            Map.entry("Lab Equipment", List.of("lab", "microscope", "beaker", "laboratory")),
            Map.entry("Travel", List.of("travel", "suitcase", "luggage", "backpack")));

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "i", "me", "my", "we", "you", "u", "your", "can", "could", "would", "please", "pls",
            "do", "does", "did", "is", "are", "was", "were", "be", "am", "to", "for", "of", "in", "on", "at", "with",
            "and", "or", "this", "that", "these", "those", "some", "any", "something", "anything", "item", "items", "product", "products",
            "need", "want", "looking", "look", "find", "search", "show", "suggest", "recommend", "recommendation", "recommendations",
            "borrow", "rent", "rental", "available", "availability", "have", "get", "give", "good", "best", "cheap", "cheapest", "option", "options",
            "under", "below", "less", "than", "up", "upto", "max", "maximum", "within", "budget", "rupees", "rs", "per", "day", "days", "night", "nights",
            "around", "about", "cost", "price", "prices", "much", "many", "how", "what", "which", "where", "when", "why", "who", "campus", "borrowbox");

    private final AIService ai;
    private final ItemService items;
    private final CategoryRepository categories;

    public ChatService(AIService ai, ItemService items, CategoryRepository categories) {
        this.ai = ai;
        this.items = items;
        this.categories = categories;
    }

    public ChatResponse respond(String email, ChatRequest request) {
        String text = request.message().trim();
        List<Category> availableCategories = categories.findAll();
        ChatIntent intent = parse(text, availableCategories);

        if (!"SEARCH_ITEM".equals(intent.intent())) {
            String fallback = faq(text);
            AIService.ChatReply answer = ai.generateChatReply(text, List.of(), fallback);
            return new ChatResponse(answer.message(), List.of(), answer.aiEnhanced(), answer.aiStatus());
        }

        String category = resolveCategory(intent.category(), availableCategories);
        List<String> terms = queryTerms(intent.query());
        List<String> matchTerms = expandSearchTerms(terms);
        Map<Long, ItemResponse> candidates = collectCandidates(category, matchTerms, intent.maxPricePerDay());
        boolean overBudgetAlternatives = false;
        if (candidates.isEmpty() && intent.maxPricePerDay() != null) {
            candidates = collectCandidates(category, matchTerms, null);
            overBudgetAlternatives = !candidates.isEmpty();
        }

        Comparator<ItemResponse> relevance = Comparator
                .comparingInt((ItemResponse item) -> score(item, matchTerms, category)).reversed()
                .thenComparing(ItemResponse::pricePerDay)
                .thenComparing(ItemResponse::name, String.CASE_INSENSITIVE_ORDER);
        List<ItemResponse> matches = candidates.values().stream().sorted(relevance).limit(RESULT_LIMIT).toList();
        if (overBudgetAlternatives) {
            matches = candidates.values().stream().sorted(Comparator.comparing(ItemResponse::pricePerDay)
                    .thenComparing(relevance)).limit(RESULT_LIMIT).toList();
        }

        boolean exactTextMatch = matches.stream().anyMatch(item -> textMatchScore(item, matchTerms) > 0);
        String fallback = searchMessage(intent, category, terms, matches, overBudgetAlternatives, exactTextMatch);
        AIService.ChatReply answer = ai.generateChatReply(text, matches, fallback);
        return new ChatResponse(answer.message(), matches, answer.aiEnhanced(), answer.aiStatus());
    }

    private ChatIntent parse(String text, List<Category> availableCategories) {
        String lower = text.toLowerCase(Locale.ROOT);
        String category = detectCategory(lower, availableCategories);
        BigDecimal maxPrice = null;
        Matcher priceMatcher = PRICE.matcher(text);
        if (priceMatcher.find()) {
            try { maxPrice = new BigDecimal(priceMatcher.group(1)); }
            catch (NumberFormatException ignored) { /* Ignore malformed amounts and still search. */ }
        }
        Integer duration = null;
        Matcher daysMatcher = DAYS.matcher(text);
        if (daysMatcher.find()) {
            int days = Integer.parseInt(daysMatcher.group(1));
            if (days >= 1 && days <= 90) duration = days;
        }

        String query = cleanQuery(text);
        boolean searchCue = containsAny(lower, "suggest", "recommend", "find", "search", "show", "looking for", "need", "want", "available", "under ₹", "under rs", "under ", "below ", "within ", "budget");
        boolean greeting = isGreeting(lower);
        boolean faqQuestion = category == null && (greeting || lower.startsWith("how ") || containsAny(lower,
                "cancel", "cancellation", "deposit", "refund", "list an item", "list my", "add a listing", "create a listing",
                "contact", "phone number", "share details", "pickup", "pick up", "payment", "checkout", "card",
                "return", "give back", "review", "rating", "how does", "how do", "how to", "how can i", "how can we",
                "help", "what can i ask", "what can you do", "assistant", "chatbot", "artificial intelligence"));
        boolean search = category != null || (!faqQuestion && (!query.isBlank() || searchCue || maxPrice != null));
        if (lower.matches(".*\\b(suggest|recommend)\\b.*") && (lower.contains("something") || lower.contains("anything"))) search = true;
        return new ChatIntent(search ? "SEARCH_ITEM" : "FAQ", query, category, maxPrice, duration);
    }

    private boolean isGreeting(String text) {
        return text.matches("(?is)^\\s*(?:hi|hello|hey|howdy|good\\s+(?:morning|afternoon|evening))(?:\\b.*)?$");
    }

    private String detectCategory(String lower, List<Category> availableCategories) {
        String direct = availableCategories.stream().map(Category::getName)
                .filter(name -> containsPhrase(lower, name))
                .max(Comparator.comparingInt(String::length)).orElse(null);
        if (direct != null) return direct;

        for (Map.Entry<String, List<String>> entry : CATEGORY_ALIASES.entrySet()) {
            if (entry.getValue().stream().anyMatch(alias -> containsPhrase(lower, alias))) {
                return availableCategories.stream().map(Category::getName)
                        .filter(name -> name.equalsIgnoreCase(entry.getKey())).findFirst().orElse(null);
            }
        }
        return null;
    }

    private String resolveCategory(String requested, List<Category> availableCategories) {
        if (requested == null || requested.isBlank()) return null;
        return availableCategories.stream().map(Category::getName)
                .filter(name -> name.equalsIgnoreCase(requested)).findFirst().orElse(null);
    }

    private String cleanQuery(String text) {
        String query = text.toLowerCase(Locale.ROOT)
                .replaceAll("(?:under|below|less than|up to|upto|max(?:imum)?|within|budget(?: of)?|₹|rs\\.?\\s*)\\s*₹?\\s*\\d+(?:\\.\\d{1,2})?", " ")
                .replaceAll("\\b\\d{1,2}\\s*(?:days?|nights?)\\b", " ")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
        if (query.isBlank()) return "";
        return Arrays.stream(query.split("\\s+"))
                .filter(word -> !STOP_WORDS.contains(word))
                .filter(word -> word.length() > 1)
                .distinct()
                .reduce((left, right) -> left + " " + right).orElse("");
    }

    private List<String> queryTerms(String query) {
        if (query == null || query.isBlank()) return List.of();
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(word -> word.length() > 1).distinct().toList();
    }

    private List<String> expandSearchTerms(List<String> terms) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String term : terms) {
            expanded.add(term);
            if (term.endsWith("ies") && term.length() > 4) expanded.add(term.substring(0, term.length() - 3) + "y");
            else if (term.endsWith("s") && !term.endsWith("ss") && term.length() > 3) expanded.add(term.substring(0, term.length() - 1));
        }
        return List.copyOf(expanded);
    }

    private Map<Long, ItemResponse> collectCandidates(String category, List<String> terms, BigDecimal maxPrice) {
        Map<Long, ItemResponse> results = new LinkedHashMap<>();
        if (category != null) add(results, items.search(null, category, maxPrice, true, null, null, 0, 100));
        if (terms.isEmpty() && category == null) add(results, items.search(null, null, maxPrice, true, null, null, 0, 100));
        for (String term : terms) {
            add(results, items.search(term, category, maxPrice, true, null, null, 0, 30));
            if (category != null) add(results, items.search(term, null, maxPrice, true, null, null, 0, 30));
        }
        return results;
    }

    private void add(Map<Long, ItemResponse> results, Page<ItemResponse> page) {
        page.getContent().forEach(item -> results.putIfAbsent(item.id(), item));
    }

    private int score(ItemResponse item, List<String> terms, String category) {
        int score = textMatchScore(item, terms);
        if (category != null && item.category().equalsIgnoreCase(category)) score += 3;
        return score;
    }

    private int textMatchScore(ItemResponse item, List<String> terms) {
        String name = item.name().toLowerCase(Locale.ROOT);
        String description = item.description().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (name.contains(term)) score += 5;
            else if (description.contains(term)) score += 2;
        }
        return score;
    }

    private String searchMessage(ChatIntent intent, String category, List<String> terms, List<ItemResponse> matches,
                                 boolean overBudgetAlternatives, boolean exactTextMatch) {
        String itemRequest = terms.isEmpty() ? (category == null ? "available items" : category + " items") : String.join(" ", terms);
        if (matches.isEmpty()) {
            if (intent.maxPricePerDay() != null) {
                return "I couldn't find an available " + itemRequest + " within ₹" + money(intent.maxPricePerDay())
                        + " per day. Try a higher daily budget or remove the budget to see more options.";
            }
            if (!terms.isEmpty()) return "I couldn't find a live listing matching “" + itemRequest
                    + "” right now. Try another item name or browse all available listings.";
            return "There aren't any available listings to suggest right now. Try again later, or ask me how BorrowBox works.";
        }

        StringBuilder message = new StringBuilder();
        if (overBudgetAlternatives) {
            message.append("I couldn't find an available ").append(itemRequest).append(" within ₹")
                    .append(money(intent.maxPricePerDay())).append(" per day. Here are the closest live options; their daily rates are above that budget.");
        } else if (!exactTextMatch && !terms.isEmpty() && category != null) {
            message.append("I couldn't find an exact match for ").append(itemRequest).append(". These available ")
                    .append(category).append(" listings may be useful.");
        } else if (terms.isEmpty()) {
            message.append("Here are a few items currently available to borrow.");
        } else {
            message.append("I found ").append(matches.size()).append(" available listing")
                    .append(matches.size() == 1 ? "" : "s").append(" matching ").append(itemRequest).append(".");
        }
        if (intent.maxPricePerDay() != null && !overBudgetAlternatives)
            message.append(" They are within ₹").append(money(intent.maxPricePerDay())).append(" per day.");
        if (intent.durationDays() != null) {
            message.append(" For ").append(intent.durationDays()).append(" days, rental plus the listed deposit is: ");
            message.append(matches.stream().map(item -> item.name() + " ₹" + estimate(item, intent.durationDays()))
                    .collect(java.util.stream.Collectors.joining("; "))).append(". The owner still needs to confirm your dates.");
        } else {
            message.append(" Choose a listing to see its details and request dates.");
        }
        return message.toString();
    }

    private String estimate(ItemResponse item, int days) {
        BigDecimal total = item.pricePerDay().multiply(BigDecimal.valueOf(days)).add(item.securityDeposit());
        return money(total);
    }

    private String money(BigDecimal amount) { return amount.stripTrailingZeros().toPlainString(); }

    private String faq(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "hello", "hi", "hey", "good morning", "good evening"))
            return "Hi! Tell me an item or category and your daily budget, and I'll look through current BorrowBox listings. You can also ask how renting works.";
        if (containsAny(lower, "cancel", "cancellation"))
            return "Open My Rentals and choose Cancel while the request is pending or accepted and before payment. If the rental is already active, contact the other participant to agree on next steps.";
        if (containsAny(lower, "deposit", "security deposit", "refund"))
            return "Each owner sets a security deposit. Review the deposit and rental total on the listing and request before paying. BorrowBox's checkout is a demo and does not charge a real card.";
        if (containsAny(lower, "list an item", "list my", "add a listing", "create a listing", "owner", "sell", "upload"))
            return "Sign in, choose List an item, add its name, category, condition, daily price, deposit, photos and description, then publish. You can edit or remove your listing from its detail page or My Dashboard.";
        if (containsAny(lower, "contact", "phone number", "share details", "pickup", "pick up", "meet the owner"))
            return "After the owner accepts a request, open My Rentals to see the other participant's contact details and coordinate pickup directly. Confirm the time and place together.";
        if (containsAny(lower, "payment", "pay", "checkout", "card"))
            return "After the owner accepts your request, open My Rentals to complete the demo payment. It records a simulated payment; don't enter real card details.";
        if (containsAny(lower, "return", "give back"))
            return "Open My Rentals and mark the rental complete after the item is returned. The borrower and owner can coordinate the handoff there.";
        if (containsAny(lower, "review", "rating"))
            return "After a rental is completed, open it in My Rentals to leave a rating and review for the other participant/listing.";
        if (containsAny(lower, "borrow", "rent", "how does", "how do", "how to", "work"))
            return "Browse or search available items, open one to select dates, and send a rental request. The owner can accept or reject it. After acceptance, use My Rentals for the demo payment and to coordinate pickup.";
        if (containsAny(lower, "ai", "assistant", "chatbot"))
            return "I can search the live BorrowBox inventory and answer common rental questions. When the backend AI provider is configured, I can also phrase replies more naturally; recommendations always come from actual listings.";
        return "I can help you find available campus items, compare daily prices, estimate a rental total with its deposit, or explain how BorrowBox works. What item are you looking for?";
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) if (text.contains(phrase)) return true;
        return false;
    }

    private boolean containsPhrase(String text, String phrase) {
        String normalizedPhrase = phrase.toLowerCase(Locale.ROOT).trim();
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedPhrase) + "(?![\\p{L}\\p{N}])")
                .matcher(text).find();
    }
}
