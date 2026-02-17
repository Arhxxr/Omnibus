import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Link, useNavigate } from "react-router-dom"
import { useAuth } from "@/contexts/AuthContext"
import api from "@/lib/api"
import type { AuthResponse, ApiError } from "@/types"
import { Eye, EyeOff, Shield } from "lucide-react"
import type { AxiosError } from "axios"
import { toast } from "sonner"
import { Button } from "@/components/ui/Button"
import { Input } from "@/components/ui/Input"

const loginSchema = z.object({
    username: z.string().min(1, "Username is required"),
    password: z.string().min(1, "Password is required"),
})

type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const [showPassword, setShowPassword] = useState(false)
    const [serverError, setServerError] = useState("")

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginForm>({
        resolver: zodResolver(loginSchema),
    })

    const onSubmit = async (data: LoginForm) => {
        setServerError("")
        try {
            const response = await api.post<AuthResponse>("/auth/login", data)
            await login(response.data.token)
            toast.success("Welcome back!", {
                description: `Signed in as ${response.data.username}`,
            })
            navigate("/dashboard")
        } catch (err) {
            const error = err as AxiosError<ApiError>
            const message =
                error.response?.data?.detail || "Login failed. Please try again."
            setServerError(message)
            toast.error("Login failed", { description: message })
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
                    <h1 className="text-3xl font-bold tracking-tight">Welcome back</h1>
                    <p className="text-muted-foreground mt-2">
                        Sign in to your Omnibus account
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
                            id="login-username"
                            type="text"
                            label="Username"
                            autoComplete="username"
                            placeholder="Enter your username"
                            error={errors.username?.message}
                            {...register("username")}
                        />

                        <div className="space-y-2">
                            <label
                                htmlFor="login-password"
                                className="text-sm font-medium leading-none"
                            >
                                Password
                            </label>
                            <div className="relative">
                                <Input
                                    id="login-password"
                                    type={showPassword ? "text" : "password"}
                                    autoComplete="current-password"
                                    className="pr-10"
                                    placeholder="Enter your password"
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
                            {isSubmitting ? "Signing in…" : "Sign In"}
                        </Button>
                    </form>
                </div>

                <p className="text-center text-sm text-muted-foreground">
                    Don't have an account?{" "}
                    <Link
                        to="/register"
                        className="font-semibold text-primary hover:underline"
                    >
                        Create one
                    </Link>
                </p>
            </div>
        </div>
    )
}
