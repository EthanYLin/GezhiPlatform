import { useState, useCallback, useRef } from "react";
import type {
  PermissionGroup,
  FieldMetadata,
  FieldTreeNode,
  ValidationRule,
  RoleType,
} from "./_types";
import {
  getPermissionGroupDetail,
  createPermissionGroup,
  updatePermissionGroup,
  fetchMetadataSchema,
  fetchMetadataFields,
} from "./_api";
import { toast } from "sonner";

interface FormData {
  name: string;
  description: string;
  displayCaption: string;
  enabled: boolean;
  roleTypes: RoleType[];
}

const EMPTY_FORM: FormData = {
  name: "",
  description: "",
  displayCaption: "",
  enabled: true,
  roleTypes: [],
};

function buildFieldTree(
  fieldsMap: Record<string, FieldMetadata>,
  orderedJsonPaths: string[]
): FieldTreeNode[] {
  const entries = Object.entries(fieldsMap);

  const nodeMap = new Map<string, FieldTreeNode>();
  for (const [jsonPath, meta] of entries) {
    nodeMap.set(jsonPath, {
      jsonPath,
      title: meta.titles[meta.titles.length - 1],
      depth: meta.paths.length - 1,
      allowEdit: meta.allowEdit,
      isArray: meta.isArray,
      insideArray: meta.insideArray,
      ancestorPaths: meta.ancestorPaths,
      children: [],
      isLastChild: false,
    });
  }

  const roots: FieldTreeNode[] = [];
  const attached = new Set<string>();

  for (const jsonPath of orderedJsonPaths) {
    const meta = fieldsMap[jsonPath];
    const node = nodeMap.get(jsonPath);
    if (!meta || !node || attached.has(jsonPath)) continue;

    if (meta.ancestorPaths.length === 0) {
      roots.push(node);
    } else {
      const parentPath = meta.ancestorPaths[meta.ancestorPaths.length - 1];
      const parent = nodeMap.get(parentPath);
      if (parent) {
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }

    attached.add(jsonPath);
  }

  function markLastChild(nodes: FieldTreeNode[]) {
    for (const node of nodes) {
      node.isLastChild = false;
      markLastChild(node.children);
    }
    if (nodes.length > 0) {
      nodes[nodes.length - 1].isLastChild = true;
    }
  }

  markLastChild(roots);

  return orderedJsonPaths
    .map((jsonPath) => nodeMap.get(jsonPath))
    .filter((node): node is FieldTreeNode => !!node);
}

function collectSchemaOrderedJsonPaths(
  schema: unknown,
  availableJsonPaths: Set<string>
): string[] {
  const ordered: string[] = [];
  const seen = new Set<string>();

  const walk = (node: unknown) => {
    if (!node || typeof node !== "object") return;

    const schemaNode = node as Record<string, unknown>;
    const jsonPath = schemaNode["x-jsonpath"];
    if (
      typeof jsonPath === "string" &&
      availableJsonPaths.has(jsonPath) &&
      !seen.has(jsonPath)
    ) {
      ordered.push(jsonPath);
      seen.add(jsonPath);
    }

    const properties = schemaNode.properties;
    if (properties && typeof properties === "object" && !Array.isArray(properties)) {
      for (const child of Object.values(properties as Record<string, unknown>)) {
        walk(child);
      }
    }

    if (schemaNode.items) {
      walk(schemaNode.items);
    }

    if (Array.isArray(schemaNode.anyOf)) {
      for (const option of schemaNode.anyOf) {
        walk(option);
      }
    }
  };

  walk(schema);
  return ordered;
}

export function useRoleDetail() {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [mode, setMode] = useState<"create" | "edit">("create");
  const [editId, setEditId] = useState<number | null>(null);

  const [formData, setFormData] = useState<FormData>({ ...EMPTY_FORM });
  const [flatFields, setFlatFields] = useState<FieldTreeNode[]>([]);
  const fieldsMapRef = useRef<Record<string, FieldMetadata>>({});

  const [readableSet, setReadableSet] = useState<Set<string>>(new Set());
  const [writableSet, setWritableSet] = useState<Set<string>>(new Set());
  const [addArraySet, setAddArraySet] = useState<Set<string>>(new Set());
  const [editArraySet, setEditArraySet] = useState<Set<string>>(new Set());
  const [deleteArraySet, setDeleteArraySet] = useState<Set<string>>(new Set());

  const [validations, setValidations] = useState<ValidationRule[]>([]);

  const loadDetail = useCallback(async (id?: number) => {
    setLoading(true);

    const [schemaRes, fieldsRes] = await Promise.all([
      fetchMetadataSchema(),
      fetchMetadataFields(),
    ]);

    if (!fieldsRes.data) {
      toast.error("加载字段元数据失败");
      setLoading(false);
      return;
    }

    if (!schemaRes.data) {
      toast.error("加载字段结构失败");
      setLoading(false);
      return;
    }

    const fieldsMap = fieldsRes.data;
    fieldsMapRef.current = fieldsMap;

    const fieldPaths = Object.keys(fieldsMap);
    const schemaOrdered = collectSchemaOrderedJsonPaths(
      schemaRes.data,
      new Set(fieldPaths)
    );

    if (schemaOrdered.length === 0) {
      toast.error("加载字段结构失败");
      setLoading(false);
      return;
    }

    setFlatFields(buildFieldTree(fieldsMap, schemaOrdered));

    if (id) {
      setMode("edit");
      setEditId(id);

      const res = await getPermissionGroupDetail(id);
      if (!res.data) {
        toast.error(res.error || "加载权限组详情失败");
        setLoading(false);
        return;
      }
      const pg = res.data;
      setFormData({
        name: pg.name,
        description: pg.description || "",
        displayCaption: pg.displayCaption || "",
        enabled: pg.enabled,
        roleTypes: pg.roleTypes,
      });
      setReadableSet(new Set(pg.allowedReadableJsonPaths));
      setWritableSet(new Set(pg.allowedWritableJsonPaths));
      setAddArraySet(new Set(pg.allowedAddArrayJsonPaths));
      setEditArraySet(new Set(pg.allowedEditArrayJsonPaths));
      setDeleteArraySet(new Set(pg.allowedDeleteArrayJsonPaths));
      setValidations(pg.validations.map((v) => ({ ...v })));
    } else {
      setMode("create");
      setEditId(null);
      setFormData({ ...EMPTY_FORM });
      setReadableSet(new Set());
      setWritableSet(new Set());
      setAddArraySet(new Set());
      setEditArraySet(new Set());
      setDeleteArraySet(new Set());
      setValidations([]);
    }

    setLoading(false);
  }, []);

  const getMeta = (jsonPath: string) => fieldsMapRef.current[jsonPath];

  const getDescendants = useCallback(
    (jsonPath: string): string[] => {
      return flatFields
        .filter((f) => f.ancestorPaths.includes(jsonPath))
        .map((f) => f.jsonPath);
    },
    [flatFields]
  );

  const toggleVisible = useCallback(
    (jsonPath: string) => {
      const meta = getMeta(jsonPath);
      if (!meta) return;

      setReadableSet((prev) => {
        const next = new Set(prev);
        const wasChecked = next.has(jsonPath);

        if (wasChecked) {
          // Unchecking visible
          next.delete(jsonPath);

          // Rule 5: uncheck editable too
          setWritableSet((ws) => {
            const wn = new Set(ws);
            wn.delete(jsonPath);

            // Rule 4: cascade down for editable (non-array, non-insideArray)
            if (!meta.isArray && !meta.insideArray) {
              for (const desc of getDescendants(jsonPath)) {
                const dm = getMeta(desc);
                if (dm && !dm.isArray && !dm.insideArray) {
                  wn.delete(desc);
                }
              }
            }
            return wn;
          });

          // Rule 4: cascade uncheck descendants' visible (non-array, non-insideArray)
          if (!meta.isArray && !meta.insideArray) {
            for (const desc of getDescendants(jsonPath)) {
              const dm = getMeta(desc);
              if (dm && !dm.isArray && !dm.insideArray) {
                next.delete(desc);
              }
            }
            // Also cascade uncheck descendants' editable
            setWritableSet((ws) => {
              const wn = new Set(ws);
              for (const desc of getDescendants(jsonPath)) {
                const dm = getMeta(desc);
                if (dm && !dm.isArray && !dm.insideArray) {
                  wn.delete(desc);
                }
              }
              return wn;
            });
          }
        } else {
          // Checking visible
          next.add(jsonPath);

          // Rule 4: cascade check ancestors' visible (non-array, non-insideArray)
          if (!meta.isArray && !meta.insideArray) {
            for (const anc of meta.ancestorPaths) {
              const am = getMeta(anc);
              if (am && !am.isArray && !am.insideArray) {
                next.add(anc);
              }
            }
          }
        }

        return next;
      });
    },
    [getDescendants]
  );

  const toggleEditable = useCallback(
    (jsonPath: string) => {
      const meta = getMeta(jsonPath);
      if (!meta) return;

      setWritableSet((prev) => {
        const next = new Set(prev);
        const wasChecked = next.has(jsonPath);

        if (wasChecked) {
          // Unchecking editable
          next.delete(jsonPath);

          // Rule 4: cascade uncheck descendants' editable (non-array, non-insideArray)
          if (!meta.isArray && !meta.insideArray) {
            for (const desc of getDescendants(jsonPath)) {
              const dm = getMeta(desc);
              if (dm && !dm.isArray && !dm.insideArray) {
                next.delete(desc);
              }
            }
          }
        } else {
          // Checking editable
          next.add(jsonPath);

          // Rule 5: auto check visible
          setReadableSet((rs) => {
            const rn = new Set(rs);
            rn.add(jsonPath);

            // Rule 4: cascade check ancestors' visible (non-array, non-insideArray)
            if (!meta.isArray && !meta.insideArray) {
              for (const anc of meta.ancestorPaths) {
                const am = getMeta(anc);
                if (am && !am.isArray && !am.insideArray) {
                  rn.add(anc);
                }
              }
            }
            return rn;
          });

          // Rule 4: cascade check ancestors' editable (non-array, non-insideArray)
          if (!meta.isArray && !meta.insideArray) {
            for (const anc of meta.ancestorPaths) {
              const am = getMeta(anc);
              if (am && !am.isArray && !am.insideArray) {
                next.add(anc);
              }
            }
          }
        }

        return next;
      });
    },
    [getDescendants]
  );

  const toggleArrayPerm = useCallback(
    (jsonPath: string, type: "add" | "edit" | "delete") => {
      const setterMap = {
        add: setAddArraySet,
        edit: setEditArraySet,
        delete: setDeleteArraySet,
      };
      const setter = setterMap[type];
      setter((prev) => {
        const next = new Set(prev);
        if (next.has(jsonPath)) {
          next.delete(jsonPath);
        } else {
          next.add(jsonPath);
        }
        return next;
      });
    },
    []
  );

  const updateFormData = useCallback(
    (partial: Partial<FormData>) => {
      setFormData((prev) => ({ ...prev, ...partial }));
    },
    []
  );

  const addValidation = useCallback(() => {
    setValidations((prev) => [
      ...prev,
      { spelExpr: "", jsExpr: "", message: "" },
    ]);
  }, []);

  const removeValidation = useCallback((index: number) => {
    setValidations((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const updateValidation = useCallback(
    (index: number, partial: Partial<ValidationRule>) => {
      setValidations((prev) =>
        prev.map((v, i) => (i === index ? { ...v, ...partial } : v))
      );
    },
    []
  );

  const save = useCallback(async (): Promise<boolean> => {
    if (!formData.name.trim()) {
      toast.error("请输入权限组名称");
      return false;
    }

    setSaving(true);

    const payload: Omit<PermissionGroup, "id"> = {
      name: formData.name.trim(),
      description: formData.description.trim() || null,
      displayCaption: formData.displayCaption.trim() || null,
      enabled: formData.enabled,
      roleTypes: formData.roleTypes,
      allowedReadableJsonPaths: Array.from(readableSet),
      allowedWritableJsonPaths: Array.from(writableSet),
      allowedAddArrayJsonPaths: Array.from(addArraySet),
      allowedEditArrayJsonPaths: Array.from(editArraySet),
      allowedDeleteArrayJsonPaths: Array.from(deleteArraySet),
      validations: validations.filter(
        (v) => v.message.trim() || v.spelExpr.trim() || v.jsExpr.trim()
      ),
    };

    let res;
    if (mode === "edit" && editId) {
      res = await updatePermissionGroup(editId, payload);
    } else {
      res = await createPermissionGroup(payload);
    }

    setSaving(false);

    if (res.error) {
      toast.error(res.error);
      return false;
    }

    toast.success(mode === "edit" ? "权限组已更新" : "权限组已创建");
    return true;
  }, [
    formData,
    readableSet,
    writableSet,
    addArraySet,
    editArraySet,
    deleteArraySet,
    validations,
    mode,
    editId,
  ]);

  return {
    loading,
    saving,
    mode,
    formData,
    flatFields,
    readableSet,
    writableSet,
    addArraySet,
    editArraySet,
    deleteArraySet,
    validations,

    loadDetail,
    toggleVisible,
    toggleEditable,
    toggleArrayPerm,
    updateFormData,
    addValidation,
    removeValidation,
    updateValidation,
    save,
  };
}
