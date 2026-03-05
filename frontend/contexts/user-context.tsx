"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { get } from "@/lib/api-client";
import { isAuthenticated } from "@/lib/auth";

interface UserProfile {
  name: string;
  username: string;
  roles: string[];
}

interface UserContextType {
  profile: UserProfile | null;
  loading: boolean;
  refreshProfile: () => Promise<void>;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export function UserProvider({ children }: { children: ReactNode }) {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchProfile = async () => {
    // 只在已登录状态下请求用户信息
    if (!isAuthenticated()) {
      setProfile(null);
      setLoading(false);
      return;
    }

    try {
      const response = await get<UserProfile>("/auth/me");
      if (response.data) {
        setProfile(response.data);
      } else {
        setProfile(null);
      }
    } catch (error) {
      console.error("Failed to fetch user profile:", error);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const refreshProfile = async () => {
    setLoading(true);
    await fetchProfile();
  };

  return (
    <UserContext.Provider value={{ profile, loading, refreshProfile }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  const context = useContext(UserContext);
  if (context === undefined) {
    throw new Error("useUser must be used within a UserProvider");
  }
  return context;
}

