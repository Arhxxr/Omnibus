import { useAuth } from "@/contexts/AuthContext"
import { useQuery } from "@tanstack/react-query"
import api from "@/lib/api"
import { formatCurrency, formatDate } from "@/lib/utils"
import type { Transaction } from "@/types"
import {
    ArrowUpRight,
    ArrowDownLeft,
    TrendingUp,
    Wallet,
    ArrowLeftRight,
    Clock,
} from "lucide-react"
import { Link } from "react-router-dom"
import { CardSkeleton, TransactionSkeleton } from "@/components/ui/Skeleton"

export function DashboardPage() {
    const { user } = useAuth()
    const primaryAccount = user?.accounts?.[0]

    const { data: transactions = [], isLoading } = useQuery<Transaction[]>({
        queryKey: ["transactions", primaryAccount?.id],
        queryFn: async () => {
            if (!primaryAccount) return []
            const { data } = await api.get<Transaction[]>(
                `/accounts/${primaryAccount.id}/transactions`
            )
            return data
        },
        enabled: !!primaryAccount,
    })

    const recentTransactions = transactions.slice(0, 5)

    return (
        <div className="space-y-8 animate-fade-in">
            {/* Header */}
            <div>
                <h1 className="text-3xl font-bold tracking-tight">
                    Welcome back, {user?.username}
                </h1>
                <p className="text-muted-foreground mt-1">
                    Here's an overview of your account
                </p>
            </div>

            {/* Stats Cards */}
            {isLoading ? (
                <div className="grid gap-4 md:grid-cols-3">
                    <CardSkeleton />
                    <CardSkeleton />
                    <CardSkeleton />
                </div>
            ) : (
                <div className="grid gap-4 md:grid-cols-3">
                    <div className="rounded-2xl border border-border bg-card p-6 shadow-sm hover:shadow-md transition-shadow group">
                        <div className="flex items-center justify-between mb-4">
                            <p className="text-sm font-medium text-muted-foreground">
                                Available Balance
                            </p>
                            <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center group-hover:bg-primary/20 transition-colors">
                                <Wallet className="h-5 w-5 text-primary" />
                            </div>
                        </div>
                        <p className="text-3xl font-bold tracking-tight">
                            {primaryAccount
                                ? formatCurrency(
                                    primaryAccount.balance,
                                    primaryAccount.currency
                                )
                                : "$0.00"}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">
                            Account: {primaryAccount?.accountNumber ?? "—"}
                        </p>
                    </div>

                    <div className="rounded-2xl border border-border bg-card p-6 shadow-sm hover:shadow-md transition-shadow group">
                        <div className="flex items-center justify-between mb-4">
                            <p className="text-sm font-medium text-muted-foreground">
                                Total Transactions
                            </p>
                            <div className="h-10 w-10 rounded-xl bg-blue-500/10 flex items-center justify-center group-hover:bg-blue-500/20 transition-colors">
                                <TrendingUp className="h-5 w-5 text-blue-500" />
                            </div>
                        </div>
                        <p className="text-3xl font-bold tracking-tight">
                            {transactions.length}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">All time</p>
                    </div>

                    <div className="rounded-2xl border border-border bg-card p-6 shadow-sm hover:shadow-md transition-shadow group">
                        <div className="flex items-center justify-between mb-4">
                            <p className="text-sm font-medium text-muted-foreground">
                                Account Status
                            </p>
                            <div className="h-10 w-10 rounded-xl bg-green-500/10 flex items-center justify-center group-hover:bg-green-500/20 transition-colors">
                                <Clock className="h-5 w-5 text-green-500" />
                            </div>
                        </div>
                        <p className="text-3xl font-bold tracking-tight capitalize">
                            {primaryAccount?.status?.toLowerCase() ?? "—"}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">
                            Since{" "}
                            {primaryAccount?.createdAt
                                ? formatDate(primaryAccount.createdAt)
                                : "—"}
                        </p>
                    </div>
                </div>
            )}

            {/* Quick Actions */}
            <div className="grid gap-4 md:grid-cols-2">
                <Link
                    to="/send"
                    className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-6 hover:border-primary/30 hover:shadow-md transition-all"
                >
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center group-hover:bg-primary group-hover:text-primary-foreground transition-colors duration-200">
                        <ArrowLeftRight className="h-6 w-6 text-primary group-hover:text-primary-foreground" />
                    </div>
                    <div>
                        <p className="font-semibold">Send Money</p>
                        <p className="text-sm text-muted-foreground">
                            Transfer funds to another user
                        </p>
                    </div>
                </Link>

                <Link
                    to="/activity"
                    className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-6 hover:border-primary/30 hover:shadow-md transition-all"
                >
                    <div className="h-12 w-12 rounded-xl bg-blue-500/10 flex items-center justify-center group-hover:bg-blue-500 group-hover:text-white transition-colors duration-200">
                        <Clock className="h-6 w-6 text-blue-500 group-hover:text-white" />
                    </div>
                    <div>
                        <p className="font-semibold">View Activity</p>
                        <p className="text-sm text-muted-foreground">
                            Browse your full transaction history
                        </p>
                    </div>
                </Link>
            </div>

            {/* Recent Transactions */}
            <div className="rounded-2xl border border-border bg-card shadow-sm">
                <div className="flex items-center justify-between px-6 py-4 border-b border-border">
                    <h2 className="font-semibold text-lg">Recent Activity</h2>
                    {transactions.length > 5 && (
                        <Link
                            to="/activity"
                            className="text-sm text-primary hover:underline font-medium"
                        >
                            View all
                        </Link>
                    )}
                </div>

                {isLoading ? (
                    <div>
                        <TransactionSkeleton />
                        <TransactionSkeleton />
                        <TransactionSkeleton />
                    </div>
                ) : recentTransactions.length === 0 ? (
                    <div className="px-6 py-12 text-center">
                        <ArrowLeftRight className="h-10 w-10 text-muted-foreground/40 mx-auto mb-3" />
                        <p className="text-muted-foreground font-medium">
                            No transactions yet
                        </p>
                        <p className="text-sm text-muted-foreground mt-1">
                            Send money to get started
                        </p>
                    </div>
                ) : (
                    <div className="divide-y divide-border">
                        {recentTransactions.map((txn, i) => {
                            const isSent = txn.sourceAccountId === primaryAccount?.id
                            return (
                                <div
                                    key={txn.id}
                                    className="flex items-center justify-between px-6 py-4 hover:bg-muted/50 transition-colors animate-slide-up"
                                    style={{ animationDelay: `${i * 50}ms` }}
                                >
                                    <div className="flex items-center gap-3">
                                        <div
                                            className={`h-10 w-10 rounded-full flex items-center justify-center shrink-0 ${isSent
                                                    ? "bg-red-500/10 text-red-500"
                                                    : "bg-green-500/10 text-green-500"
                                                }`}
                                        >
                                            {isSent ? (
                                                <ArrowUpRight className="h-4 w-4" />
                                            ) : (
                                                <ArrowDownLeft className="h-4 w-4" />
                                            )}
                                        </div>
                                        <div>
                                            <p className="font-medium text-sm">
                                                {isSent ? "Sent" : "Received"}
                                            </p>
                                            <p className="text-xs text-muted-foreground">
                                                {txn.description || txn.type}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p
                                            className={`font-semibold text-sm ${isSent ? "text-red-500" : "text-green-500"
                                                }`}
                                        >
                                            {isSent ? "-" : "+"}
                                            {formatCurrency(txn.amount, txn.currency)}
                                        </p>
                                        <p className="text-xs text-muted-foreground">
                                            {formatDate(txn.createdAt)}
                                        </p>
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                )}
            </div>
        </div>
    )
}
