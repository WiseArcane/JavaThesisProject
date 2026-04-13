# StaySync System Features

## Overview

StaySync is a Java-based dorm and apartment management system with separate workspaces for tenants and landlords. The application focuses on resident onboarding, room assignment, payment tracking, receipt verification, co-occupant approval, and tenant notifications.

The current implementation includes:

- Role-based access for `Tenant` and `Landlord`
- Account persistence using `storage/accounts.json`
- Receipt image storage under `storage/receipts/`
- Seeded demo tenant data and a default landlord account

## User Roles

### Tenant

Tenants can:

- Register a new account with full name, email, username, password, and contact number
- Sign in using username or email
- Reset their password through the forgot-password flow using their contact number
- View their room assignment, room type, monthly rent, due day, and payment status
- Edit profile details such as full name and contact number
- Change their password after logging in
- Submit payment for landlord verification
- Upload a receipt photo as proof of payment
- View payment history with timestamps, status, notes, and receipt attachment state
- Open previously uploaded receipt files when available
- Receive and view notifications from the landlord
- Submit a co-occupant request for landlord approval
- Track whether a co-occupant request is pending, approved, or rejected
- Switch between light mode and dark mode

### Landlord

Landlords can:

- Sign in through a dedicated landlord login
- View a dashboard summary of all tenants
- Monitor total tenant count and payment counts for `Paid`, `Pending`, and `Late`
- Search tenants by name, occupant name, approved co-occupant name, or room number
- Filter tenant records by payment status
- View tenant details including room assignment, rent, contact number, and current payment status
- Update tenant payment status to `Paid`, `Pending`, or `Late`
- Review the latest uploaded payment receipt for a tenant
- Assign or update room number, room type, and monthly rent
- Approve or reject co-occupant requests
- Send tenant notifications such as payment reminders, maintenance notices, and general updates
- Delete tenant records
- Refresh dashboard data from persisted storage
- Switch between light mode and dark mode

## Functional Modules

### 1. Authentication and Access Control

The system provides separate entry points for tenants and landlords.

Implemented capabilities:

- Tenant login validation
- Landlord login validation
- Tenant registration with duplicate username and duplicate email checks
- Forgot-password / password reset using:
  - username or email
  - matching contact number
  - new password confirmation

Default landlord credentials in the current implementation:

- Username: `wise`
- Password: `1234`

### 2. Tenant Account Management

Each tenant account stores:

- Full name
- Email
- Username
- Password
- Contact number
- Room information
- Payment status
- Payment history
- Notifications
- Approved co-occupant information
- Co-occupant request state

Validation rules currently enforced include:

- Full name must have at least 3 characters
- Username must be 4 to 20 valid characters
- Password must be at least 4 characters
- Contact number must follow the accepted numeric format
- Email must be valid if provided

### 3. Room Assignment and Billing

The system supports room and rent management through landlord controls.

Implemented room/billing features:

- Room assignment per tenant
- Supported room types:
  - `Standard`
  - `Deluxe`
  - `Family`
- Monthly rent assignment
- Default due day set to day `5` of each month
- Tenant-side display of room, rent, and due reminders

### 4. Payment Monitoring

StaySync tracks tenant billing using payment status and history records.

Supported payment states:

- `Paid`
- `Pending`
- `Late`

Implemented payment features:

- Tenant marks payment as submitted for verification
- Landlord verifies and updates the final payment status
- Payment history stores:
  - timestamp
  - status
  - note
  - who updated the record
- Dashboard summaries count paid, pending, and late accounts

### 5. Receipt Upload and Verification

The tenant can attach proof of payment, and the landlord can review it.

Implemented receipt capabilities:

- Upload receipt image files
- Accept supported image formats:
  - `.png`
  - `.jpg`
  - `.jpeg`
  - `.gif`
  - `.bmp`
- Save receipt copies to tenant-specific folders in `storage/receipts/`
- Track receipt attachment state in payment history
- Preview or open stored receipt files when available

### 6. Notifications

The system includes tenant-facing notifications sent by the landlord.

Supported notification types:

- Payment reminder
- Maintenance notice
- General update
- Co-occupant update

Each notification stores:

- type
- title
- message
- sender
- timestamp

### 7. Co-Occupant Management

Tenants can request approval for a roommate or co-occupant, and landlords can act on that request.

Implemented co-occupant features:

- Submit co-occupant request by name
- Prevent duplicate pending requests
- Prevent requesting the tenant's own name
- Approve a co-occupant request
- Reject a co-occupant request
- Display approved co-occupant name in occupant information
- Notify the tenant when the request is approved or rejected

Supported request states:

- `Pending`
- `Approved`
- `Rejected`

### 8. Dashboard and Search

The landlord workspace includes operational monitoring tools.

Implemented dashboard features:

- Portfolio overview cards
- Tenant table / resident list
- Search by tenant and room-related fields
- Quick status filtering
- Selected-tenant control panel
- Refresh action for reloading current records

### 9. Data Persistence

The system uses file-based persistence instead of a database.

Current persistence behavior:

- Tenant accounts are loaded from `storage/accounts.json`
- If no stored accounts are found, demo data is seeded automatically
- Changes are saved after registration, profile updates, password changes, room assignment changes, payment updates, receipt uploads, notifications, co-occupant actions, and tenant deletion

## Demo Data

If no saved data exists yet, the system seeds sample tenant accounts for demonstration. This helps test:

- tenant login
- landlord monitoring
- payment status tracking
- search and filtering

## Summary

StaySync currently functions as a desktop property management system for small-scale dormitory or apartment operations. Its strongest implemented features are:

- tenant onboarding
- room and rent management
- payment tracking
- receipt-based verification
- landlord-to-tenant communication
- co-occupant approval workflow
- persistent local storage for account records
