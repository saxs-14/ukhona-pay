# COPY EVERYTHING BELOW INTO: ~/marketplace-connect-pay/RESEARCH/PLATFORM_CONCEPT.md

================================================================================
MARKETPLACE CONNECT - FINTECH PAYMENT & SAVINGS PLATFORM
Market Connectivity Challenge - EDHE Hackathon 2026
Java Backend + React Frontend + ABSA Integration
================================================================================

PROJECT PIVOT & NEW DIRECTION
================================================================================

UPDATED FOCUS:
   • Challenge: MARKET CONNECTIVITY (100% focus)
   • Platform Type: Fintech Payment & Savings Platform
   • Target Users: Vendors, taxi drivers, service providers, corporate buyers
   • Core Value: Unified payment, savings, and cashback ecosystem
   • Key Feature: Money saved on platform can be withdrawn as cashback
   • Corporate Partner: ABSA Bank (South Africa)
   • Approach: Themeless now, adaptable to hackathon theme later

================================================================================
CORE PROBLEM & OPPORTUNITY
================================================================================

MARKET CONNECTIVITY CHALLENGE IN PAYMENT SYSTEMS:

Current State:
   • Vendors operate in isolation (spaza shops, informal traders)
   • Taxi drivers handle cash transactions with no record/protection
   • Buyers have no consolidated payment system across different vendors
   • Service providers cannot securely receive payments
   • No ecosystem connecting informal economy to formal banking

