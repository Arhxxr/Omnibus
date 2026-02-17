import { useState } from "react"
import { useForm } from "react-hook-form"
import type { Resolver, SubmitHandler } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useAuth } from "@/contexts/AuthContext"
import { useQueryClient } from "@tanstack/react-query"
import api from "@/lib/api"
import { formatCurrency } from "@/lib/utils"
import type { AccountLookup, TransferResponse, ApiError } from "@/types"
import type { AxiosError } from "axios"
import { toast } from "sonner"
import {
    ArrowRight,
    CheckCircle2,
    Loader2,
    Search,
    User,
    AlertCircle,
} from "lucide-react"
import { Button } from "@/components/ui/Button"

const sendSchema = z.object({
    recipientUsername: z.string().min(1, "Recipient username is required"),
    amount: z
        .number({ error: "Enter a valid amount" })
        .positive("Amount must be positive")
        .max(1000000, "Amount too large"),
    description: z.string().optional(),
})

type SendForm = z.infer<typeof sendSchema>

type Step = "form" | "review" | "success"

export function SendMoneyPage() {
    const { user, refreshProfile } = useAuth()
    const queryClient = useQueryClient()
    const primaryAccount = user?.accounts?.[0]

    const [step, setStep] = useState<Step>("form")
    const [recipient, setRecipient] = useState<AccountLookup | null>(null)
    const [lookupError, setLookupError] = useState("")
    const [lookingUp, setLookingUp] = useState(false)
    const [serverError, setServerError] = useState("")
    const [idempotencyKey, setIdempotencyKey] = useState("")
    const [result, setResult] = useState<TransferResponse | null>(null)

    const {
        register,
        handleSubmit,
        watch,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<SendForm>({
        resolver: zodResolver(sendSchema) as Resolver<SendForm>,
        defaultValues: { description: "" },
    })

    const watchedUsername = watch("recipientUsername")
    const watchedAmount = watch("amount")

    const lookupRecipient = async () => {
        if (!watchedUsername) return
        setLookingUp(true)
        setLookupError("")
        setRecipient(null)
        try {
            const { data } = await api.get<AccountLookup>(
                `/accounts/lookup?username=${encodeURIComponent(watchedUsername)}`
            )
            if (data.accountId === primaryAccount?.id) {
                setLookupError("You cannot send money to yourself")
                toast.warning("Invalid recipient", {
                    description: "You cannot send money to yourself",
                })
            } else {
                setRecipient(data)
                toast.success("Recipient found", {
                    description: `${data.username} (${data.accountNumber})`,
                })
            }
        } catch {
            setLookupError("User not found")
            toast.error("User not found", {
                description: "Check the username and try again",
            })
        } finally {
            setLookingUp(false)
        }
    }

    const goToReview = () => {
        if (!recipient) return
        setIdempotencyKey(crypto.randomUUID())
        setStep("review")
    }

    const onSubmit: SubmitHandler<SendForm> = async (data) => {
        if (!primaryAccount || !recipient) return
        setServerError("")

        try {
            const { data: txn } = await api.post<TransferResponse>(
                "/transfers",
                {
                    sourceAccountId: primaryAccount.id,
                    targetAccountId: recipient.accountId,
                    amount: data.amount,
                    currency: primaryAccount.currency,
                    description: data.description || undefined,
                },
                { headers: { "Idempotency-Key": idempotencyKey } }
            )
            setResult(txn)
            setStep("success")
            toast.success("Transfer complete!", {
                description: `${formatCurrency(txn.amount, txn.currency)} sent to ${recipient.username}`,
            })
            await refreshProfile()
            queryClient.invalidateQueries({ queryKey: ["transactions"] })
        } catch (err) {
            const error = err as AxiosError<ApiError>
            const status = error.response?.status
            let message: string
            if (status === 409) {
                message = "This transfer has already been processed (duplicate request)."
            } else if (status === 422) {
                message = error.response?.data?.detail || "Insufficient funds."
            } else {
                message =
                    error.response?.data?.detail || "Transfer failed. Please try again."
            }
            setServerError(message)
            toast.error("Transfer failed", { description: message })
        }
    }

    if (step === "success" && result) {
        return (
            <div className="max-w-md mx-auto mt-12 animate-fade-in">
                <div className="rounded-2xl border border-border bg-card p-8 text-center shadow-xl">
                    <div className="mx-auto h-16 w-16 rounded-full bg-green-500/10 flex items-center justify-center mb-4">
                        <CheckCircle2 className="h-8 w-8 text-green-500" />
                    </div>
                    <h2 className="text-2xl font-bold mb-2">Transfer Complete!</h2>
                    <p className="text-muted-foreground mb-6">
                        Your money has been sent successfully
                    </p>

                    <div className="rounded-xl bg-muted/50 p-4 space-y-3 text-left mb-6">
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground">Amount</span>
                            <span className="font-semibold">
                                {formatCurrency(result.amount, result.currency)}
                            </span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground">To</span>
                            <span className="font-medium">{recipient?.username}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground">Status</span>
                            <span className="inline-flex items-center gap-1 text-green-600 font-medium text-sm">
                                <CheckCircle2 className="h-3.5 w-3.5" />
                                {result.status}
                            </span>
                        </div>
                    </div>

                    <Button
                        onClick={() => {
                            setStep("form")
                            setRecipient(null)
                            setResult(null)
                            reset()
                        }}
                        className="w-full"
                    >
                        Send Another
                    </Button>
                </div>
            </div>
        )
    }

    if (step === "review") {
        return (
            <div className="max-w-md mx-auto mt-12 animate-fade-in">
                <div className="rounded-2xl border border-border bg-card p-8 shadow-xl">
                    <h2 className="text-2xl font-bold mb-6 text-center">
                        Review Transfer
                    </h2>

                    <div className="space-y-4 mb-8">
                        <div className="rounded-xl bg-muted/50 p-4 space-y-3">
                            <div className="flex justify-between">
                                <span className="text-sm text-muted-foreground">To</span>
                                <span className="font-semibold">{recipient?.username}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-sm text-muted-foreground">Amount</span>
                                <span className="text-xl font-bold text-primary">
                                    {formatCurrency(
                                        watchedAmount || 0,
                                        primaryAccount?.currency
                                    )}
                                </span>
                            </div>
                        </div>
                    </div>

                    {serverError && (
                        <div className="rounded-lg bg-destructive/10 border border-destructive/20 px-4 py-3 text-sm text-destructive flex items-center gap-2 mb-4 animate-fade-in">
                            <AlertCircle className="h-4 w-4 shrink-0" />
                            {serverError}
                        </div>
                    )}

                    <div className="flex gap-3">
                        <Button
                            variant="outline"
                            onClick={() => {
                                setStep("form")
                                setServerError("")
                            }}
                            className="flex-1"
                        >
                            Back
                        </Button>
                        <Button
                            onClick={handleSubmit(onSubmit)}
                            loading={isSubmitting}
                            className="flex-1"
                        >
                            {isSubmitting ? "Sending…" : "Confirm & Send"}
                        </Button>
                    </div>
                </div>
            </div>
        )
    }

    return (
        <div className="max-w-md mx-auto space-y-6 animate-fade-in">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Send Money</h1>
                <p className="text-muted-foreground mt-1">
                    Transfer funds to another Omnibus user
                </p>
            </div>

            <div className="rounded-2xl border border-border bg-card p-8 shadow-xl shadow-primary/5">
                <form
                    onSubmit={(e) => {
                        e.preventDefault()
                        goToReview()
                    }}
                    className="space-y-5"
                >
                    {/* Recipient Lookup */}
                    <div className="space-y-2">
                        <label
                            htmlFor="send-recipient"
                            className="text-sm font-medium leading-none"
                        >
                            Recipient
                        </label>
                        <div className="flex gap-2">
                            <input
                                id="send-recipient"
                                type="text"
                                className="flex-1 h-11 rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 transition-shadow"
                                placeholder="Enter username"
                                {...register("recipientUsername")}
                            />
                            <button
                                type="button"
                                onClick={lookupRecipient}
                                disabled={lookingUp || !watchedUsername}
                                className="h-11 px-4 rounded-lg border border-border text-sm font-medium hover:bg-muted transition-colors disabled:opacity-50"
                                aria-label="Search for user"
                            >
                                {lookingUp ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                    <Search className="h-4 w-4" />
                                )}
                            </button>
                        </div>
                        {errors.recipientUsername && (
                            <p className="text-sm text-destructive">
                                {errors.recipientUsername.message}
                            </p>
                        )}
                        {lookupError && (
                            <p className="text-sm text-destructive animate-fade-in">
                                {lookupError}
                            </p>
                        )}
                        {recipient && (
                            <div className="flex items-center gap-2 rounded-lg bg-green-500/10 border border-green-500/20 px-3 py-2 text-sm text-green-700 dark:text-green-400 animate-fade-in">
                                <User className="h-4 w-4" />
                                Found:{" "}
                                <span className="font-semibold">{recipient.username}</span>
                                <span className="text-xs opacity-75">
                                    ({recipient.accountNumber})
                                </span>
                            </div>
                        )}
                    </div>

                    {/* Amount */}
                    <div className="space-y-2">
                        <label
                            htmlFor="send-amount"
                            className="text-sm font-medium leading-none"
                        >
                            Amount
                        </label>
                        <div className="relative">
                            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm font-medium">
                                $
                            </span>
                            <input
                                id="send-amount"
                                type="number"
                                step="0.01"
                                className="flex h-11 w-full rounded-lg border border-input bg-background pl-7 pr-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 transition-shadow"
                                placeholder="0.00"
                                {...register("amount", { valueAsNumber: true })}
                            />
                        </div>
                        {errors.amount && (
                            <p className="text-sm text-destructive">
                                {errors.amount.message}
                            </p>
                        )}
                        {primaryAccount && (
                            <p className="text-xs text-muted-foreground">
                                Available:{" "}
                                {formatCurrency(
                                    primaryAccount.balance,
                                    primaryAccount.currency
                                )}
                            </p>
                        )}
                    </div>

                    {/* Description */}
                    <div className="space-y-2">
                        <label
                            htmlFor="send-description"
                            className="text-sm font-medium leading-none"
                        >
                            Description{" "}
                            <span className="text-muted-foreground font-normal">
                                (optional)
                            </span>
                        </label>
                        <input
                            id="send-description"
                            type="text"
                            className="flex h-11 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 transition-shadow"
                            placeholder="What's it for?"
                            {...register("description")}
                        />
                    </div>

                    <Button
                        type="submit"
                        disabled={!recipient}
                        className="w-full"
                    >
                        Review Transfer
                        <ArrowRight className="h-4 w-4" />
                    </Button>
                </form>
            </div>
        </div>
    )
}
