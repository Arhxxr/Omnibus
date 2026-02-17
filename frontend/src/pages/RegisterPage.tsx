import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Link, useNavigate } from "react-router-dom"
import { useAuth } from "@/contexts/AuthContext"
import api from "@/lib/api"
import type { AuthResponse, ApiError } from "@/types"
import { Eye, EyeOff, Shield, CheckCircle2 } from "lucide-react"
import type { AxiosError } from "axios"
import { toast } from "sonner"
import { Button } from "@/components/ui/Button"
import { Input } from "@/components/ui/Input"

const registerSchema = z.object({
    username: z
        .string()
        .min(3, "Must be at least 3 characters")
        .max(50, "Must be 50 characters or less"),
    email: z.string().email("Must be a valid email address"),
    password: z.string().min(8, "Must be at least 8 characters"),
})

type RegisterForm = z.infer<typeof registerSchema>

export function RegisterPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const [showPassword, setShowPassword] = useState(false)
    const [serverError, setServerError] = useState("")

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<RegisterForm>({
        resolver: zodResolver(registerSchema),
    })

    const onSubmit = async (data: RegisterForm) => {
        setServerError("")
        try {
            const response = await api.post<AuthResponse>("/auth/register", data)
            await login(response.data.token)
            toast.success("Account created!", {
                description: "Your account has been funded with $10,000 demo balance.",
            })
            navigate("/dashboard")
        } catch (err) {
            const error = err as AxiosError<ApiError>
            const message =
                error.response?.data?.detail ||
                "Registration failed. Please try again."
            setServerError(message)
            toast.error("Registration failed", { description: message })
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-background to-primary/5 px-4">
            <div className="w-full max-w-md space-y-8 animate-fade-in">
                {/* Logo */}
                <div className="text-center">
                    <div className="inline-flex items-center justify-center h-16 w-16 rounded-2xl bg-primary text-primary-foreground mb-4 shadow-lg animate-pulse-glow">
                        <Shield className="h-8 w-8" />
                    </div>
                    <h1 className="text-3xl font-bold tracking-tight">Create account</h1>
                    <p className="text-muted-foreground mt-2">
                        Get started with $10,000 demo balance
                    </p>
                </div>

                {/* Form Card */}
                <div className="rounded-2xl border border-border bg-card p-8 shadow-xl shadow-primary/5">
                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                        {serverError && (
                            <div className="rounded-lg bg-destructive/10 border border-destructive/20 px-4 py-3 text-sm text-destructive animate-fade-in">
                                {serverError}
                            </div>
                        )}

                        <Input
                            id="register-username"
                            type="text"
                            label="Username"
                            autoComplete="username"
                            placeholder="Choose a username"
                            error={errors.username?.message}
                            {...register("username")}
                        />

                        <Input
                            id="register-email"
                            type="email"
                            label="Email"
                            autoComplete="email"
                            placeholder="you@example.com"
                            error={errors.email?.message}
                            {...register("email")}
                        />

                        <div className="space-y-2">
                            <label
                                htmlFor="register-password"
                                className="text-sm font-medium leading-none"
                            >
                                Password
                            </label>
                            <div className="relative">
                                <Input
                                    id="register-password"
                                    type={showPassword ? "text" : "password"}
                                    autoComplete="new-password"
                                    className="pr-10"
                                    placeholder="Minimum 8 characters"
                                    error={errors.password?.message}
                                    {...register("password")}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                                    aria-label={showPassword ? "Hide password" : "Show password"}
                                >
                                    {showPassword ? (
                                        <EyeOff className="h-4 w-4" />
                                    ) : (
                                        <Eye className="h-4 w-4" />
                                    )}
                                </button>
                            </div>
                        </div>

                        <Button
                            type="submit"
                            loading={isSubmitting}
                            className="w-full"
                        >
                            {isSubmitting ? "Creating account…" : "Create Account"}
                        </Button>
                    </form>

                    {/* Features */}
                    <div className="mt-6 pt-6 border-t border-border space-y-2.5">
                        {[
                            "Instant $10,000 demo balance",
                            "Send money to any user",
                            "Full transaction history",
                        ].map((feature) => (
                            <div
                                key={feature}
                                className="flex items-center gap-2.5 text-sm text-muted-foreground"
                            >
                                <CheckCircle2 className="h-4 w-4 text-primary shrink-0" />
                                {feature}
                            </div>
                        ))}
                    </div>
                </div>

                <p className="text-center text-sm text-muted-foreground">
                    Already have an account?{" "}
                    <Link
                        to="/login"
                        className="font-semibold text-primary hover:underline"
                    >
                        Sign in
                    </Link>
                </p>
            </div>
        </div>
    )
}
