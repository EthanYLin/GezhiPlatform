import {del, get, post, put} from "@/lib/api-client";
import type {
    NewUserRequest,
    PageResult,
    PasswordResetResponse,
    SortState,
    User,
    UserFilters,
    UserRoleDetailsDTO,
    UserUpdateRequest,
} from "./_types";

export async function searchUsers(params: {
  filters: UserFilters;
  page: number;
  size: number;
  sort?: SortState;
}) {
  const query = new URLSearchParams();
  if (params.filters.keyword) query.append("keyword", params.filters.keyword);
  if (params.filters.isLocked && params.filters.isLocked !== "all")
    query.append("isLocked", params.filters.isLocked);
  if (params.filters.isEnabled && params.filters.isEnabled !== "all")
    query.append("isEnabled", params.filters.isEnabled);
  if (params.filters.roleType && params.filters.roleType !== "all")
    query.append("roleType", params.filters.roleType);
  query.append("page", params.page.toString());
  query.append("size", params.size.toString());
  if (params.sort) query.append("sort", `${params.sort.field},${params.sort.direction}`);
  return get<PageResult<User>>(`/admin/users?${query.toString()}`);
}

export async function createUser(data: NewUserRequest) {
  return post<User>("/admin/users", data);
}

export async function importUsers(data: NewUserRequest[]) {
  return post<User[]>("/admin/users/import", data);
}

export async function deleteUsers(userIds: number[]) {
  const query = userIds.map((id) => `userIds=${id}`).join("&");
  return del<void>(`/admin/users?${query}`);
}

export async function getUserDetail(userId: number) {
  return get<User>(`/admin/users/${userId}`);
}

export async function updateUserInfo(userId: number, data: UserUpdateRequest) {
  return put<User>(`/admin/users/${userId}/info`, data);
}

export async function getUserRoles(userId: number) {
  return get<UserRoleDetailsDTO[]>(`/admin/users/${userId}/roles`);
}

export async function updateUserRoles(userId: number, roles: UserRoleDetailsDTO[]) {
  return put<UserRoleDetailsDTO[]>(`/admin/users/${userId}/roles`, roles);
}

export async function resetPassword(userId: number) {
  return post<PasswordResetResponse>(`/admin/users/${userId}/password`);
}

export async function lockUser(userId: number) {
  return post<void>(`/admin/users/${userId}/lock`);
}

export async function unlockUser(userId: number) {
  return post<void>(`/admin/users/${userId}/unlock`);
}

export async function kickoutUser(userId: number) {
  return post<void>(`/admin/users/${userId}/kickout`);
}
