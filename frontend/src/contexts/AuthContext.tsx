/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from "react"
import type { ReactNode } from "react"
import type { UserProfile } from "@/types"
import api from "@/lib/api"

interface AuthContextType {
    user: UserProfile | null
    token: string | null
    isLoading: boolean
    isAuthenticated: boolean
    login: (token: string) => Promise<void>
    logout: () => void
    refreshProfile: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserProfile | null>(null)
    const [token, setToken] = useState<string | null>(() =>
        localStorage.getItem("token")
    )
    const [isLoading, setIsLoading] = useState(true)

    const fetchProfile = useCallback(async () => {
        try {
            const { data } = await api.get<UserProfile>("/auth/me")
            setUser(data)
        } catch {
            // Token invalid or expired
            localStorage.removeItem("token")
            setToken(null)
            setUser(null)
        }
    }, [])

    const login = useCallback(
        async (newToken: string) => {
            localStorage.setItem("token", newToken)
            setToken(newToken)
            await fetchProfile()
        },
        [fetchProfile]
    )

    const logout = useCallback(() => {
        localStorage.removeItem("token")
        setToken(null)
        setUser(null)
    }, [])

    const refreshProfile = useCallback(async () => {
        if (token) {
            await fetchProfile()
        }
    }, [token, fetchProfile])

    useEffect(() => {
        let cancelled = false
        if (token) {
            const load = async () => {
                try {
                    await fetchProfile()
                } finally {
                    if (!cancelled) setIsLoading(false)
                }
            }
            load()
        } else {
            setIsLoading(false)
        }
        return () => { cancelled = true }
    }, [token, fetchProfile])

    return (
        <AuthContext.Provider
            value={{
                user,
                token,
                isLoading,
                isAuthenticated: !!user && !!token,
                login,
                logout,
                refreshProfile,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const context = useContext(AuthContext)
    if (context === undefined) {
        throw new Error("useAuth must be used within an AuthProvider")
    }
    return context
}
