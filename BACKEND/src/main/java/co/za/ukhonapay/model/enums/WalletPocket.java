package co.za.ukhonapay.model.enums;

// The three balances every personal wallet holds - see Wallet.balance /
// savingsBalance / maintenanceBalance. Used by WalletService's internal
// transfer so a user can move money between their own pockets (e.g. pull
// savings back into the spendable balance).
public enum WalletPocket {
    BALANCE,
    SAVINGS,
    MAINTENANCE
}
