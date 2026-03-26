package staysync.ui;

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import staysync.core.StaySyncService;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.PaymentStatus;

public class StaySyncFrame extends JFrame implements AuthActions, TenantActions, LandlordActions {
    private static final String WELCOME_SCREEN = "welcome";
    private static final String TENANT_LOGIN_SCREEN = "tenantLogin";
    private static final String TENANT_REGISTER_SCREEN = "tenantRegister";
    private static final String LANDLORD_LOGIN_SCREEN = "landlordLogin";
    private static final String TENANT_DASHBOARD_SCREEN = "tenantDashboard";
    private static final String LANDLORD_DASHBOARD_SCREEN = "landlordDashboard";

    private final StaySyncService staySyncService;
    private final CardLayout cardLayout;
    private final JPanel screenPanel;

    private WelcomePanel welcomePanel;
    private TenantLoginPanel tenantLoginPanel;
    private TenantRegistrationPanel tenantRegistrationPanel;
    private LandlordLoginPanel landlordLoginPanel;
    private TenantDashboardPanel tenantDashboardPanel;
    private LandlordDashboardPanel landlordDashboardPanel;
    private String currentScreen;

    public StaySyncFrame() {
        AppTheme.install();
        staySyncService = new StaySyncService();
        cardLayout = new CardLayout();
        screenPanel = new JPanel(cardLayout);
        rebuildScreens();

        setTitle("StaySync Dorm and Apartment Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        setContentPane(screenPanel);
        showWelcomeScreen();
    }

    @Override
    public void showWelcomeScreen() {
        tenantLoginPanel.clearFields();
        tenantRegistrationPanel.clearFields();
        landlordLoginPanel.clearFields();
        landlordDashboardPanel.clearSearch();
        showScreen(WELCOME_SCREEN);
    }

    @Override
    public void showTenantLoginScreen() {
        tenantLoginPanel.clearFields();
        showScreen(TENANT_LOGIN_SCREEN);
    }

    @Override
    public void showTenantRegistrationScreen() {
        tenantRegistrationPanel.clearFields();
        showScreen(TENANT_REGISTER_SCREEN);
    }

    @Override
    public void showLandlordLoginScreen() {
        landlordLoginPanel.clearFields();
        showScreen(LANDLORD_LOGIN_SCREEN);
    }

    @Override
    public void registerTenant(
            String fullName,
            String username,
            String password,
            String confirmPassword,
            String contactNumber,
            String roomNumber,
            String roomType) {
        String registerMessage = staySyncService.registerTenant(
                fullName,
                username,
                password,
                confirmPassword,
                contactNumber,
                roomNumber,
                roomType);

        if (registerMessage != null) {
            tenantRegistrationPanel.showError(registerMessage);
            return;
        }

        tenantRegistrationPanel.showSuccess("Registration successful. You can now log in as a tenant.");
        showTenantLoginScreen();
        tenantLoginPanel.setUsername(username);
        tenantLoginPanel.showSuccess("Account created. Sign in with your new tenant credentials.");
    }

    @Override
    public void loginTenant(String username, String password) {
        String validationMessage = staySyncService.validateLoginCredentials(username, password);
        if (validationMessage != null) {
            tenantLoginPanel.showError(validationMessage);
            return;
        }

        TenantAccount tenant = staySyncService.authenticateTenant(username, password);
        if (tenant == null) {
            tenantLoginPanel.showError("Invalid tenant credentials.");
            return;
        }

        tenantLoginPanel.clearFeedback();
        tenantDashboardPanel.setTenant(tenant);
        tenantDashboardPanel.showFeedback("Signed in successfully. Your dashboard is ready.", true);
        showScreen(TENANT_DASHBOARD_SCREEN);
    }

    @Override
    public void loginLandlord(String username, String password) {
        String validationMessage = staySyncService.validateLoginCredentials(username, password);
        if (validationMessage != null) {
            landlordLoginPanel.showError(validationMessage);
            return;
        }

        if (!staySyncService.authenticateLandlord(username.trim(), password)) {
            landlordLoginPanel.showError("Invalid landlord credentials.");
            return;
        }

        landlordLoginPanel.clearFeedback();
        updateLandlordDashboard("");
        showScreen(LANDLORD_DASHBOARD_SCREEN);
    }

    @Override
    public void toggleTheme() {
        String screenToRestore = currentScreen == null ? WELCOME_SCREEN : currentScreen;
        String landlordQuery = landlordDashboardPanel == null ? "" : landlordDashboardPanel.getSearchQuery();
        TenantAccount tenant = tenantDashboardPanel == null ? null : tenantDashboardPanel.getCurrentTenant();

        AppTheme.toggleMode();
        rebuildScreens();

        if (tenant != null) {
            tenantDashboardPanel.setTenant(tenant);
        }
        if (LANDLORD_DASHBOARD_SCREEN.equals(screenToRestore)) {
            updateLandlordDashboard(landlordQuery);
        }

        showScreen(screenToRestore);
    }

    @Override
    public boolean isDarkMode() {
        return AppTheme.isDarkMode();
    }

    @Override
    public void markTenantAsPaid(TenantAccount tenant) {
        if (tenant == null) {
            return;
        }

        staySyncService.markTenantAsPaid(tenant);
        tenantDashboardPanel.refreshTenantData();
        tenantDashboardPanel.showFeedback("Payment status updated to Paid.", true);
        refreshLandlordDashboard();
    }

    @Override
    public void refreshLandlordDashboard() {
        updateLandlordDashboard(landlordDashboardPanel.getSearchQuery());
    }

    @Override
    public void searchLandlordTenants(String query) {
        updateLandlordDashboard(query);
    }

    @Override
    public void updateTenantStatusFromLandlord(TenantAccount tenant, PaymentStatus status) {
        if (tenant == null || status == null) {
            JOptionPane.showMessageDialog(this, "Select a tenant and a payment status first.", "Update Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        staySyncService.updateTenantStatusFromLandlord(tenant, status);
        tenantDashboardPanel.refreshTenantData();
        tenantDashboardPanel.showFeedback(
                "Landlord updated " + tenant.getFullName() + " to " + status.getLabel() + ".",
                true);
        refreshLandlordDashboard();
    }

    @Override
    public void editTenantProfile(TenantAccount tenant) {
        if (tenant == null) {
            return;
        }

        TenantProfileFormPanel formPanel = new TenantProfileFormPanel();
        formPanel.setTenant(tenant);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    formPanel,
                    "Edit Tenant Profile",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String updateMessage = staySyncService.updateTenantProfile(
                    tenant,
                    formPanel.getFullNameValue(),
                    formPanel.getContactNumberValue(),
                    formPanel.getRoomNumberValue(),
                    formPanel.getRoomTypeValue());
            if (updateMessage != null) {
                JOptionPane.showMessageDialog(this, updateMessage, "Profile Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            tenantDashboardPanel.refreshTenantData();
            tenantDashboardPanel.showFeedback("Tenant profile updated successfully.", true);
            refreshLandlordDashboard();
            return;
        }
    }

    @Override
    public void changeTenantPassword(TenantAccount tenant) {
        if (tenant == null) {
            return;
        }

        ChangePasswordPanel changePasswordPanel = new ChangePasswordPanel();

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    changePasswordPanel,
                    "Change Password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String passwordMessage = staySyncService.changeTenantPassword(
                    tenant,
                    changePasswordPanel.getCurrentPasswordValue(),
                    changePasswordPanel.getNewPasswordValue(),
                    changePasswordPanel.getConfirmPasswordValue());
            if (passwordMessage != null) {
                JOptionPane.showMessageDialog(this, passwordMessage, "Password Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            tenantDashboardPanel.showFeedback("Password changed successfully.", true);
            return;
        }
    }

    private void updateLandlordDashboard(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        landlordDashboardPanel.setSearchQuery(normalizedQuery);
        landlordDashboardPanel.refreshTable(staySyncService.getLandlordDashboardData(normalizedQuery));
    }

    private void rebuildScreens() {
        welcomePanel = new WelcomePanel(this);
        tenantLoginPanel = new TenantLoginPanel(this);
        tenantRegistrationPanel = new TenantRegistrationPanel(this);
        landlordLoginPanel = new LandlordLoginPanel(this);
        tenantDashboardPanel = new TenantDashboardPanel(this);
        landlordDashboardPanel = new LandlordDashboardPanel(this);

        screenPanel.removeAll();
        screenPanel.add(welcomePanel, WELCOME_SCREEN);
        screenPanel.add(tenantLoginPanel, TENANT_LOGIN_SCREEN);
        screenPanel.add(tenantRegistrationPanel, TENANT_REGISTER_SCREEN);
        screenPanel.add(landlordLoginPanel, LANDLORD_LOGIN_SCREEN);
        screenPanel.add(tenantDashboardPanel, TENANT_DASHBOARD_SCREEN);
        screenPanel.add(landlordDashboardPanel, LANDLORD_DASHBOARD_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    private void showScreen(String screen) {
        currentScreen = screen;
        cardLayout.show(screenPanel, screen);
    }
}