Market Opportunity:
   • 2M+ informal vendors, taxi drivers, service providers in South Africa
   • Collective transactions: R50B+ annually (fragmented, cash-based)
   • Corporate buyers (Absa's target) need integrated payment solutions
   • Unmet need: Payment + savings + cashback in single platform
   • Policy tailwind: Government push for formalization & digital inclusion

================================================================================
PLATFORM CONCEPT: MARKETPLACE CONNECT PAY
================================================================================

NAME: MARKETPLACE CONNECT PAY
Tagline: "Pay. Save. Earn Cashback. Connect."

WHAT IT DOES:

Three-sided marketplace where:

   SIDE 1: VENDORS (Spaza shops, taxi drivers, service providers)
      • Accept digital payments from customers
      • View transaction history and earnings
      • Withdraw funds to bank account (ABSA integration)
      • Build digital reputation/ratings
      • Access to working capital (future feature)

   SIDE 2: CORPORATE BUYERS & EMPLOYEES
      • Pay vendors, taxi drivers, service providers digitally
      • No cash handling (security + convenience)
      • Earn cashback on every transaction (2-5% typical)
      • Save accumulated cashback in wallet
      • Withdraw cashback as cash or transfer to bank
      • Track spending and savings

   SIDE 3: ABSA BANK (Corporate Partner)
      • White-label payment infrastructure
      • Aggregated transaction data for corporate clients
      • Lending risk assessment based on transaction history
      • Commission on transactions
      • Integration with existing ABSA banking services

================================================================================
HOW IT SOLVES MARKET CONNECTIVITY
================================================================================

FRAGMENTATION PROBLEM → PLATFORM SOLUTION

Problem 1: Vendors Isolated from Digital Economy
   Status Quo: Spaza shop owner only takes cash, has no digital footprint
   Platform Solution: Accept digital payments, build digital identity
   Connectivity Gain: Vendor now visible to corporate buyers, ABSA sees financials

Problem 2: Corporate Buyers Have No Efficient Payment System
   Status Quo: Company reimburses employees for taxi, vendor, service provider 
              payments; employees handle cash
   Platform Solution: Employees use MARKETPLACE PAY to pay vendors/taxi drivers 
                     directly; company gets consolidated reports
   Connectivity Gain: Corporate buyer connected to entire vendor network

Problem 3: Taxi Drivers Operate Outside Formal System
   Status Quo: Taxi drivers collect cash; no record, no safety, no banking
   Platform Solution: Accept digital payments via platform, auto-settlement
   Connectivity Gain: Taxi driver formalized, corporate buyer can pay digitally

Problem 4: No Trust/Verification in Informal Economy
   Status Quo: Buyer worried about vendor legitimacy, payment security
   Platform Solution: Platform verification, rating system, dispute resolution
   Connectivity Gain: Trust layer enables larger transactions

RESULT: Fragmented informal economy becomes connected, visible, and 
        formal-ready

================================================================================
CORE FEATURES (MVP - 48 Hours)
================================================================================

FEATURE SET 1: PAYMENT & WALLET
─────────────────────────────────

For All Users:
   ✓ Digital wallet (account balance)
   ✓ Send money to other users (peer-to-peer)
   ✓ Receive money from other users
   ✓ View transaction history
   ✓ Real-time transaction notifications

For Vendors/Service Providers:
   ✓ Generate payment QR code (customer scans to pay)
   ✓ Accept mobile payments (phone number or QR)
   ✓ Daily settlement summary
   ✓ Withdraw to bank account (ABSA integration - mock for MVP)

For Corporate/Employees:
   ✓ Search vendors/taxi drivers by category or location
   ✓ Quick pay button (send money with one tap)
   ✓ Save favorite vendors
   ✓ Payment request to vendor

─────────────────────────────────

FEATURE SET 2: SAVINGS & CASHBACK
──────────────────────────────────

For Corporate/Employees:
   ✓ Earn cashback on each transaction (2-5% configurable)
   ✓ Cashback auto-added to savings wallet
   ✓ View total cashback earned
   ✓ Withdraw cashback to bank (mock ABSA integration)
   ✓ Cashback history/breakdown by category

For Vendors:
   ✓ View money received today/week/month
   ✓ Set withdrawal preferences (daily, weekly, monthly)
   ✓ Automatic settlement schedule

─────────────────────────────────

FEATURE SET 3: VENDOR MANAGEMENT & TRUST
──────────────────────────────────────────

For Vendors:
   ✓ Create vendor profile (name, category, location)
   ✓ Verify phone number (SMS OTP)
   ✓ Upload business photo
   ✓ View transaction history
   ✓ Rating/review from customers

For Corporate Buyers:
   ✓ Search vendors by category (taxi, food, services, retail)
   ✓ Filter by location or rating
   ✓ See vendor verification status
   ✓ View ratings and reviews
   ✓ Contact vendor directly

─────────────────────────────────

FEATURE SET 4: DASHBOARD & REPORTING
──────────────────────────────────────

For Vendors:
   ✓ Daily earnings dashboard
   ✓ Transaction volume chart
   ✓ Customer count growing
   ✓ Rating trend
   ✓ Quick actions: withdraw, view history

For Corporate/Employees:
   ✓ Total spending this month
   ✓ Total cashback earned
   ✓ Savings progress
   ✓ Category breakdown (taxi, food, services)
   ✓ Recent transactions

For ABSA (Backend Analytics):
   ✓ Aggregated transaction volume
   ✓ Vendor growth metrics
   ✓ Default risk indicators (for lending decisions)
   ✓ Corporate spending patterns

================================================================================
TECHNICAL ARCHITECTURE
================================================================================

FRONTEND: React.js
─────────────────

Structure:
   src/
   ├── components/
   │   ├── auth/
   │   │   ├── LoginPage.jsx
   │   │   ├── SignupPage.jsx
   │   │   └── OTPVerification.jsx
   │   ├── wallet/
   │   │   ├── WalletDashboard.jsx
   │   │   ├── SendMoney.jsx
   │   │   ├── ReceiveMoney.jsx
   │   │   ├── TransactionHistory.jsx
   │   │   └── Withdraw.jsx
   │   ├── vendor/
   │   │   ├── VendorProfile.jsx
   │   │   ├── VendorDashboard.jsx
   │   │   ├── GenerateQR.jsx
   │   │   ├── EarningsView.jsx
   │   │   └── SettlementSchedule.jsx
   │   ├── corporate/
   │   │   ├── CorporateSearch.jsx
   │   │   ├── VendorSearch.jsx
   │   │   ├── CashbackDashboard.jsx
   │   │   ├── SpendingAnalytics.jsx
   │   │   └── WithdrawCashback.jsx
   │   ├── common/
   │   │   ├── NavBar.jsx
   │   │   ├── QRScanner.jsx
   │   │   ├── PaymentConfirmation.jsx
   │   │   └── Notification.jsx
   ├── pages/
   │   ├── HomePage.jsx
   │   ├── ProfilePage.jsx
   │   └── SettingsPage.jsx
   ├── services/
   │   ├── authService.js
   │   ├── paymentService.js
   │   ├── walletService.js
   │   ├── vendorService.js
   │   └── abisIntegration.js
   ├── styles/
   │   ├── global.css
   │   ├── theme.css
   │   └── responsive.css
   ├── utils/
   │   ├── validation.js
   │   ├── formatters.js
   │   └── constants.js
   └── App.jsx

Key Libraries:
   • React Router (navigation)
   • Axios (API calls)
   • Tailwind CSS (styling)
   • React QR Code (QR generation/scanning)
   • Chart.js (analytics charts)
   • Redux (state management - optional)

─────────────────────────────────

BACKEND: Java Spring Boot
─────────────────────────

Structure:
   src/main/java/com/marketplaceconnect/
   ├── controller/
   │   ├── AuthController.java
   │   ├── PaymentController.java
   │   ├── WalletController.java
   │   ├── VendorController.java
   │   ├── TransactionController.java
   │   ├── CashbackController.java
   │   └── AbsaIntegrationController.java
   ├── service/
   │   ├── AuthService.java
   │   ├── PaymentService.java
   │   ├── WalletService.java
   │   ├── VendorService.java
   │   ├── TransactionService.java
   │   ├── CashbackService.java
   │   └── AbsaService.java
   ├── repository/
   │   ├── UserRepository.java
   │   ├── WalletRepository.java
   │   ├── VendorRepository.java
   │   ├── TransactionRepository.java
   │   ├── CashbackRepository.java
   │   └── SettlementRepository.java
   ├── model/
   │   ├── User.java
   │   ├── Wallet.java
   │   ├── Vendor.java
   │   ├── Transaction.java
   │   ├── Cashback.java
   │   └── Settlement.java
   ├── dto/
   │   ├── PaymentRequest.java
   │   ├── PaymentResponse.java
   │   ├── VendorProfile.java
   │   ├── TransactionDTO.java
   │   └── CashbackDTO.java
   ├── security/
   │   ├── JwtTokenProvider.java
   │   ├── CustomUserDetailsService.java
   │   └── SecurityConfig.java
   ├── integration/
   │   ├── AbsaPaymentGateway.java
   │   ├── AbsaSMSService.java
   │   └── AbsaWebhookListener.java
   ├── exception/
   │   ├── InsufficientFundsException.java
   │   ├── VendorNotFoundException.java
   │   ├── PaymentProcessingException.java
   │   └── GlobalExceptionHandler.java
   └── MarketplaceConnectApplication.java

Key Dependencies:
   • Spring Boot 3.x
   • Spring Data JPA (database)
   • Spring Security (authentication)
   • Spring Web (REST APIs)
   • PostgreSQL/MySQL (database)
   • Lombok (reduce boilerplate)
   • JWT (authentication tokens)
   • RestTemplate or Feign (external API calls to ABSA)
   • JUnit 5 & Mockito (testing)

─────────────────────────────────

DATABASE: PostgreSQL/MySQL
──────────────────────────

Core Tables:

   users
   ├── id (PK)
   ├── phone_number (unique)
   ├── password (hashed)
   ├── user_type (VENDOR, CORPORATE, EMPLOYEE)
   ├── name
   ├── email
   ├── created_at
   └── updated_at

   wallets
   ├── id (PK)
   ├── user_id (FK)
   ├── balance (decimal)
   ├── currency (ZAR)
   ├── locked_amount (for pending transactions)
   └── updated_at

   vendors
   ├── id (PK)
   ├── user_id (FK)
   ├── category (TAXI, FOOD, SERVICES, RETAIL)
   ├── location (lat/lng)
   ├── rating (1-5 stars)
   ├── verified (boolean)
   ├── qr_code (unique)
   └── updated_at

   transactions
   ├── id (PK)
   ├── sender_id (FK to users)
   ├── receiver_id (FK to users)
   ├── amount (decimal)
   ├── cashback_amount (calculated)
   ├── status (PENDING, COMPLETED, FAILED)
   ├── description
   ├── timestamp
   └── reference

   cashback
   ├── id (PK)
   ├── user_id (FK)
   ├── transaction_id (FK)
   ├── cashback_earned (decimal)
   ├── status (EARNED, WITHDRAWN)
   ├── withdrawn_date
   └── timestamp

   settlements
   ├── id (PK)
   ├── vendor_id (FK)
   ├── amount (decimal)
   ├── status (PENDING, SETTLED, FAILED)
   ├── settlement_date
   ├── bank_reference
   └── created_at

================================================================================
MVP SCOPE (48-HOUR HACKATHON)
================================================================================

MUST BUILD (Core MVP):

Frontend - React:
   ✓ Login/Signup (phone + OTP)
   ✓ Vendor profile setup
   ✓ Employee/Corporate profile
   ✓ Wallet dashboard (view balance)
   ✓ Send money (to vendor or employee)
   ✓ Receive money (notifications)
   ✓ View transaction history
   ✓ QR code generation (vendors)
   ✓ Cashback display
   ✓ Withdraw cashback (mock)

Backend - Java:
   ✓ User authentication (JWT)
   ✓ Phone verification (mock SMS)
   ✓ Wallet management (credit/debit)
   ✓ Payment processing
   ✓ Cashback calculation (2-5% per transaction)
   ✓ Transaction history API
   ✓ Vendor search endpoint
   ✓ Withdrawal request endpoint (mock ABSA)
   ✓ Database schema & repository layer

Database:
   ✓ Users table
   ✓ Wallets table
   ✓ Transactions table
   ✓ Vendors table
   ✓ Cashback table

DEMO DATA:
   ✓ 5 vendors (taxi, food, services)
   ✓ 10 corporate employees
   ✓ 20 sample transactions
   ✓ R5,000 total transaction volume
   ✓ R250 total cashback earned

SKIP (NOT IN MVP):
   ✗ Real ABSA API integration (mock responses)
   ✗ Real SMS gateway (mocked OTP)
   ✗ Multi-language support
   ✗ Mobile app (web only)
   ✗ Analytics dashboard (basic version only)
   ✗ Dispute resolution system
   ✗ Advanced vendor verification
   ✗ Machine learning for credit scoring

DELIVERABLE:
   • Working web platform (React + Java)
   • Live demo: Employee pays vendor, sees cashback, withdraws funds
   • Vendor sees money received, views earnings
   • Data stored in database
   • All accessible via web browser

================================================================================
GO-TO-MARKET (Post-Hackathon)
================================================================================

PHASE 1: ABSA Pilot (Months 1-3)
   • Partnership with Absa
   • Real API integration
   • 10 Absa corporate clients pilot
   • 100 vendors in pilot zones
   • Goal: Prove concept with real Absa ecosystem

PHASE 2: Market Expansion (Months 3-6)
   • Launch to other banks (FNB, Capitec, Investec)
   • Expand to 1,000 vendors
   • Expand to 50 corporate clients
   • Target: R50M transaction volume

PHASE 3: Scale (Months 6-12)
   • National rollout
   • Mobile app launch
   • Lending product (vendor microloans)
   • Corporate reimbursement suite
   • Target: R500M+ transaction volume

REVENUE MODEL:
   • 0.5-1% commission on transactions (Absa/bank takes 0.5%, platform keeps 0.5%)
   • Corporate data services (selling aggregated insights to Absa)
   • Premium features for vendors (advanced analytics, marketing tools)
   • Lending origination fees (1-2% on vendor loans)
   Year 1 Projection: R5-10M revenue

================================================================================
SUMMARY: PROJECT STRUCTURE
================================================================================

Project Name:        MARKETPLACE CONNECT PAY
Challenge:           Market Connectivity (100%)
Target Market:       Vendors, taxi drivers, corporate buyers
Core Value:          Payment + Savings + Cashback + Bank Integration
Tech Stack:
   Frontend:         React.js
   Backend:          Java Spring Boot
   Database:         PostgreSQL/MySQL
   Integration:      ABSA Bank APIs (mocked for MVP)
MVP Timeline:        48 hours (hackathon weekend)
Post-Hackathon:      Phase 1 (Absa pilot), Phase 2 (expansion), Phase 3 (scale)

================================================================================
