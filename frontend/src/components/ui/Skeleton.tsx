export function Skeleton({ className = "" }: { className?: string }) {
    return (
        <div
            className={`animate-pulse rounded-lg bg-muted ${className}`}
            aria-hidden="true"
        />
    )
}

export function CardSkeleton() {
    return (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-10 w-10 rounded-xl" />
            </div>
            <Skeleton className="h-8 w-36 mb-1" />
            <Skeleton className="h-3 w-24" />
        </div>
    )
}

export function TransactionSkeleton() {
    return (
        <div className="flex items-center justify-between px-6 py-4">
            <div className="flex items-center gap-3">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div>
                    <Skeleton className="h-4 w-20 mb-1.5" />
                    <Skeleton className="h-3 w-32" />
                </div>
            </div>
            <div className="text-right">
                <Skeleton className="h-4 w-16 mb-1.5" />
                <Skeleton className="h-3 w-24" />
            </div>
        </div>
    )
}
