import { useAuth } from "@/contexts/AuthContext"
import { useQuery } from "@tanstack/react-query"
import api from "@/lib/api"
import { formatCurrency, formatDate } from "@/lib/utils"
import type { Transaction } from "@/types"
import { ArrowUpRight, ArrowDownLeft, ArrowLeftRight } from "lucide-react"
import { TransactionSkeleton } from "@/components/ui/Skeleton"

export function ActivityPage() {
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

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Activity</h1>
                    <p className="text-muted-foreground mt-1">
                        Your complete transaction history
                    </p>
                </div>
                {!isLoading && transactions.length > 0 && (
                    <span className="text-sm text-muted-foreground bg-muted px-3 py-1 rounded-full">
                        {transactions.length} transaction{transactions.length !== 1 ? "s" : ""}
                    </span>
                )}
            </div>

            <div className="rounded-2xl border border-border bg-card shadow-sm">
                {isLoading ? (
                    <div className="divide-y divide-border">
                        {Array.from({ length: 5 }).map((_, i) => (
                            <TransactionSkeleton key={i} />
                        ))}
                    </div>
                ) : transactions.length === 0 ? (
                    <div className="px-6 py-16 text-center">
                        <ArrowLeftRight className="h-12 w-12 text-muted-foreground/30 mx-auto mb-4" />
                        <p className="text-lg font-medium text-muted-foreground">
                            No transactions yet
                        </p>
                        <p className="text-sm text-muted-foreground mt-1">
                            Send some money to see your activity here
                        </p>
                    </div>
                ) : (
                    <div className="divide-y divide-border">
                        {transactions.map((txn, i) => {
                            const isSent = txn.sourceAccountId === primaryAccount?.id
                            return (
                                <div
                                    key={txn.id}
                                    className="flex items-center justify-between px-6 py-4 hover:bg-muted/50 transition-colors animate-slide-up"
                                    style={{ animationDelay: `${i * 30}ms` }}
                                >
                                    <div className="flex items-center gap-4">
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
                                        <div className="min-w-0">
                                            <p className="font-medium text-sm">
                                                {isSent ? "Money Sent" : "Money Received"}
                                            </p>
                                            <p className="text-xs text-muted-foreground truncate max-w-[200px]">
                                                {txn.description || txn.type}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-center gap-6">
                                        <div className="hidden sm:block">
                                            <span
                                                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${txn.status === "COMPLETED"
                                                        ? "bg-green-500/10 text-green-600 dark:text-green-400"
                                                        : txn.status === "FAILED"
                                                            ? "bg-red-500/10 text-red-600 dark:text-red-400"
                                                            : "bg-yellow-500/10 text-yellow-600 dark:text-yellow-400"
                                                    }`}
                                            >
                                                {txn.status}
                                            </span>
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
                                </div>
                            )
                        })}
                    </div>
                )}
            </div>
        </div>
    )
}
