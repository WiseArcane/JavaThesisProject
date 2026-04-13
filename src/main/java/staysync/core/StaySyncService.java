package staysync.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import staysync.core.TenantAccount.NotificationType;
import staysync.core.TenantAccount.CoOccupantRequest;
import staysync.core.TenantAccount.PaymentStatus;
import staysync.core.TenantAccount.RoomInfo;

public class StaySyncService {
    private static final String LANDLORD_USERNAME = "wise";
    private static final String LANDLORD_PASSWORD = "1234";
    private static final String[] ROOM_TYPES = { "Standard", "Deluxe", "Family" };
    private static final int DEFAULT_DUE_DAY = 5;
    private static final Path ACCOUNTS_STORAGE_FILE = Path.of("storage", "accounts.json");
    private static final Path RECEIPT_STORAGE_DIRECTORY = Path.of("storage", "receipts");

    private final List<TenantAccount> tenants = new ArrayList<>();

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
        submitTenantPaymentForVerification(tenant);
    }

    public synchronized void submitTenantPaymentForVerification(TenantAccount tenant) {
        updatePaymentStatus(
                tenant,
                null,
                "Payment submitted by the tenant and is waiting for landlord verification.",
                "Tenant");
    }

    public synchronized void updateTenantStatusFromLandlord(TenantAccount tenant, PaymentStatus status) {
        updatePaymentStatus(
                tenant,
                status,
                "Payment status updated by the landlord.",
                "Landlord");
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

        try {
            Path storedReceipt = storeReceiptCopy(storedTenant, sourceImage);
            storedTenant.submitPaymentReceipt(
                    "Receipt photo submitted by the tenant for landlord review.",
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
            if (status == null) {
                if (!storedTenant.isPaymentAwaitingVerification()) {
                    storedTenant.submitPaymentForVerification(note, updatedBy);
                    saveTenants();
                }
                return;
            }
            storedTenant.updatePaymentStatus(status, note, updatedBy);
            saveTenants();
        }
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
        try {
            List<TenantAccount> storedTenants = TenantAccountJsonStore.load(ACCOUNTS_STORAGE_FILE);
            if (storedTenants != null) {
                tenants.addAll(storedTenants);
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
            TenantAccountJsonStore.save(ACCOUNTS_STORAGE_FILE, tenants);
        } catch (IOException exception) {
            System.err.println("Unable to save tenant accounts to " + ACCOUNTS_STORAGE_FILE + ": " + exception.getMessage());
        }
    }

    private void seedDemoData() {
        addDemoTenant("Maria Santos", "maria.s", "maria123", "09171234567", "A-101", "Standard", PaymentStatus.PAID,
                "Payment settled for the current month.", "Landlord");
        addDemoTenant("Joshua Lim", "josh.l", "josh123", "09184561234", "B-204", "Deluxe", PaymentStatus.PENDING,
                "Awaiting payment before the due date.", "System");
        addDemoTenant("Angela Cruz", "angela.c", "angela123", "09221239876", "C-301", "Family", PaymentStatus.LATE,
                "Payment has not been received after the deadline.", "Landlord");
    }

    private void addDemoTenant(
            String fullName,
            String username,
            String password,
            String contactNumber,
            String roomNumber,
            String roomType,
            PaymentStatus paymentStatus,
            String note,
            String updatedBy) {
        RoomInfo roomInfo = new RoomInfo(roomNumber, roomType, getMonthlyRentForRoomType(roomType), DEFAULT_DUE_DAY);
        TenantAccount tenant = new TenantAccount(fullName, username, password, contactNumber, roomInfo);
        tenant.updatePaymentStatus(paymentStatus, note, updatedBy);
        tenants.add(tenant);
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
