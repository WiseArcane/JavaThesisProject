package staysync.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TenantAccount {
    private String fullName;
    private final String email;
    private final String username;
    private String password;
    private String contactNumber;
    private final RoomInfo roomInfo;
    private PaymentStatus paymentStatus;
    private boolean paymentAwaitingVerification;
    private final List<PaymentRecord> paymentHistory;
    private final List<NotificationRecord> notifications;
    private String approvedCoOccupantName;
    private CoOccupantRequest coOccupantRequest;

    public TenantAccount(String fullName, String username, String password, String contactNumber, RoomInfo roomInfo) {
        this(fullName, "", username, password, contactNumber, roomInfo);
    }

    public TenantAccount(String fullName, String email, String username, String password, String contactNumber, RoomInfo roomInfo) {
        this(
                fullName,
                email,
                username,
                password,
                contactNumber,
                roomInfo,
                PaymentStatus.PENDING,
                false,
                null,
                null,
                "",
                null,
                true);
    }

    TenantAccount(
            String fullName,
            String email,
            String username,
            String password,
            String contactNumber,
            RoomInfo roomInfo,
            PaymentStatus paymentStatus,
            boolean paymentAwaitingVerification,
            List<PaymentRecord> paymentHistory,
            List<NotificationRecord> notifications,
            String approvedCoOccupantName,
            CoOccupantRequest coOccupantRequest) {
        this(
                fullName,
                email,
                username,
                password,
                contactNumber,
                roomInfo,
                paymentStatus,
                paymentAwaitingVerification,
                paymentHistory,
                notifications,
                approvedCoOccupantName,
                coOccupantRequest,
                false);
    }

    private TenantAccount(
            String fullName,
            String email,
            String username,
            String password,
            String contactNumber,
            RoomInfo roomInfo,
            PaymentStatus paymentStatus,
            boolean paymentAwaitingVerification,
            List<PaymentRecord> paymentHistory,
            List<NotificationRecord> notifications,
            String approvedCoOccupantName,
            CoOccupantRequest coOccupantRequest,
            boolean createInitialPaymentRecord) {
        this.fullName = fullName;
        this.email = email == null ? "" : email.trim();
        this.username = username;
        this.password = password;
        this.contactNumber = contactNumber;
        this.roomInfo = roomInfo;
        this.paymentStatus = paymentStatus == null ? PaymentStatus.PENDING : paymentStatus;
        this.paymentAwaitingVerification = paymentAwaitingVerification;
        this.paymentHistory = paymentHistory == null ? new ArrayList<>() : new ArrayList<>(paymentHistory);
        this.notifications = notifications == null ? new ArrayList<>() : new ArrayList<>(notifications);
        this.approvedCoOccupantName = approvedCoOccupantName == null ? "" : approvedCoOccupantName;
        this.coOccupantRequest = coOccupantRequest;
        if (createInitialPaymentRecord) {
            addPaymentRecord(
                    PaymentStatus.PENDING,
                    roomInfo.isAssignmentComplete()
                            ? "Account created. Payment is waiting to be settled."
                            : "Account created. Waiting for the landlord to assign a room and monthly rent.",
                    "System");
        }
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
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

    public String getApprovedCoOccupantName() {
        return approvedCoOccupantName;
    }

    public CoOccupantRequest getCoOccupantRequest() {
        return coOccupantRequest;
    }

    public boolean isPaymentAwaitingVerification() {
        return paymentAwaitingVerification;
    }

    public boolean hasApprovedCoOccupant() {
        return !approvedCoOccupantName.isBlank();
    }

    public boolean hasPendingCoOccupantRequest() {
        return coOccupantRequest != null && coOccupantRequest.isPending();
    }

    public String getOccupantDisplayName() {
        return hasApprovedCoOccupant()
                ? fullName + " & " + approvedCoOccupantName
                : fullName;
    }

    public boolean passwordMatches(String value) {
        return password.equals(value);
    }

    String getPasswordForPersistence() {
        return password;
    }

    public void updateProfile(String fullName, String contactNumber) {
        this.fullName = fullName;
        this.contactNumber = contactNumber;
    }

    public void updateRoomAssignment(String roomNumber, String roomType, double monthlyRent) {
        roomInfo.setRoomNumber(roomNumber);
        roomInfo.setRoomType(roomType);
        roomInfo.setMonthlyRent(monthlyRent);
    }

    public void updateMonthlyRent(double monthlyRent) {
        roomInfo.setMonthlyRent(monthlyRent);
    }

    public void changePassword(String newPassword) {
        password = newPassword;
    }

    public void updatePaymentStatus(PaymentStatus paymentStatus, String note, String updatedBy) {
        this.paymentStatus = paymentStatus;
        this.paymentAwaitingVerification = false;
        addPaymentRecord(paymentStatus, note, updatedBy);
    }

    public void submitPaymentReceipt(String note, String updatedBy, String receiptImagePath, String receiptFileName) {
        addPaymentRecord(paymentStatus, note, updatedBy, receiptImagePath, receiptFileName);
    }

    public void submitPaymentForVerification(String note, String updatedBy) {
        paymentAwaitingVerification = true;
        addPaymentRecord(paymentStatus, note, updatedBy);
    }

    public void addNotification(NotificationType type, String title, String message, String sentBy) {
        notifications.add(0, new NotificationRecord(type, title, message, sentBy));
    }

    public void submitCoOccupantRequest(String requestedName, String note, String updatedBy) {
        coOccupantRequest = new CoOccupantRequest(requestedName, note, updatedBy);
    }

    public void approveCoOccupantRequest(String note, String updatedBy) {
        if (coOccupantRequest == null) {
            return;
        }

        approvedCoOccupantName = coOccupantRequest.getRequestedName();
        coOccupantRequest.markApproved(note, updatedBy);
    }

    public void rejectCoOccupantRequest(String note, String updatedBy) {
        if (coOccupantRequest == null) {
            return;
        }

        coOccupantRequest.markRejected(note, updatedBy);
    }

    public String getPaymentStatusLabel() {
        return paymentStatus.getLabel();
    }

    public String getDueNotificationMessage() {
        if (!roomInfo.isAssignmentComplete()) {
            return "Your landlord will assign your room and monthly rent before billing starts.";
        }
        if (paymentStatus == PaymentStatus.PAID) {
            return "No due payment right now. Your account is updated.";
        }
        if (paymentAwaitingVerification) {
            return "Payment sent. Waiting for the landlord to verify and update your account.";
        }
        if (paymentStatus == PaymentStatus.LATE) {
            return "Payment is overdue. Please settle it as soon as possible.";
        }
        return "Payment is pending. Please pay on or before day " + roomInfo.getDueDay() + " of the month.";
    }

    private void addPaymentRecord(PaymentStatus status, String note, String updatedBy) {
        addPaymentRecord(status, note, updatedBy, "", "");
    }

    private void addPaymentRecord(
            PaymentStatus status,
            String note,
            String updatedBy,
            String receiptImagePath,
            String receiptFileName) {
        paymentHistory.add(0, new PaymentRecord(status, note, updatedBy, receiptImagePath, receiptFileName));
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
        GENERAL_UPDATE("General update"),
        CO_OCCUPANT_UPDATE("Co-occupant update");

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

    public enum CoOccupantRequestStatus {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String label;

        CoOccupantRequestStatus(String label) {
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

        public boolean hasAssignedRoom() {
            return isAssignedValue(roomNumber);
        }

        public boolean hasAssignedRoomType() {
            return isAssignedValue(roomType);
        }

        public boolean hasAssignedRent() {
            return monthlyRent > 0;
        }

        public boolean isAssignmentComplete() {
            return hasAssignedRoom() && hasAssignedRoomType() && hasAssignedRent();
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

        private boolean isAssignedValue(String value) {
            if (value == null) {
                return false;
            }

            String normalizedValue = value.trim();
            return !normalizedValue.isEmpty()
                    && !"TBD".equalsIgnoreCase(normalizedValue)
                    && !"UNASSIGNED".equalsIgnoreCase(normalizedValue);
        }
    }

    public static final class PaymentRecord {
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        private final LocalDateTime timestamp;
        private final PaymentStatus status;
        private final String note;
        private final String updatedBy;
        private final String receiptImagePath;
        private final String receiptFileName;

        public PaymentRecord(PaymentStatus status, String note, String updatedBy) {
            this(status, note, updatedBy, "", "");
        }

        public PaymentRecord(
                PaymentStatus status,
                String note,
                String updatedBy,
                String receiptImagePath,
                String receiptFileName) {
            this(LocalDateTime.now(), status, note, updatedBy, receiptImagePath, receiptFileName);
        }

        public PaymentRecord(
                LocalDateTime timestamp,
                PaymentStatus status,
                String note,
                String updatedBy,
                String receiptImagePath,
                String receiptFileName) {
            this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
            this.status = status;
            this.note = note;
            this.updatedBy = updatedBy;
            this.receiptImagePath = receiptImagePath == null ? "" : receiptImagePath;
            this.receiptFileName = receiptFileName == null ? "" : receiptFileName;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
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

        public String getReceiptImagePath() {
            return receiptImagePath;
        }

        public String getReceiptFileName() {
            return receiptFileName;
        }

        public boolean hasReceiptImage() {
            return !receiptImagePath.isBlank();
        }

        public String getReceiptStatusLabel() {
            return hasReceiptImage() ? "Photo attached" : "No photo";
        }

        public String getFormattedTimestamp() {
            return timestamp.format(DISPLAY_FORMATTER);
        }
    }

    public static final class CoOccupantRequest {
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        private final String requestedName;
        private final LocalDateTime submittedAt;
        private LocalDateTime updatedAt;
        private CoOccupantRequestStatus status;
        private String note;
        private String updatedBy;

        public CoOccupantRequest(String requestedName, String note, String updatedBy) {
            this(
                    requestedName,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    CoOccupantRequestStatus.PENDING,
                    note,
                    updatedBy);
        }

        public CoOccupantRequest(
                String requestedName,
                LocalDateTime submittedAt,
                LocalDateTime updatedAt,
                CoOccupantRequestStatus status,
                String note,
                String updatedBy) {
            this.requestedName = requestedName == null ? "" : requestedName.trim();
            this.submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt;
            this.updatedAt = updatedAt == null ? this.submittedAt : updatedAt;
            this.status = status == null ? CoOccupantRequestStatus.PENDING : status;
            this.note = note == null ? "" : note;
            this.updatedBy = updatedBy == null ? "" : updatedBy;
        }

        public String getRequestedName() {
            return requestedName;
        }

        public LocalDateTime getSubmittedAt() {
            return submittedAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public CoOccupantRequestStatus getStatus() {
            return status;
        }

        public String getStatusLabel() {
            return status.getLabel();
        }

        public String getNote() {
            return note;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public boolean isPending() {
            return status == CoOccupantRequestStatus.PENDING;
        }

        public String getFormattedSubmittedAt() {
            return submittedAt.format(DISPLAY_FORMATTER);
        }

        public String getFormattedUpdatedAt() {
            return updatedAt.format(DISPLAY_FORMATTER);
        }

        private void markApproved(String note, String updatedBy) {
            update(CoOccupantRequestStatus.APPROVED, note, updatedBy);
        }

        private void markRejected(String note, String updatedBy) {
            update(CoOccupantRequestStatus.REJECTED, note, updatedBy);
        }

        private void update(CoOccupantRequestStatus nextStatus, String note, String updatedBy) {
            status = nextStatus;
            this.note = note == null ? "" : note;
            this.updatedBy = updatedBy == null ? "" : updatedBy;
            updatedAt = LocalDateTime.now();
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
            this(LocalDateTime.now(), type, title, message, sentBy);
        }

        public NotificationRecord(LocalDateTime timestamp, NotificationType type, String title, String message, String sentBy) {
            this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
            this.type = type;
            this.title = title;
            this.message = message;
            this.sentBy = sentBy;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
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
