import { get, post, put, del } from "@/lib/api-client";
import type {
  PermissionGroup,
  PageResult,
  FieldMetadata,
} from "./_types";

export async function searchPermissionGroups(params: {
  keyword?: string;
  roleType?: string;
  page: number;
  size: number;
  sort?: string;
}) {
  const query = new URLSearchParams();
  if (params.keyword) query.append("keyword", params.keyword);
  if (params.roleType) query.append("roleType", params.roleType);
  query.append("page", params.page.toString());
  query.append("size", params.size.toString());
  if (params.sort) query.append("sort", params.sort);
  return get<PageResult<PermissionGroup>>(
    `/archive/permission-groups?${query.toString()}`
  );
}

export async function createPermissionGroup(data: Omit<PermissionGroup, "id">) {
  return post<PermissionGroup>("/archive/permission-groups", data);
}

export async function batchDeletePermissionGroups(ids: number[]) {
  const query = ids.map((id) => `ids=${id}`).join("&");
  return del<void>(`/archive/permission-groups?${query}`);
}

export async function getPermissionGroupDetail(id: number) {
  return get<PermissionGroup>(`/archive/permission-groups/${id}`);
}

export async function updatePermissionGroup(
  id: number,
  data: Omit<PermissionGroup, "id">
) {
  return put<PermissionGroup>(`/archive/permission-groups/${id}`, data);
}

export async function fetchMetadataSchema() {
  return get<Record<string, unknown>>("/archive/metadata");
}

export async function fetchMetadataFields() {
  return get<Record<string, FieldMetadata>>("/archive/metadata/fields");
}
