package staysync.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import staysync.core.TenantAccount.CoOccupantRequest;
import staysync.core.TenantAccount.CoOccupantRequestStatus;
import staysync.core.TenantAccount.NotificationRecord;
import staysync.core.TenantAccount.NotificationType;
import staysync.core.TenantAccount.PaymentRecord;
import staysync.core.TenantAccount.PaymentStatus;
import staysync.core.TenantAccount.RoomInfo;
import staysync.core.TenantAccount.VerificationStatus;
import staysync.core.StaySyncService.LandlordNotification;
import staysync.core.StaySyncService.LandlordNotificationType;

final class TenantAccountJsonStore {
    private static final String VERSION_KEY = "version";
    private static final String TENANTS_KEY = "tenants";
    private static final String LANDLORD_NOTIFICATIONS_KEY = "landlordNotifications";

    private TenantAccountJsonStore() {
    }

    static StoreData load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }

        String json = Files.readString(file, StandardCharsets.UTF_8);
        if (json.trim().isEmpty()) {
            return new StoreData(new ArrayList<>(), new ArrayList<>());
        }

        Object root = new JsonParser(json).parse();
        Map<String, Object> rootObject = requireObject(root, "root");
        List<Object> tenantObjects = requireArray(rootObject.get(TENANTS_KEY), TENANTS_KEY);
        List<Object> landlordNotificationObjects = optionalArray(rootObject.get(LANDLORD_NOTIFICATIONS_KEY), LANDLORD_NOTIFICATIONS_KEY);

        List<TenantAccount> tenants = new ArrayList<>();
        for (Object tenantObject : tenantObjects) {
            tenants.add(readTenant(requireObject(tenantObject, "tenant")));
        }
        return new StoreData(tenants, readLandlordNotifications(landlordNotificationObjects));
    }

    static void save(Path file, List<TenantAccount> tenants, List<LandlordNotification> landlordNotifications) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put(VERSION_KEY, 4);

        List<Object> serializedTenants = new ArrayList<>();
        for (TenantAccount tenant : tenants) {
            serializedTenants.add(writeTenant(tenant));
        }
        root.put(TENANTS_KEY, serializedTenants);
        root.put(LANDLORD_NOTIFICATIONS_KEY, writeLandlordNotifications(landlordNotifications));

        String json = JsonWriter.write(root);
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> writeTenant(TenantAccount tenant) {
        Map<String, Object> tenantObject = new LinkedHashMap<>();
        tenantObject.put("fullName", tenant.getFullName());
        tenantObject.put("email", tenant.getEmail());
        tenantObject.put("username", tenant.getUsername());
        tenantObject.put("password", tenant.getPasswordForPersistence());
        tenantObject.put("contactNumber", tenant.getContactNumber());
        tenantObject.put("paymentStatus", tenant.getPaymentStatus().name());
        tenantObject.put("verificationStatus", tenant.getVerificationStatus().name());
        tenantObject.put("approvedCoOccupantName", tenant.getApprovedCoOccupantName());
        tenantObject.put("roomInfo", writeRoomInfo(tenant.getRoomInfo()));
        tenantObject.put("paymentHistory", writePaymentHistory(tenant.getPaymentHistory()));
        tenantObject.put("notifications", writeNotifications(tenant.getNotifications()));
        tenantObject.put("coOccupantRequest", writeCoOccupantRequest(tenant.getCoOccupantRequest()));
        return tenantObject;
    }

    private static Map<String, Object> writeRoomInfo(RoomInfo roomInfo) {
        Map<String, Object> roomObject = new LinkedHashMap<>();
        roomObject.put("roomNumber", roomInfo.getRoomNumber());
        roomObject.put("roomType", roomInfo.getRoomType());
        roomObject.put("monthlyRent", roomInfo.getMonthlyRent());
        roomObject.put("dueDay", roomInfo.getDueDay());
        return roomObject;
    }

    private static List<Object> writePaymentHistory(List<PaymentRecord> paymentHistory) {
        List<Object> records = new ArrayList<>();
        for (PaymentRecord record : paymentHistory) {
            Map<String, Object> recordObject = new LinkedHashMap<>();
            recordObject.put("timestamp", record.getTimestamp().toString());
            recordObject.put("status", record.getStatus().name());
            recordObject.put("note", record.getNote());
            recordObject.put("updatedBy", record.getUpdatedBy());
            recordObject.put("billingMonth", record.getBillingMonth());
            recordObject.put("amount", record.getAmount());
            recordObject.put("referenceNumber", record.getReferenceNumber());
            recordObject.put("receiptImagePath", record.getReceiptImagePath());
            recordObject.put("receiptFileName", record.getReceiptFileName());
            records.add(recordObject);
        }
        return records;
    }

    private static List<Object> writeNotifications(List<NotificationRecord> notifications) {
        List<Object> serializedNotifications = new ArrayList<>();
        for (NotificationRecord notification : notifications) {
            Map<String, Object> notificationObject = new LinkedHashMap<>();
            notificationObject.put("timestamp", notification.getTimestamp().toString());
            notificationObject.put("type", notification.getType().name());
            notificationObject.put("title", notification.getTitle());
            notificationObject.put("message", notification.getMessage());
            notificationObject.put("sentBy", notification.getSentBy());
            notificationObject.put("read", notification.isRead());
            serializedNotifications.add(notificationObject);
        }
        return serializedNotifications;
    }

    private static List<Object> writeLandlordNotifications(List<LandlordNotification> notifications) {
        List<Object> serializedNotifications = new ArrayList<>();
        if (notifications == null) {
            return serializedNotifications;
        }

        for (LandlordNotification notification : notifications) {
            Map<String, Object> notificationObject = new LinkedHashMap<>();
            notificationObject.put("timestamp", notification.getTimestamp().toString());
            notificationObject.put("type", notification.getType().name());
            notificationObject.put("tenantUsername", notification.getTenantUsername());
            notificationObject.put("tenantName", notification.getTenantName());
            notificationObject.put("title", notification.getTitle());
            notificationObject.put("message", notification.getMessage());
            notificationObject.put("read", notification.isRead());
            serializedNotifications.add(notificationObject);
        }
        return serializedNotifications;
    }

    private static Object writeCoOccupantRequest(CoOccupantRequest request) {
        if (request == null) {
            return null;
        }

        Map<String, Object> requestObject = new LinkedHashMap<>();
        requestObject.put("requestedName", request.getRequestedName());
        requestObject.put("submittedAt", request.getSubmittedAt().toString());
        requestObject.put("updatedAt", request.getUpdatedAt().toString());
        requestObject.put("status", request.getStatus().name());
        requestObject.put("note", request.getNote());
        requestObject.put("updatedBy", request.getUpdatedBy());
        return requestObject;
    }

    private static TenantAccount readTenant(Map<String, Object> tenantObject) throws IOException {
        RoomInfo roomInfo = readRoomInfo(optionalObject(tenantObject.get("roomInfo"), "roomInfo"));
        List<PaymentRecord> paymentHistory = readPaymentHistory(optionalArray(tenantObject.get("paymentHistory"), "paymentHistory"));
        List<NotificationRecord> notifications = readNotifications(optionalArray(tenantObject.get("notifications"), "notifications"));
        CoOccupantRequest coOccupantRequest = readCoOccupantRequest(optionalObject(tenantObject.get("coOccupantRequest"), "coOccupantRequest"));

        return new TenantAccount(
                readString(tenantObject, "fullName", ""),
                readString(tenantObject, "email", ""),
                readString(tenantObject, "username", ""),
                readString(tenantObject, "password", ""),
                readString(tenantObject, "contactNumber", ""),
                roomInfo,
                readEnum(tenantObject, "paymentStatus", PaymentStatus.class, PaymentStatus.PENDING),
                readVerificationStatus(tenantObject),
                paymentHistory,
                notifications,
                readString(tenantObject, "approvedCoOccupantName", ""),
                coOccupantRequest);
    }

    private static RoomInfo readRoomInfo(Map<String, Object> roomObject) throws IOException {
        if (roomObject == null) {
            return new RoomInfo("TBD", "", 0, 5);
        }

        return new RoomInfo(
                readString(roomObject, "roomNumber", "TBD"),
                readString(roomObject, "roomType", ""),
                readDouble(roomObject, "monthlyRent", 0),
                readInt(roomObject, "dueDay", 5));
    }

    private static List<PaymentRecord> readPaymentHistory(List<Object> records) throws IOException {
        List<PaymentRecord> paymentHistory = new ArrayList<>();
        if (records == null) {
            return paymentHistory;
        }

        for (Object recordObject : records) {
            Map<String, Object> recordMap = requireObject(recordObject, "paymentRecord");
            paymentHistory.add(new PaymentRecord(
                    readDateTime(recordMap, "timestamp"),
                    readEnum(recordMap, "status", PaymentStatus.class, PaymentStatus.PENDING),
                    readString(recordMap, "note", ""),
                    readString(recordMap, "updatedBy", ""),
                    readString(recordMap, "billingMonth", ""),
                    readDouble(recordMap, "amount", 0),
                    readString(recordMap, "referenceNumber", ""),
                    readString(recordMap, "receiptImagePath", ""),
                    readString(recordMap, "receiptFileName", "")));
        }
        return paymentHistory;
    }

    private static List<NotificationRecord> readNotifications(List<Object> records) throws IOException {
        List<NotificationRecord> notifications = new ArrayList<>();
        if (records == null) {
            return notifications;
        }

        for (Object recordObject : records) {
            Map<String, Object> recordMap = requireObject(recordObject, "notification");
            notifications.add(new NotificationRecord(
                    readDateTime(recordMap, "timestamp"),
                    readEnum(recordMap, "type", NotificationType.class, NotificationType.GENERAL_UPDATE),
                    readString(recordMap, "title", ""),
                    readString(recordMap, "message", ""),
                    readString(recordMap, "sentBy", ""),
                    readBoolean(recordMap, "read", false)));
        }
        return notifications;
    }

    private static List<LandlordNotification> readLandlordNotifications(List<Object> records) throws IOException {
        List<LandlordNotification> notifications = new ArrayList<>();
        if (records == null) {
            return notifications;
        }

        for (Object recordObject : records) {
            Map<String, Object> recordMap = requireObject(recordObject, "landlordNotification");
            notifications.add(new LandlordNotification(
                    readDateTime(recordMap, "timestamp"),
                    readEnum(recordMap, "type", LandlordNotificationType.class, LandlordNotificationType.NOTICE_OR_CONCERN),
                    readString(recordMap, "tenantUsername", ""),
                    readString(recordMap, "tenantName", ""),
                    readString(recordMap, "title", ""),
                    readString(recordMap, "message", ""),
                    readBoolean(recordMap, "read", false)));
        }
        return notifications;
    }

    static final class StoreData {
        private final List<TenantAccount> tenants;
        private final List<LandlordNotification> landlordNotifications;

        private StoreData(List<TenantAccount> tenants, List<LandlordNotification> landlordNotifications) {
            this.tenants = tenants == null ? new ArrayList<>() : new ArrayList<>(tenants);
            this.landlordNotifications = landlordNotifications == null ? new ArrayList<>() : new ArrayList<>(landlordNotifications);
        }

        List<TenantAccount> getTenants() {
            return tenants;
        }

        List<LandlordNotification> getLandlordNotifications() {
            return landlordNotifications;
        }
    }

    private static CoOccupantRequest readCoOccupantRequest(Map<String, Object> requestObject) throws IOException {
        if (requestObject == null) {
            return null;
        }

        return new CoOccupantRequest(
                readString(requestObject, "requestedName", ""),
                readDateTime(requestObject, "submittedAt"),
                readDateTime(requestObject, "updatedAt"),
                readEnum(requestObject, "status", CoOccupantRequestStatus.class, CoOccupantRequestStatus.PENDING),
                readString(requestObject, "note", ""),
                readString(requestObject, "updatedBy", ""));
    }

    private static Map<String, Object> requireObject(Object value, String key) throws IOException {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        throw new IOException("Expected JSON object for " + key + ".");
    }

    private static Map<String, Object> optionalObject(Object value, String key) throws IOException {
        if (value == null) {
            return null;
        }
        return requireObject(value, key);
    }

    private static List<Object> requireArray(Object value, String key) throws IOException {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        throw new IOException("Expected JSON array for " + key + ".");
    }

    private static List<Object> optionalArray(Object value, String key) throws IOException {
        if (value == null) {
            return null;
        }
        return requireArray(value, key);
    }

    private static String readString(Map<String, Object> object, String key, String defaultValue) {
        Object value = object.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static boolean readBoolean(Map<String, Object> object, String key, boolean defaultValue) {
        Object value = object.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static double readDouble(Map<String, Object> object, String key, double defaultValue) throws IOException {
        Object value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IOException("Expected number for " + key + ".", exception);
        }
    }

    private static int readInt(Map<String, Object> object, String key, int defaultValue) throws IOException {
        Object value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IOException("Expected integer for " + key + ".", exception);
        }
    }

    private static LocalDateTime readDateTime(Map<String, Object> object, String key) throws IOException {
        String value = readString(object, key, "");
        if (value.isBlank()) {
            throw new IOException("Expected date-time value for " + key + ".");
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid date-time value for " + key + ".", exception);
        }
    }

    private static <T extends Enum<T>> T readEnum(
            Map<String, Object> object,
            String key,
            Class<T> enumType,
            T defaultValue) throws IOException {
        String value = readString(object, key, "");
        if (value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid value for " + key + ".", exception);
        }
    }

    private static VerificationStatus readVerificationStatus(Map<String, Object> tenantObject) throws IOException {
        Object explicitValue = tenantObject.get("verificationStatus");
        if (explicitValue != null && !String.valueOf(explicitValue).isBlank()) {
            return readEnum(tenantObject, "verificationStatus", VerificationStatus.class, VerificationStatus.CLEAR);
        }
        return readBoolean(tenantObject, "paymentAwaitingVerification", false)
                ? VerificationStatus.FOR_REVIEW
                : VerificationStatus.CLEAR;
    }

    private static final class JsonWriter {
        private JsonWriter() {
        }

        static String write(Object value) {
            StringBuilder builder = new StringBuilder();
            appendValue(builder, value, 0);
            builder.append(System.lineSeparator());
            return builder.toString();
        }

        private static void appendValue(StringBuilder builder, Object value, int indent) {
            if (value == null) {
                builder.append("null");
                return;
            }
            if (value instanceof String stringValue) {
                appendString(builder, stringValue);
                return;
            }
            if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
                return;
            }
            if (value instanceof Map<?, ?> map) {
                appendObject(builder, map, indent);
                return;
            }
            if (value instanceof List<?> list) {
                appendArray(builder, list, indent);
                return;
            }

            appendString(builder, String.valueOf(value));
        }

        private static void appendObject(StringBuilder builder, Map<?, ?> map, int indent) {
            builder.append("{");
            if (map.isEmpty()) {
                builder.append("}");
                return;
            }

            builder.append(System.lineSeparator());
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(builder, indent + 1);
                appendString(builder, String.valueOf(entry.getKey()));
                builder.append(": ");
                appendValue(builder, entry.getValue(), indent + 1);
                if (index < map.size() - 1) {
                    builder.append(",");
                }
                builder.append(System.lineSeparator());
                index++;
            }
            indent(builder, indent);
            builder.append("}");
        }

        private static void appendArray(StringBuilder builder, List<?> list, int indent) {
            builder.append("[");
            if (list.isEmpty()) {
                builder.append("]");
                return;
            }

            builder.append(System.lineSeparator());
            for (int index = 0; index < list.size(); index++) {
                indent(builder, indent + 1);
                appendValue(builder, list.get(index), indent + 1);
                if (index < list.size() - 1) {
                    builder.append(",");
                }
                builder.append(System.lineSeparator());
            }
            indent(builder, indent);
            builder.append("]");
        }

        private static void appendString(StringBuilder builder, String value) {
            builder.append('"');
            for (int index = 0; index < value.length(); index++) {
                char current = value.charAt(index);
                switch (current) {
                    case '\\' -> builder.append("\\\\");
                    case '"' -> builder.append("\\\"");
                    case '\b' -> builder.append("\\b");
                    case '\f' -> builder.append("\\f");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> {
                        if (current < 0x20) {
                            builder.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
                        } else {
                            builder.append(current);
                        }
                    }
                }
            }
            builder.append('"');
        }

        private static void indent(StringBuilder builder, int level) {
            for (int index = 0; index < level; index++) {
                builder.append("  ");
            }
        }
    }

    private static final class JsonParser {
        private final String source;
        private int index;

        private JsonParser(String source) {
            this.source = source;
        }

        Object parse() throws IOException {
            Object value = parseValue();
            skipWhitespace();
            if (!isAtEnd()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Unexpected end of input");
            }

            char current = source.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected character '" + current + "'");
                }
            };
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            skipWhitespace();

            Map<String, Object> object = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            skipWhitespace();

            List<Object> values = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return values;
            }

            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isAtEnd()) {
                char current = source.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (isAtEnd()) {
                        throw error("Unexpected end of escape sequence");
                    }
                    char escaped = source.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicodeEscape());
                        default -> throw error("Unsupported escape sequence \\" + escaped + "'");
                    }
                    continue;
                }
                builder.append(current);
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() throws IOException {
            if (index + 4 > source.length()) {
                throw error("Incomplete unicode escape");
            }
            String hex = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid unicode escape");
            }
        }

        private Object parseNumber() throws IOException {
            int start = index;
            if (source.charAt(index) == '-') {
                index++;
            }
            readDigits();
            if (!isAtEnd() && source.charAt(index) == '.') {
                index++;
                readDigits();
            }
            if (!isAtEnd() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                index++;
                if (!isAtEnd() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }
                readDigits();
            }

            String token = source.substring(start, index);
            try {
                if (token.contains(".") || token.contains("e") || token.contains("E")) {
                    return Double.parseDouble(token);
                }
                long longValue = Long.parseLong(token);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void readDigits() throws IOException {
            int start = index;
            while (!isAtEnd() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected digit");
            }
        }

        private Object parseLiteral(String literal, Object value) throws IOException {
            if (source.regionMatches(index, literal, 0, literal.length())) {
                index += literal.length();
                return value;
            }
            throw error("Expected '" + literal + "'");
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (isAtEnd() || source.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char value) {
            return !isAtEnd() && source.charAt(index) == value;
        }

        private boolean isAtEnd() {
            return index >= source.length();
        }

        private IOException error(String message) {
            return new IOException(message + " at position " + index + ".");
        }
    }
}
