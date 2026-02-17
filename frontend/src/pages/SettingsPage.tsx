import { useAuth } from "@/contexts/AuthContext"
import { useTheme } from "@/hooks/useTheme"
import { formatCurrency, getInitials } from "@/lib/utils"
import {
    User,
    Mail,
    CreditCard,
    Shield,
    Moon,
    Sun,
    Hash,
    CheckCircle2,
    Calendar,
} from "lucide-react"
import { Button } from "@/components/ui/Button"

export function SettingsPage() {
    const { user } = useAuth()
    const { theme, toggleTheme } = useTheme()
    const primaryAccount = user?.accounts?.[0]

    return (
        <div className="space-y-8 animate-fade-in">
            {/* Header */}
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
                <p className="text-muted-foreground mt-1">
                    Manage your profile and preferences
                </p>
            </div>

            {/* Profile Card */}
            <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
                <h2 className="text-lg font-semibold mb-6 flex items-center gap-2">
                    <User className="h-5 w-5 text-primary" />
                    Profile
                </h2>
                <div className="flex items-start gap-6">
                    <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary text-primary-foreground text-2xl font-bold shadow-md">
                        {getInitials(user?.username ?? "")}
                    </div>
                    <div className="flex-1 space-y-4">
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div className="flex items-center gap-3">
                                <User className="h-4 w-4 text-muted-foreground" />
                                <div>
                                    <p className="text-xs text-muted-foreground">
                                        Username
                                    </p>
                                    <p className="font-medium">
                                        {user?.username}
                                    </p>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                <Mail className="h-4 w-4 text-muted-foreground" />
                                <div>
                                    <p className="text-xs text-muted-foreground">
                                        Email
                                    </p>
                                    <p className="font-medium">
                                        {user?.email}
                                    </p>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                <Shield className="h-4 w-4 text-muted-foreground" />
                                <div>
                                    <p className="text-xs text-muted-foreground">
                                        User ID
                                    </p>
                                    <p className="font-mono text-sm">
                                        {user?.userId}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Account Details Card */}
            {primaryAccount && (
                <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
                    <h2 className="text-lg font-semibold mb-6 flex items-center gap-2">
                        <CreditCard className="h-5 w-5 text-primary" />
                        Account Details
                    </h2>
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        <div className="flex items-center gap-3 p-4 rounded-xl bg-accent/50 border border-border">
                            <Hash className="h-5 w-5 text-muted-foreground" />
                            <div>
                                <p className="text-xs text-muted-foreground">
                                    Account Number
                                </p>
                                <p className="font-mono font-medium">
                                    {primaryAccount.accountNumber}
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3 p-4 rounded-xl bg-accent/50 border border-border">
                            <CreditCard className="h-5 w-5 text-muted-foreground" />
                            <div>
                                <p className="text-xs text-muted-foreground">
                                    Balance
                                </p>
                                <p className="font-bold text-lg">
                                    {formatCurrency(
                                        primaryAccount.balance,
                                        primaryAccount.currency
                                    )}
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3 p-4 rounded-xl bg-accent/50 border border-border">
                            <CheckCircle2 className="h-5 w-5 text-green-500" />
                            <div>
                                <p className="text-xs text-muted-foreground">
                                    Status
                                </p>
                                <p className="font-medium capitalize">
                                    {primaryAccount.status.toLowerCase()}
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3 p-4 rounded-xl bg-accent/50 border border-border">
                            <Calendar className="h-5 w-5 text-muted-foreground" />
                            <div>
                                <p className="text-xs text-muted-foreground">
                                    Currency
                                </p>
                                <p className="font-medium">
                                    {primaryAccount.currency}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Preferences Card */}
            <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
                <h2 className="text-lg font-semibold mb-6 flex items-center gap-2">
                    {theme === "dark" ? (
                        <Moon className="h-5 w-5 text-primary" />
                    ) : (
                        <Sun className="h-5 w-5 text-primary" />
                    )}
                    Preferences
                </h2>
                <div className="flex items-center justify-between p-4 rounded-xl bg-accent/50 border border-border">
                    <div>
                        <p className="font-medium">Appearance</p>
                        <p className="text-sm text-muted-foreground">
                            Currently using{" "}
                            <span className="font-medium capitalize">
                                {theme}
                            </span>{" "}
                            mode
                        </p>
                    </div>
                    <Button variant="outline" size="sm" onClick={toggleTheme}>
                        {theme === "dark" ? (
                            <Sun className="h-4 w-4 mr-2" />
                        ) : (
                            <Moon className="h-4 w-4 mr-2" />
                        )}
                        {theme === "dark" ? "Light Mode" : "Dark Mode"}
                    </Button>
                </div>
            </div>

            {/* Security Info Card */}
            <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
                <h2 className="text-lg font-semibold mb-6 flex items-center gap-2">
                    <Shield className="h-5 w-5 text-primary" />
                    Security
                </h2>
                <div className="space-y-3">
                    <div className="flex items-center gap-3 p-4 rounded-xl bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800">
                        <CheckCircle2 className="h-5 w-5 text-green-500" />
                        <div>
                            <p className="font-medium text-green-800 dark:text-green-200">
                                JWT Authentication Active
                            </p>
                            <p className="text-sm text-green-600 dark:text-green-400">
                                Your session is secured with HMAC-SHA256 signed
                                tokens
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-3 p-4 rounded-xl bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800">
                        <CheckCircle2 className="h-5 w-5 text-green-500" />
                        <div>
                            <p className="font-medium text-green-800 dark:text-green-200">
                                Idempotency Protection
                            </p>
                            <p className="text-sm text-green-600 dark:text-green-400">
                                All transfers use unique idempotency keys to
                                prevent duplicate processing
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-3 p-4 rounded-xl bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800">
                        <CheckCircle2 className="h-5 w-5 text-green-500" />
                        <div>
                            <p className="font-medium text-green-800 dark:text-green-200">
                                Account Ownership Verified
                            </p>
                            <p className="text-sm text-green-600 dark:text-green-400">
                                Only you can access your account data and
                                initiate transfers
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
