package staysync.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TenantAccount {
    private String fullName;
    private final String username;
    private String password;
    private String contactNumber;
    private final RoomInfo roomInfo;
    private PaymentStatus paymentStatus;
    private final List<PaymentRecord> paymentHistory;
    private final List<NotificationRecord> notifications;

    public TenantAccount(String fullName, String username, String password, String contactNumber, RoomInfo roomInfo) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.contactNumber = contactNumber;
        this.roomInfo = roomInfo;
        this.paymentStatus = PaymentStatus.PENDING;
        this.paymentHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
        addPaymentRecord(PaymentStatus.PENDING, "Account created. Payment is waiting to be settled.", "System");
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public RoomInfo getRoomInfo() {
        return roomInfo;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public List<PaymentRecord> getPaymentHistory() {
        return Collections.unmodifiableList(paymentHistory);
    }

    public List<NotificationRecord> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    public boolean passwordMatches(String value) {
        return password.equals(value);
    }

    public void updateProfile(String fullName, String contactNumber, String roomNumber, String roomType, double monthlyRent) {
        this.fullName = fullName;
        this.contactNumber = contactNumber;
        roomInfo.setRoomNumber(roomNumber);
        roomInfo.setRoomType(roomType);
        roomInfo.setMonthlyRent(monthlyRent);
    }

    public void changePassword(String newPassword) {
        password = newPassword;
    }

    public void updatePaymentStatus(PaymentStatus paymentStatus, String note, String updatedBy) {
        this.paymentStatus = paymentStatus;
        addPaymentRecord(paymentStatus, note, updatedBy);
    }

    public void addNotification(NotificationType type, String title, String message, String sentBy) {
        notifications.add(0, new NotificationRecord(type, title, message, sentBy));
    }

    public String getPaymentStatusLabel() {
        return paymentStatus.getLabel();
    }

    public String getDueNotificationMessage() {
        if (paymentStatus == PaymentStatus.PAID) {
            return "No due payment right now. Your account is updated.";
        }
        if (paymentStatus == PaymentStatus.LATE) {
            return "Payment is overdue. Please settle it as soon as possible.";
        }
        return "Payment is pending. Please pay on or before day " + roomInfo.getDueDay() + " of the month.";
    }

    private void addPaymentRecord(PaymentStatus status, String note, String updatedBy) {
        paymentHistory.add(0, new PaymentRecord(status, note, updatedBy));
    }

    public enum PaymentStatus {
        PAID("Paid"),
        PENDING("Pending"),
        LATE("Late");

        private final String label;

        PaymentStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum NotificationType {
        PAYMENT_REMINDER("Payment reminder"),
        MAINTENANCE_NOTICE("Maintenance notice"),
        GENERAL_UPDATE("General update");

        private final String label;

        NotificationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class RoomInfo {
        private String roomNumber;
        private String roomType;
        private double monthlyRent;
        private int dueDay;

        public RoomInfo(String roomNumber, String roomType, double monthlyRent, int dueDay) {
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.monthlyRent = monthlyRent;
            this.dueDay = dueDay;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public String getRoomType() {
            return roomType;
        }

        public double getMonthlyRent() {
            return monthlyRent;
        }

        public int getDueDay() {
            return dueDay;
        }

        private void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        private void setRoomType(String roomType) {
            this.roomType = roomType;
        }

        private void setMonthlyRent(double monthlyRent) {
            this.monthlyRent = monthlyRent;
        }
    }

    public static final class PaymentRecord {
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        private final LocalDateTime timestamp;
        private final PaymentStatus status;
        private final String note;
        private final String updatedBy;

        public PaymentRecord(PaymentStatus status, String note, String updatedBy) {
            this.timestamp = LocalDateTime.now();
            this.status = status;
            this.note = note;
            this.updatedBy = updatedBy;
        }

        public PaymentStatus getStatus() {
            return status;
        }

        public String getNote() {
            return note;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public String getFormattedTimestamp() {
            return timestamp.format(DISPLAY_FORMATTER);
        }
    }

    public static final class NotificationRecord {
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        private final LocalDateTime timestamp;
        private final NotificationType type;
        private final String title;
        private final String message;
        private final String sentBy;

        public NotificationRecord(NotificationType type, String title, String message, String sentBy) {
            this.timestamp = LocalDateTime.now();
            this.type = type;
            this.title = title;
            this.message = message;
            this.sentBy = sentBy;
        }

        public NotificationType getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public String getSentBy() {
            return sentBy;
        }

        public String getFormattedTimestamp() {
            return timestamp.format(DISPLAY_FORMATTER);
        }
    }
}
