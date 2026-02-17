// ── Auth ──────────────────────────────────────────────
export interface AuthResponse {
    userId: string
    username: string
    token: string
    expiresInMs: number
}

export interface RegisterRequest {
    username: string
    email: string
    password: string
}

export interface LoginRequest {
    username: string
    password: string
}

// ── User Profile ─────────────────────────────────────
export interface UserProfile {
    userId: string
    username: string
    email: string
    accounts: Account[]
}

// ── Accounts ─────────────────────────────────────────
export interface Account {
    id: string
    userId: string
    accountNumber: string
    balance: number
    currency: string
    status: string
    createdAt: string
}

export interface AccountLookup {
    accountId: string
    username: string
    accountNumber: string
}

// ── Transactions ─────────────────────────────────────
export interface Transaction {
    id: string
    type: string
    status: string
    sourceAccountId: string
    targetAccountId: string
    amount: number
    currency: string
    description: string
    createdAt: string
    completedAt: string | null
}

// ── Transfers ────────────────────────────────────────
export interface TransferRequest {
    sourceAccountId: string
    targetAccountId: string
    amount: number
    currency: string
    description?: string
}

export interface TransferResponse {
    transactionId: string
    status: string
    sourceAccountId: string
    targetAccountId: string
    amount: number
    currency: string
    description: string
    createdAt: string
}

// ── API Error ────────────────────────────────────────
export interface ApiError {
    type?: string
    title: string
    status: number
    detail: string
    instance?: string
    timestamp?: string
    errors?: string[]
}
