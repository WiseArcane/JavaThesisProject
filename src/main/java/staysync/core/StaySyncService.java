package staysync.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import staysync.core.TenantAccount.CoOccupantRequest;
import staysync.core.TenantAccount.NotificationRecord;
import staysync.core.TenantAccount.NotificationType;
import staysync.core.TenantAccount.PaymentRecord;
import staysync.core.TenantAccount.PaymentStatus;
import staysync.core.TenantAccount.RoomInfo;
import staysync.core.TenantAccount.VerificationStatus;

public class StaySyncService {
    private static final String LANDLORD_USERNAME = "wise";
    private static final String LANDLORD_PASSWORD = "1234";
    private static final String[] ROOM_TYPES = { "Standard", "Deluxe", "Family" };
    private static final int DEFAULT_DUE_DAY = 5;
    private static final Path ACCOUNTS_STORAGE_FILE = Path.of("storage", "accounts.json");
    private static final Path RECEIPT_STORAGE_DIRECTORY = Path.of("storage", "receipts");

    private final List<TenantAccount> tenants = new ArrayList<>();
    private final List<LandlordNotification> landlordNotifications = new ArrayList<>();

    public StaySyncService() {
        loadTenants();
    }

    public synchronized String registerTenant(
            String fullName,
            String email,
            String username,
            String password,
            String confirmPassword,
            String contactNumber,
            String roomNumber,
            String roomType) {
        String validationMessage = validateTenantRegistration(
                fullName, email, username, password, confirmPassword, contactNumber, roomNumber, roomType);
        if (validationMessage != null) {
            return validationMessage;
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        if (findTenantByUsername(normalizedUsername) != null) {
            return "That username is already in use.";
        }
        if (!normalizedEmail.isEmpty() && findTenantByEmail(normalizedEmail) != null) {
            return "That email is already in use.";
        }

        RoomInfo roomInfo = new RoomInfo(
                "TBD",
                "",
                0,
                DEFAULT_DUE_DAY);
        tenants.add(new TenantAccount(
                fullName.trim(),
                normalizedEmail,
                normalizedUsername,
                password,
                contactNumber.trim(),
                roomInfo));
        saveTenants();
        return null;
    }

    public synchronized String registerTenant(
            String fullName,
            String username,
            String password,
            String confirmPassword,
            String contactNumber,
            String roomNumber,
            String roomType) {
        return registerTenant(fullName, "", username, password, confirmPassword, contactNumber, roomNumber, roomType);
    }

    public String validateLoginCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return "Enter both username and password.";
        }
        return null;
    }

    public synchronized TenantAccount authenticateTenant(String username, String password) {
        TenantAccount tenant = findTenantByLogin(username);
        if (tenant == null) {
            return null;
        }

        return tenant.passwordMatches(password) ? tenant : null;
    }

    public boolean authenticateLandlord(String username, String password) {
        return LANDLORD_USERNAME.equals(username) && LANDLORD_PASSWORD.equals(password);
    }

    public synchronized void markTenantAsPaid(TenantAccount tenant) {
        submitTenantPaymentForVerification(tenant, "");
    }

    public synchronized void submitTenantPaymentForVerification(TenantAccount tenant) {
        submitTenantPaymentForVerification(tenant, "");
    }

    public synchronized String submitTenantPaymentForVerification(TenantAccount tenant, String referenceNumber) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.getRoomInfo().isAssignmentComplete()) {
            return "Your landlord needs to assign your room and monthly rent before you can submit payment proof.";
        }
        if (storedTenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "This billing cycle is already marked as paid.";
        }
        if (storedTenant.isPaymentAwaitingVerification()) {
            return "Your payment proof is already with the landlord for review.";
        }
        if (findLatestReceiptRecord(storedTenant) == null) {
            return "Upload a receipt photo before submitting payment proof.";
        }
        if (isBlank(referenceNumber)) {
            return "Enter the payment reference number before submitting payment proof.";
        }

        storedTenant.submitPaymentForVerification(
                "Payment proof submitted by the tenant and is ready for landlord review.",
                "Tenant",
                referenceNumber.trim());
        addLandlordNotification(
                LandlordNotificationType.PAYMENT_SUBMITTED,
                storedTenant,
                "Payment proof submitted",
                storedTenant.getFullName() + " submitted payment proof for "
                        + storedTenant.getCurrentBillingMonth()
                        + ". Reference: " + referenceNumber.trim() + ".");
        saveTenants();
        return null;
    }

    public synchronized void updateTenantStatusFromLandlord(TenantAccount tenant, PaymentStatus status) {
        updateTenantStatusFromLandlord(tenant, status, "Payment status updated by the landlord.");
    }

    public synchronized String updateTenantStatusFromLandlord(TenantAccount tenant, PaymentStatus status, String note) {
        if (tenant == null) {
            return "Select a tenant first.";
        }
        if (status == null) {
            return "Choose a billing status.";
        }

        updatePaymentStatus(
                tenant,
                status,
                isBlank(note) ? "Payment status updated by the landlord." : note.trim(),
                "Landlord");
        return null;
    }

    public synchronized String verifyTenantPaymentSubmission(TenantAccount tenant, String note) {
        if (tenant == null) {
            return "Select a tenant first.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.isPaymentAwaitingVerification()) {
            return "This tenant does not have a payment proof waiting for review.";
        }

        storedTenant.updatePaymentStatus(
                PaymentStatus.PAID,
                isBlank(note) ? "Payment verified by the landlord." : note.trim(),
                "Landlord");
        saveTenants();
        return null;
    }

    public synchronized String rejectTenantPaymentSubmission(TenantAccount tenant, String note) {
        if (tenant == null) {
            return "Select a tenant first.";
        }
        if (isBlank(note)) {
            return "Enter a short reason so the tenant knows what to fix.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.isPaymentAwaitingVerification()) {
            return "This tenant does not have a payment proof waiting for review.";
        }

        storedTenant.rejectPaymentSubmission(note.trim(), "Landlord");
        saveTenants();
        return null;
    }

    public synchronized String submitTenantPaymentReceipt(TenantAccount tenant, Path sourceImage) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        String validationMessage = validateReceiptFile(sourceImage);
        if (validationMessage != null) {
            return validationMessage;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.getRoomInfo().isAssignmentComplete()) {
            return "Your landlord needs to assign your room and monthly rent before you can upload payment proof.";
        }
        if (storedTenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "This billing cycle is already marked as paid.";
        }

        try {
            Path storedReceipt = storeReceiptCopy(storedTenant, sourceImage);
            storedTenant.submitPaymentReceipt(
                    "Receipt photo uploaded for the current billing review.",
                    "Tenant",
                    storedReceipt.toString(),
                    sourceImage.getFileName().toString());
            saveTenants();
            return null;
        } catch (IOException exception) {
            return "The receipt photo could not be saved. Please try a different image.";
        }
    }

    public synchronized String updateTenantProfile(
            TenantAccount tenant,
            String fullName,
            String contactNumber) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        String validationMessage = validateTenantProfile(fullName, contactNumber);
        if (validationMessage != null) {
            return validationMessage;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        storedTenant.updateProfile(
                fullName.trim(),
                contactNumber.trim());
        saveTenants();
        return null;
    }

    public synchronized String updateTenantRoomAssignment(
            TenantAccount tenant,
            String roomNumber,
            String roomType,
            double monthlyRent) {
        if (tenant == null) {
            return "Select a tenant first.";
        }

        String validationMessage = validateTenantRoomAssignment(roomNumber, roomType, monthlyRent);
        if (validationMessage != null) {
            return validationMessage;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        storedTenant.updateRoomAssignment(roomNumber.trim(), roomType, monthlyRent);
        saveTenants();
        return null;
    }

    public synchronized String updateTenantMonthlyRent(TenantAccount tenant, double monthlyRent) {
        if (tenant == null) {
            return "Select a tenant first.";
        }
        if (monthlyRent <= 0) {
            return "Monthly rent must be greater than 0.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.getRoomInfo().hasAssignedRoom()) {
            return "Assign a room before setting the monthly rent.";
        }
        if (!storedTenant.getRoomInfo().hasAssignedRoomType()) {
            return "Assign a room type before setting the monthly rent.";
        }

        storedTenant.updateMonthlyRent(monthlyRent);
        saveTenants();
        return null;
    }

    public synchronized String changeTenantPassword(
            TenantAccount tenant,
            String currentPassword,
            String newPassword,
            String confirmPassword) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        String validationMessage = validatePasswordChange(currentPassword, newPassword, confirmPassword);
        if (validationMessage != null) {
            return validationMessage;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.passwordMatches(currentPassword)) {
            return "Current password is incorrect.";
        }

        storedTenant.changePassword(newPassword);
        saveTenants();
        return null;
    }

    public synchronized String resetTenantPassword(
            String loginValue,
            String contactNumber,
            String newPassword,
            String confirmPassword) {
        if (isBlank(loginValue) || isBlank(contactNumber) || isBlank(newPassword) || isBlank(confirmPassword)) {
            return "All reset fields are required.";
        }

        TenantAccount storedTenant = findTenantByLogin(loginValue);
        if (storedTenant == null) {
            return "No tenant account matches that username or email.";
        }

        if (!storedTenant.getContactNumber().trim().equals(contactNumber.trim())) {
            return "Contact number does not match this account.";
        }

        String validationMessage = validatePasswordReset(storedTenant, newPassword, confirmPassword);
        if (validationMessage != null) {
            return validationMessage;
        }

        storedTenant.changePassword(newPassword);
        saveTenants();
        return null;
    }

    public synchronized String submitCoOccupantRequest(TenantAccount tenant, String requestedName) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }
        if (!storedTenant.getRoomInfo().hasAssignedRoom()) {
            return "Your landlord needs to assign your room before you can request a co-occupant.";
        }

        String validationMessage = validateCoOccupantName(storedTenant, requestedName);
        if (validationMessage != null) {
            return validationMessage;
        }

        String normalizedRequestedName = requestedName.trim();
        if (storedTenant.hasApprovedCoOccupant()
                && storedTenant.getApprovedCoOccupantName().equalsIgnoreCase(normalizedRequestedName)) {
            return normalizedRequestedName + " is already approved for this room.";
        }

        CoOccupantRequest existingRequest = storedTenant.getCoOccupantRequest();
        if (existingRequest != null
                && existingRequest.isPending()
                && existingRequest.getRequestedName().equalsIgnoreCase(normalizedRequestedName)) {
            return "That co-occupant request is already waiting for landlord approval.";
        }

        storedTenant.submitCoOccupantRequest(
                normalizedRequestedName,
                "Requested approval for " + normalizedRequestedName + " to share the room.",
                "Tenant");
        storedTenant.addNotification(
                NotificationType.CO_OCCUPANT_UPDATE,
                "Co-occupant request sent",
                "Your request for " + normalizedRequestedName + " was sent to the landlord for approval.",
                "System");
        saveTenants();
        return null;
    }

    public synchronized String submitTenantConcern(TenantAccount tenant, String title, String message) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }
        if (isBlank(title) || isBlank(message)) {
            return "Enter both a subject and message for your notice or concern.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        String normalizedTitle = title.trim();
        String normalizedMessage = message.trim();
        addLandlordNotification(
                LandlordNotificationType.NOTICE_OR_CONCERN,
                storedTenant,
                normalizedTitle,
                normalizedMessage);
        storedTenant.addNotification(
                NotificationType.GENERAL_UPDATE,
                "Notice or concern sent",
                "Your notice or concern was sent to the landlord. They can review it from their notification bell.",
                "System");
        saveTenants();
        return null;
    }

    public synchronized String approveCoOccupantRequest(TenantAccount tenant) {
        if (tenant == null) {
            return "Select a tenant first.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        CoOccupantRequest request = storedTenant.getCoOccupantRequest();
        if (request == null || !request.isPending()) {
            return "There is no pending co-occupant request to approve.";
        }

        storedTenant.approveCoOccupantRequest(
                "Co-occupant request approved for " + request.getRequestedName() + ".",
                "Landlord");
        storedTenant.addNotification(
                NotificationType.CO_OCCUPANT_UPDATE,
                "Co-occupant approved",
                request.getRequestedName() + " was approved as a co-occupant for room "
                        + storedTenant.getRoomInfo().getRoomNumber() + ".",
                "Landlord");
        saveTenants();
        return null;
    }

    public synchronized String rejectCoOccupantRequest(TenantAccount tenant) {
        if (tenant == null) {
            return "Select a tenant first.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        CoOccupantRequest request = storedTenant.getCoOccupantRequest();
        if (request == null || !request.isPending()) {
            return "There is no pending co-occupant request to reject.";
        }

        String requestedName = request.getRequestedName();
        storedTenant.rejectCoOccupantRequest(
                "Co-occupant request rejected for " + requestedName + ".",
                "Landlord");
        storedTenant.addNotification(
                NotificationType.CO_OCCUPANT_UPDATE,
                "Co-occupant request rejected",
                "Your request for " + requestedName + " was reviewed and not approved.",
                "Landlord");
        saveTenants();
        return null;
    }

    public synchronized String deleteTenant(TenantAccount tenant) {
        if (tenant == null) {
            return "Select a tenant first.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        tenants.remove(storedTenant);
        saveTenants();
        return null;
    }

    public synchronized String sendNotificationToTenant(
            TenantAccount tenant,
            NotificationType type,
            String title,
            String message) {
        if (tenant == null) {
            return "Select a tenant first.";
        }
        if (type == null) {
            return "Choose a notification type.";
        }
        if (isBlank(title) || isBlank(message)) {
            return "Enter both a notification title and message.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        storedTenant.addNotification(type, title.trim(), message.trim(), "Landlord");
        saveTenants();
        return null;
    }

    public synchronized String updateTenantNotificationReadState(
            TenantAccount tenant,
            NotificationRecord notification,
            boolean read) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }
        if (notification == null) {
            return "Notification record was not found.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        storedTenant.updateNotificationReadState(notification, read);
        saveTenants();
        return null;
    }

    public synchronized String markAllTenantNotificationsRead(TenantAccount tenant) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        storedTenant.markAllNotificationsRead();
        saveTenants();
        return null;
    }

    public synchronized DashboardSnapshot getLandlordDashboardData(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        List<TenantAccount> matchingTenants = searchTenants(normalizedQuery);

        return new DashboardSnapshot(
                matchingTenants,
                tenants.size(),
                countByStatus(PaymentStatus.PAID),
                countByStatus(PaymentStatus.PENDING),
                countByStatus(PaymentStatus.LATE),
                normalizedQuery);
    }

    public synchronized List<LandlordNotification> getLandlordNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(landlordNotifications));
    }

    public synchronized int getUnreadLandlordNotificationCount() {
        int unreadCount = 0;
        for (LandlordNotification notification : landlordNotifications) {
            if (notification != null && notification.isUnread()) {
                unreadCount++;
            }
        }
        return unreadCount;
    }

    public synchronized String updateLandlordNotificationReadState(LandlordNotification notification, boolean read) {
        if (notification == null) {
            return "Notification record was not found.";
        }

        for (LandlordNotification existingNotification : landlordNotifications) {
            if (existingNotification != null && existingNotification.matches(notification)) {
                existingNotification.setRead(read);
                saveTenants();
                return null;
            }
        }
        return "Notification record was not found.";
    }

    public synchronized String markAllLandlordNotificationsRead() {
        for (LandlordNotification notification : landlordNotifications) {
            if (notification != null) {
                notification.markRead();
            }
        }
        saveTenants();
        return null;
    }

    public synchronized int getTenantCount() {
        return tenants.size();
    }

    public static String getLandlordUsername() {
        return LANDLORD_USERNAME;
    }

    public static String getLandlordPassword() {
        return LANDLORD_PASSWORD;
    }

    public static String[] getRoomTypes() {
        return ROOM_TYPES.clone();
    }

    private String validateTenantRegistration(
            String fullName,
            String email,
            String username,
            String password,
            String confirmPassword,
            String contactNumber,
            String roomNumber,
            String roomType) {
        if (isBlank(fullName)
                || isBlank(username)
                || isBlank(password)
                || isBlank(confirmPassword)
                || isBlank(contactNumber)) {
            return "All fields are required.";
        }

        if (fullName.trim().length() < 3) {
            return "Full name must contain at least 3 characters.";
        }

        if (!isBlank(email) && !email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Enter a valid email address.";
        }

        if (!username.trim().matches("[A-Za-z0-9._-]{4,20}")) {
            return "Username must be 4 to 20 characters and may contain letters, numbers, dot, underscore, or dash.";
        }

        if (password.length() < 4) {
            return "Password must contain at least 4 characters.";
        }

        if (!password.equals(confirmPassword)) {
            return "Password and confirm password do not match.";
        }

        if (!contactNumber.trim().matches("[0-9+\\- ]{7,15}")) {
            return "Contact number must be 7 to 15 characters and may include digits, spaces, plus, or dash.";
        }

        return null;
    }

    private String validateTenantProfile(String fullName, String contactNumber) {
        if (isBlank(fullName) || isBlank(contactNumber)) {
            return "All profile fields are required.";
        }

        if (fullName.trim().length() < 3) {
            return "Full name must contain at least 3 characters.";
        }

        if (!contactNumber.trim().matches("[0-9+\\- ]{7,15}")) {
            return "Contact number must be 7 to 15 characters and may include digits, spaces, plus, or dash.";
        }

        return null;
    }

    private String validateTenantRoomAssignment(String roomNumber, String roomType, double monthlyRent) {
        if (isBlank(roomNumber) || isBlank(roomType)) {
            return "Room number and room type are required.";
        }

        if (!roomNumber.trim().matches("[A-Za-z0-9-]{1,12}")) {
            return "Room number must be 1 to 12 characters and may include letters, numbers, or dash.";
        }

        if (!isRecognizedRoomType(roomType)) {
            return "Choose a valid room type.";
        }

        if (monthlyRent <= 0) {
            return "Monthly rent must be greater than 0.";
        }

        return null;
    }

    private String validateCoOccupantName(TenantAccount tenant, String requestedName) {
        if (isBlank(requestedName)) {
            return "Enter the co-occupant's full name.";
        }

        String normalizedName = requestedName.trim();
        if (normalizedName.length() < 3) {
            return "Co-occupant name must contain at least 3 characters.";
        }
        if (normalizedName.length() > 60) {
            return "Co-occupant name must be 60 characters or fewer.";
        }
        if (!normalizedName.matches("[A-Za-z][A-Za-z .'-]{1,59}")) {
            return "Co-occupant name may include letters, spaces, apostrophe, dot, or dash.";
        }
        if (tenant != null && tenant.getFullName().equalsIgnoreCase(normalizedName)) {
            return "The co-occupant name must be different from the tenant name.";
        }
        return null;
    }

    private String validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            return "All password fields are required.";
        }

        if (newPassword.length() < 4) {
            return "New password must contain at least 4 characters.";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "New password and confirm password do not match.";
        }

        if (currentPassword.equals(newPassword)) {
            return "New password must be different from the current password.";
        }

        return null;
    }

    private String validatePasswordReset(TenantAccount tenant, String newPassword, String confirmPassword) {
        if (newPassword.length() < 4) {
            return "New password must contain at least 4 characters.";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "New password and confirm password do not match.";
        }

        if (tenant != null && tenant.passwordMatches(newPassword)) {
            return "New password must be different from the current password.";
        }

        return null;
    }

    private synchronized List<TenantAccount> searchTenants(String query) {
        if (query.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<>(tenants));
        }

        String searchValue = query.toLowerCase();
        List<TenantAccount> matches = new ArrayList<>();
        for (TenantAccount tenant : tenants) {
            if (tenant.getFullName().toLowerCase().contains(searchValue)
                    || tenant.getOccupantDisplayName().toLowerCase().contains(searchValue)
                    || tenant.getApprovedCoOccupantName().toLowerCase().contains(searchValue)
                    || tenant.getRoomInfo().getRoomNumber().toLowerCase().contains(searchValue)) {
                matches.add(tenant);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    private void updatePaymentStatus(TenantAccount tenant, PaymentStatus status, String note, String updatedBy) {
        if (tenant == null) {
            return;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant != null) {
            storedTenant.updatePaymentStatus(status, note, updatedBy);
            saveTenants();
        }
    }

    private PaymentRecord findLatestReceiptRecord(TenantAccount tenant) {
        if (tenant == null) {
            return null;
        }
        for (PaymentRecord record : tenant.getPaymentHistory()) {
            if (record.hasReceiptImage()) {
                return record;
            }
        }
        return null;
    }

    private TenantAccount findTenantByLogin(String loginValue) {
        if (loginValue == null) {
            return null;
        }

        String normalizedLogin = loginValue.trim();
        for (TenantAccount tenant : tenants) {
            if (tenant.getUsername().equalsIgnoreCase(normalizedLogin)
                    || (!tenant.getEmail().isBlank() && tenant.getEmail().equalsIgnoreCase(normalizedLogin))) {
                return tenant;
            }
        }
        return null;
    }

    private TenantAccount findTenantByUsername(String username) {
        if (username == null) {
            return null;
        }

        String normalizedUsername = username.trim();
        for (TenantAccount tenant : tenants) {
            if (tenant.getUsername().equalsIgnoreCase(normalizedUsername)) {
                return tenant;
            }
        }
        return null;
    }

    private TenantAccount findTenantByEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalizedEmail = email.trim();
        for (TenantAccount tenant : tenants) {
            if (!tenant.getEmail().isBlank() && tenant.getEmail().equalsIgnoreCase(normalizedEmail)) {
                return tenant;
            }
        }
        return null;
    }

    private int countByStatus(PaymentStatus status) {
        int count = 0;
        for (TenantAccount tenant : tenants) {
            if (tenant.getPaymentStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private double getMonthlyRentForRoomType(String roomType) {
        if ("Deluxe".equalsIgnoreCase(roomType)) {
            return 4500.00;
        }
        if ("Family".equalsIgnoreCase(roomType)) {
            return 6000.00;
        }
        return 3200.00;
    }

    private String validateReceiptFile(Path sourceImage) {
        if (sourceImage == null) {
            return "Choose a receipt image first.";
        }
        if (!Files.exists(sourceImage) || !Files.isRegularFile(sourceImage)) {
            return "The selected receipt image could not be found.";
        }

        String fileName = sourceImage.getFileName() == null ? "" : sourceImage.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!(fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".gif")
                || fileName.endsWith(".bmp"))) {
            return "Choose a PNG, JPG, JPEG, GIF, or BMP receipt image.";
        }
        return null;
    }

    private Path storeReceiptCopy(TenantAccount tenant, Path sourceImage) throws IOException {
        Path tenantDirectory = RECEIPT_STORAGE_DIRECTORY.resolve(tenant.getUsername());
        Files.createDirectories(tenantDirectory);

        String originalName = sourceImage.getFileName() == null ? "receipt.png" : sourceImage.getFileName().toString();
        String extension = "";
        int extensionIndex = originalName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalName.substring(extensionIndex);
        }

        Path target = tenantDirectory.resolve("receipt-" + System.currentTimeMillis() + extension);
        Files.copy(sourceImage, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().normalize();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isRecognizedRoomType(String roomType) {
        if (isBlank(roomType)) {
            return false;
        }

        for (String supportedRoomType : ROOM_TYPES) {
            if (supportedRoomType.equalsIgnoreCase(roomType.trim())) {
                return true;
            }
        }
        return false;
    }

    private void loadTenants() {
        tenants.clear();
        landlordNotifications.clear();
        try {
            TenantAccountJsonStore.StoreData storeData = TenantAccountJsonStore.load(ACCOUNTS_STORAGE_FILE);
            if (storeData != null) {
                tenants.addAll(storeData.getTenants());
                landlordNotifications.addAll(storeData.getLandlordNotifications());
                backfillLandlordNotifications();
                return;
            }
        } catch (IOException exception) {
            System.err.println("Unable to load tenant accounts from " + ACCOUNTS_STORAGE_FILE + ": " + exception.getMessage());
        }

        seedDemoData();
        saveTenants();
    }

    private void saveTenants() {
        try {
            TenantAccountJsonStore.save(ACCOUNTS_STORAGE_FILE, tenants, landlordNotifications);
        } catch (IOException exception) {
            System.err.println("Unable to save tenant accounts to " + ACCOUNTS_STORAGE_FILE + ": " + exception.getMessage());
            throw new RuntimeException("CRITICAL: Failed to write data to disk. Your recent changes were not saved.", exception);
        }
    }

    private void seedDemoData() {
        landlordNotifications.clear();
        addDemoTenant(
                "Maria Santos",
                "maria.s",
                "maria123",
                "09171234567",
                "A-101",
                "Standard",
                PaymentStatus.PAID,
                VerificationStatus.CLEAR,
                "Payment verified for the current billing cycle.",
                "Landlord");
        addDemoTenant(
                "Joshua Lim",
                "josh.l",
                "josh123",
                "09184561234",
                "B-204",
                "Deluxe",
                PaymentStatus.PENDING,
                VerificationStatus.FOR_REVIEW,
                "Payment proof submitted and waiting for landlord review.",
                "Tenant");
        addDemoTenant(
                "Angela Cruz",
                "angela.c",
                "angela123",
                "09221239876",
                "C-301",
                "Family",
                PaymentStatus.LATE,
                VerificationStatus.REJECTED,
                "Receipt was too blurry to verify. Please upload a clearer image and resubmit.",
                "Landlord");
        backfillLandlordNotifications();
    }

    private void addDemoTenant(
            String fullName,
            String username,
            String password,
            String contactNumber,
            String roomNumber,
            String roomType,
            PaymentStatus paymentStatus,
            VerificationStatus verificationStatus,
            String note,
            String updatedBy) {
        RoomInfo roomInfo = new RoomInfo(roomNumber, roomType, getMonthlyRentForRoomType(roomType), DEFAULT_DUE_DAY);
        TenantAccount tenant = new TenantAccount(fullName, username, password, contactNumber, roomInfo);
        if (verificationStatus == VerificationStatus.FOR_REVIEW) {
            tenant.submitPaymentReceipt(
                    "Receipt photo uploaded for the current billing review.",
                    "Tenant",
                    "",
                    "");
            tenant.submitPaymentForVerification(note, updatedBy, "DEMO-2026-" + username.toUpperCase(Locale.ROOT));
        } else if (verificationStatus == VerificationStatus.REJECTED) {
            tenant.submitPaymentReceipt(
                    "Receipt photo uploaded for the current billing review.",
                    "Tenant",
                    "",
                    "");
            tenant.submitPaymentForVerification("Payment proof submitted by the tenant and is ready for landlord review.", "Tenant",
                    "DEMO-2026-" + username.toUpperCase(Locale.ROOT));
            tenant.rejectPaymentSubmission(note, updatedBy);
        } else {
            tenant.updatePaymentStatus(paymentStatus, note, updatedBy);
        }
        tenants.add(tenant);
    }

    private void addLandlordNotification(
            LandlordNotificationType type,
            TenantAccount tenant,
            String title,
            String message) {
        if (tenant == null) {
            return;
        }

        landlordNotifications.add(0, new LandlordNotification(
                type,
                tenant.getUsername(),
                tenant.getFullName(),
                title,
                message));
    }

    private void backfillLandlordNotifications() {
        if (!landlordNotifications.isEmpty()) {
            return;
        }

        for (TenantAccount tenant : tenants) {
            if (tenant == null) {
                continue;
            }

            if (tenant.isPaymentAwaitingVerification()) {
                String reference = "No reference";
                for (PaymentRecord paymentRecord : tenant.getPaymentHistory()) {
                    if (paymentRecord != null && paymentRecord.hasReferenceNumber()) {
                        reference = paymentRecord.getReferenceNumber();
                        break;
                    }
                }
                addLandlordNotification(
                        LandlordNotificationType.PAYMENT_SUBMITTED,
                        tenant,
                        "Payment proof submitted",
                        tenant.getFullName() + " already has a payment proof waiting for review. Reference: " + reference + ".");
            }
        }
    }

    public enum LandlordNotificationType {
        PAYMENT_SUBMITTED("Payment submitted"),
        NOTICE_OR_CONCERN("Notice/concern");

        private final String label;

        LandlordNotificationType(String label) {
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

    public static final class LandlordNotification {
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        private final LocalDateTime timestamp;
        private final LandlordNotificationType type;
        private final String tenantUsername;
        private final String tenantName;
        private final String title;
        private final String message;
        private boolean read;

        public LandlordNotification(
                LandlordNotificationType type,
                String tenantUsername,
                String tenantName,
                String title,
                String message) {
            this(LocalDateTime.now(), type, tenantUsername, tenantName, title, message, false);
        }

        public LandlordNotification(
                LocalDateTime timestamp,
                LandlordNotificationType type,
                String tenantUsername,
                String tenantName,
                String title,
                String message,
                boolean read) {
            this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
            this.type = type == null ? LandlordNotificationType.NOTICE_OR_CONCERN : type;
            this.tenantUsername = tenantUsername == null ? "" : tenantUsername.trim();
            this.tenantName = tenantName == null ? "" : tenantName.trim();
            this.title = title == null ? "" : title.trim();
            this.message = message == null ? "" : message.trim();
            this.read = read;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public LandlordNotificationType getType() {
            return type;
        }

        public String getTenantUsername() {
            return tenantUsername;
        }

        public String getTenantName() {
            return tenantName;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public boolean isRead() {
            return read;
        }

        public boolean isUnread() {
            return !read;
        }

        public void markRead() {
            read = true;
        }

        public void setRead(boolean read) {
            this.read = read;
        }

        public boolean matches(LandlordNotification other) {
            return other != null
                    && timestamp.equals(other.timestamp)
                    && type == other.type
                    && tenantUsername.equals(other.tenantUsername)
                    && tenantName.equals(other.tenantName)
                    && title.equals(other.title)
                    && message.equals(other.message);
        }

        public String getFormattedTimestamp() {
            return timestamp.format(DISPLAY_FORMATTER);
        }
    }

    public static final class DashboardSnapshot {
        private final List<TenantAccount> tenants;
        private final int totalTenantCount;
        private final int paidCount;
        private final int pendingCount;
        private final int lateCount;
        private final String query;

        public DashboardSnapshot(
                List<TenantAccount> tenants,
                int totalTenantCount,
                int paidCount,
                int pendingCount,
                int lateCount,
                String query) {
            this.tenants = Collections.unmodifiableList(new ArrayList<>(tenants));
            this.totalTenantCount = totalTenantCount;
            this.paidCount = paidCount;
            this.pendingCount = pendingCount;
            this.lateCount = lateCount;
            this.query = query;
        }

        public List<TenantAccount> getTenants() {
            return tenants;
        }

        public int getTotalTenantCount() {
            return totalTenantCount;
        }

        public int getPaidCount() {
            return paidCount;
        }

        public int getPendingCount() {
            return pendingCount;
        }

        public int getLateCount() {
            return lateCount;
        }

        public String getQuery() {
            return query;
        }
    }
}
