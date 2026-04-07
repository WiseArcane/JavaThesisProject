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
import staysync.core.TenantAccount.PaymentStatus;
import staysync.core.TenantAccount.RoomInfo;

public class StaySyncService {
    private static final String LANDLORD_USERNAME = "wise";
    private static final String LANDLORD_PASSWORD = "1234";
    private static final String[] ROOM_TYPES = { "Standard", "Deluxe", "Family" };
    private static final int DEFAULT_DUE_DAY = 5;
    private static final Path RECEIPT_STORAGE_DIRECTORY = Path.of("storage", "receipts");

    private final List<TenantAccount> tenants = new ArrayList<>();

    public StaySyncService() {
        seedDemoData();
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
                roomNumber.trim(),
                roomType,
                getMonthlyRentForRoomType(roomType),
                DEFAULT_DUE_DAY);
        tenants.add(new TenantAccount(
                fullName.trim(),
                normalizedEmail,
                normalizedUsername,
                password,
                contactNumber.trim(),
                roomInfo));
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
        updatePaymentStatus(
                tenant,
                PaymentStatus.PAID,
                "Payment marked as paid by the tenant.",
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
            return null;
        } catch (IOException exception) {
            return "The receipt photo could not be saved. Please try a different image.";
        }
    }

    public synchronized String updateTenantProfile(
            TenantAccount tenant,
            String fullName,
            String contactNumber,
            String roomNumber,
            String roomType) {
        if (tenant == null) {
            return "Tenant account was not found.";
        }

        String validationMessage = validateTenantProfile(fullName, contactNumber, roomNumber, roomType);
        if (validationMessage != null) {
            return validationMessage;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant == null) {
            return "Tenant account was not found.";
        }

        storedTenant.updateProfile(
                fullName.trim(),
                contactNumber.trim(),
                roomNumber.trim(),
                roomType,
                getMonthlyRentForRoomType(roomType));
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

        storedTenant.updateMonthlyRent(monthlyRent);
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
                || isBlank(contactNumber)
                || isBlank(roomNumber)
                || isBlank(roomType)) {
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

        if (!roomNumber.trim().matches("[A-Za-z0-9-]{1,12}")) {
            return "Room number must be 1 to 12 characters and may include letters, numbers, or dash.";
        }

        return null;
    }

    private String validateTenantProfile(String fullName, String contactNumber, String roomNumber, String roomType) {
        if (isBlank(fullName) || isBlank(contactNumber) || isBlank(roomNumber) || isBlank(roomType)) {
            return "All profile fields are required.";
        }

        if (fullName.trim().length() < 3) {
            return "Full name must contain at least 3 characters.";
        }

        if (!contactNumber.trim().matches("[0-9+\\- ]{7,15}")) {
            return "Contact number must be 7 to 15 characters and may include digits, spaces, plus, or dash.";
        }

        if (!roomNumber.trim().matches("[A-Za-z0-9-]{1,12}")) {
            return "Room number must be 1 to 12 characters and may include letters, numbers, or dash.";
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

    private synchronized List<TenantAccount> searchTenants(String query) {
        if (query.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<>(tenants));
        }

        String searchValue = query.toLowerCase();
        List<TenantAccount> matches = new ArrayList<>();
        for (TenantAccount tenant : tenants) {
            if (tenant.getFullName().toLowerCase().contains(searchValue)
                    || tenant.getRoomInfo().getRoomNumber().toLowerCase().contains(searchValue)) {
                matches.add(tenant);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    private void updatePaymentStatus(TenantAccount tenant, PaymentStatus status, String note, String updatedBy) {
        if (tenant == null || status == null) {
            return;
        }

        TenantAccount storedTenant = findTenantByUsername(tenant.getUsername());
        if (storedTenant != null) {
            storedTenant.updatePaymentStatus(status, note, updatedBy);
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
