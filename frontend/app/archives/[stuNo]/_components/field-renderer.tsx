"use client";

// ==================== 动态表单字段渲染组件 ====================

import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {Button} from "@/components/ui/button";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from "@/components/ui/select";
import {useState} from "react";
import {ChevronDown, ChevronRight, ChevronUp, Plus, Trash2} from "lucide-react";
import type {FormActions, JsonSchema, JsonSchemaProperty} from "../_types";
import {
    hasReadableChildren,
    isArrayAddable,
    isArrayDeletable,
    isFieldNullable,
    isFieldReadable,
    isFieldWritable,
    resolveFieldType,
} from "../_utils";

// ==================== 简单字段（用于数组项内） ====================

interface SimpleFieldProps {
  fieldName: string;
  fieldSchema: JsonSchemaProperty;
  fieldPath: string;
  formActions: FormActions;
  forceEditable?: boolean;
}

function SimpleField({
  fieldName,
  fieldSchema,
  fieldPath,
  formActions,
  forceEditable = false,
}: SimpleFieldProps) {
  const {
    permissions,
    validationErrors,
    getFieldValue,
    handleFieldChange,
    handleFieldBlur,
  } = formActions;

  if (!isFieldReadable(fieldSchema, permissions)) return null;

  const fieldTitle = fieldSchema.title || fieldName;
  const isReadOnly = fieldSchema.readOnly
    ? true
    : forceEditable
    ? false
    : !isFieldWritable(fieldSchema, permissions);
  const fieldValue = getFieldValue(fieldPath);

  // 解析枚举值
  let enumValues: string[] | undefined;
  if (fieldSchema.anyOf) {
    const enumOption = fieldSchema.anyOf.find((opt) => opt.enum);
    if (enumOption?.enum) enumValues = enumOption.enum;
  } else if (fieldSchema.enum) {
    enumValues = fieldSchema.enum;
  }

  // 枚举字段 → 下拉选择
  if (enumValues) {
    const nullable = isFieldNullable(fieldSchema);
    const selectValue =
      fieldValue === null || fieldValue === undefined || fieldValue === ""
        ? undefined
        : String(fieldValue);

    return (
      <div className="space-y-2">
        <Label htmlFor={fieldPath}>{fieldTitle}</Label>
        <Select
          disabled={isReadOnly}
          value={selectValue}
          onValueChange={(v) =>
            handleFieldChange(fieldPath, v === "__empty__" ? "" : v)
          }
        >
          <SelectTrigger id={fieldPath} className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {nullable && (
              <SelectItem value="__empty__">
                <span className="text-muted-foreground">(不填写)</span>
              </SelectItem>
            )}
            {enumValues.map((ev) => (
              <SelectItem key={ev} value={ev}>
                {ev}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    );
  }

  // 文本/数字输入
  const shouldUseTextarea = fieldSchema.maxLength && fieldSchema.maxLength > 200;
  const inputValue = fieldValue ?? "";
  const fieldType = resolveFieldType(fieldSchema);

  return (
    <div className="space-y-2">
      <Label htmlFor={fieldPath}>{fieldTitle}</Label>
      {shouldUseTextarea ? (
        <Textarea
          id={fieldPath}
          value={inputValue}
          disabled={isReadOnly}
          maxLength={fieldSchema.maxLength}
          onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
          onBlur={(e) =>
            handleFieldBlur(fieldPath, e.target.value, fieldSchema)
          }
          className={isReadOnly ? "bg-muted" : ""}
        />
      ) : (
        <Input
          id={fieldPath}
          type={fieldType === "integer" ? "number" : "text"}
          value={inputValue}
          disabled={isReadOnly}
          maxLength={fieldSchema.maxLength}
          onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
          onBlur={(e) =>
            handleFieldBlur(fieldPath, e.target.value, fieldSchema)
          }
          className={isReadOnly ? "bg-muted" : ""}
        />
      )}
      {validationErrors[fieldPath] && (
        <p className="text-xs text-destructive">
          {validationErrors[fieldPath]}
        </p>
      )}
    </div>
  );
}

// ==================== 数组字段 ====================

interface ArrayFieldProps {
  fieldName: string;
  fieldSchema: JsonSchemaProperty;
  fieldPath: string;
  formActions: FormActions;
}

function ArrayField({
  fieldName,
  fieldSchema,
  fieldPath,
  formActions,
}: ArrayFieldProps) {
  const {
    permissions,
    getArrayData,
    addArrayItem,
    removeArrayItem,
    moveArrayItem,
  } = formActions;

  const fieldTitle = fieldSchema.title || fieldName;
  const arrayData = getArrayData(fieldPath);
  const canAdd = isArrayAddable(fieldSchema, permissions);
  const canEdit = isFieldWritable(fieldSchema, permissions);
  const canDelete = isArrayDeletable(fieldSchema, permissions);

  // 折叠/展开状态：存储已切换默认状态的索引集合
  // 新增记录默认展开，已有记录默认折叠；点击后切换
  const [toggledItems, setToggledItems] = useState<Set<number>>(new Set());

  const toggleExpand = (index: number) => {
    setToggledItems((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  };

  // 找到第一个新增记录的索引（新增记录不能移动到这个分界线之上）
  const firstNewIndex = arrayData.findIndex(
    (item) => !item || item.id === undefined || item.id === null
  );

  return (
    <div className="mb-4 col-span-full">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base">{fieldTitle}</CardTitle>
          {canAdd && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => {
                const newIndex = arrayData.length;
                addArrayItem(fieldPath);
                // 新增记录默认展开，无需额外操作（默认状态即展开）
              }}
            >
              <Plus className="h-4 w-4 mr-1" />
              添加
            </Button>
          )}
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {arrayData.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                {canAdd ? '暂无数据，点击"添加"按钮创建条目' : "暂无数据"}
              </div>
            ) : (
              arrayData.map((item, index) => {
                // 通过是否有 id 区分已有记录和新增记录
                const isNewItem =
                  !item || item.id === undefined || item.id === null;

                // 已有记录：按钮受 canDelete 控制，编辑受 canEdit 控制
                // 新增记录（canAdd 时添加）：可编辑、可删除、可调整顺序
                const showReorderButtons = isNewItem ? true : canDelete;
                const showDeleteButton = isNewItem ? true : canDelete;
                const isItemEditable = isNewItem ? true : canEdit;

                // 新增记录不能向上移动到已有记录区域
                // 如果是新增记录且处于新增区域的第一个位置，禁止上移
                const canMoveUp = isNewItem
                  ? index > 0 && index > firstNewIndex
                  : index > 0;
                const canMoveDown = index < arrayData.length - 1;

                // 新增记录默认展开，点击后折叠；已有记录默认折叠，点击后展开
                const isExpanded = isNewItem
                  ? !toggledItems.has(index)
                  : toggledItems.has(index);

                return (
                  <div
                    key={`${fieldPath}[${index}]`}
                    className="border rounded-lg overflow-hidden"
                  >
                    {/* 数组项标题和操作按钮 */}
                    <div
                      className="flex items-center justify-between p-4 cursor-pointer hover:bg-muted/50 transition-colors"
                      onClick={() => toggleExpand(index)}
                    >
                      <div className="flex items-center gap-2">
                        {isExpanded ? (
                          <ChevronDown className="h-4 w-4 text-muted-foreground" />
                        ) : (
                          <ChevronRight className="h-4 w-4 text-muted-foreground" />
                        )}
                        <h4 className="font-medium text-sm">
                          {fieldTitle} #{index + 1}
                        </h4>
                      </div>
                      {(showReorderButtons || showDeleteButton) && (
                        <div
                          className="flex gap-1"
                          onClick={(e) => e.stopPropagation()}
                        >
                          {showReorderButtons && (
                            <>
                              <Button
                                type="button"
                                size="sm"
                                variant="ghost"
                                disabled={!canMoveUp}
                                onClick={() =>
                                  moveArrayItem(fieldPath, index, index - 1)
                                }
                              >
                                <ChevronUp className="h-4 w-4" />
                              </Button>
                              <Button
                                type="button"
                                size="sm"
                                variant="ghost"
                                disabled={!canMoveDown}
                                onClick={() =>
                                  moveArrayItem(fieldPath, index, index + 1)
                                }
                              >
                                <ChevronDown className="h-4 w-4" />
                              </Button>
                            </>
                          )}
                          {showDeleteButton && (
                            <Button
                              type="button"
                              size="sm"
                              variant="ghost"
                              className="text-destructive hover:text-destructive"
                              onClick={() => removeArrayItem(fieldPath, index)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      )}
                    </div>

                    {/* 渲染数组项的字段（折叠时隐藏，隐藏 id 字段） */}
                    {isExpanded && fieldSchema.items?.properties && (
                      <div className="px-4 pb-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {Object.entries(fieldSchema.items.properties)
                            .filter(
                              ([itemFieldName]) => itemFieldName !== "id"
                            )
                            .map(([itemFieldName, itemFieldSchema]) => (
                              <SimpleField
                                key={`${fieldPath}[${index}].${itemFieldName}`}
                                fieldName={itemFieldName}
                                fieldSchema={itemFieldSchema}
                                fieldPath={`${fieldPath}[${index}].${itemFieldName}`}
                                formActions={formActions}
                                forceEditable={isItemEditable}
                              />
                            ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ==================== 主字段渲染器 ====================

interface FieldRendererProps {
  fieldName: string;
  fieldSchema: JsonSchemaProperty;
  parentPath?: string;
  formActions: FormActions;
}

export function FieldRenderer({
  fieldName,
  fieldSchema,
  parentPath = "",
  formActions,
}: FieldRendererProps) {
  const {
    permissions,
    validationErrors,
    getFieldValue,
    handleFieldChange,
    handleFieldBlur,
  } = formActions;

  if (!isFieldReadable(fieldSchema, permissions)) return null;

  const fieldPath = parentPath ? `${parentPath}.${fieldName}` : fieldName;
  const fieldTitle = fieldSchema.title || fieldName;
  const isReadOnly = fieldSchema.readOnly || !isFieldWritable(fieldSchema, permissions);
  const fieldType = resolveFieldType(fieldSchema);

  // 解析 enum / const
  let enumValues: string[] | undefined;
  let constValue: string | undefined;

  if (fieldSchema.anyOf) {
    const enumOpt = fieldSchema.anyOf.find((o) => o.enum);
    if (enumOpt?.enum) enumValues = enumOpt.enum;
    const constOpt = fieldSchema.anyOf.find((o) => o.const);
    if (constOpt?.const) constValue = constOpt.const;
  } else if (fieldSchema.enum) {
    enumValues = fieldSchema.enum;
  } else if (fieldSchema.const) {
    constValue = fieldSchema.const;
  }

  // 对象类型 → 递归渲染子字段
  if (fieldType === "object" && fieldSchema.properties) {
    if (!hasReadableChildren(fieldSchema, permissions)) return null;

    return (
      <Card key={fieldPath} className="mb-4">
        <CardHeader>
          <CardTitle className="text-lg">{fieldTitle}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {Object.entries(fieldSchema.properties).map(
            ([subName, subSchema]) => (
              <FieldRenderer
                key={`${fieldPath}.${subName}`}
                fieldName={subName}
                fieldSchema={subSchema}
                parentPath={fieldPath}
                formActions={formActions}
              />
            )
          )}
        </CardContent>
      </Card>
    );
  }

  // 数组类型
  if (fieldType === "array" && fieldSchema.items) {
    return (
      <ArrayField
        fieldName={fieldName}
        fieldSchema={fieldSchema}
        fieldPath={fieldPath}
        formActions={formActions}
      />
    );
  }

  // 常量字段
  if (constValue !== undefined) {
    return (
      <div key={fieldPath} className="space-y-2">
        <Label htmlFor={fieldPath}>
          {fieldTitle}
          {fieldSchema.description && (
            <span className="text-xs text-muted-foreground ml-2">
              {fieldSchema.description}
            </span>
          )}
        </Label>
        <Input id={fieldPath} value={constValue} disabled className="bg-muted" />
      </div>
    );
  }

  // 枚举字段（下拉选择）
  if (enumValues) {
    const nullable = isFieldNullable(fieldSchema);
    const fieldValue = getFieldValue(fieldPath);
    const selectValue =
      fieldValue === null || fieldValue === undefined || fieldValue === ""
        ? undefined
        : String(fieldValue);

    return (
      <div key={fieldPath} className="space-y-2">
        <Label htmlFor={fieldPath}>
          {fieldTitle}
          {fieldSchema.description && (
            <span className="text-xs text-muted-foreground ml-2">
              {fieldSchema.description}
            </span>
          )}
        </Label>
        <Select
          disabled={isReadOnly}
          value={selectValue}
          onValueChange={(v) =>
            handleFieldChange(fieldPath, v === "__empty__" ? "" : v)
          }
        >
          <SelectTrigger id={fieldPath} className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {nullable && (
              <SelectItem value="__empty__">
                <span className="text-muted-foreground">(不填写)</span>
              </SelectItem>
            )}
            {enumValues.map((ev) => (
              <SelectItem key={ev} value={ev}>
                {ev}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    );
  }

  // 默认：文本 / 数字输入
  const shouldUseTextarea = fieldSchema.maxLength && fieldSchema.maxLength > 200;
  const fieldValue = getFieldValue(fieldPath);
  const inputValue = fieldValue ?? "";

  return (
    <div key={fieldPath} className="space-y-2">
      <Label htmlFor={fieldPath}>
        {fieldTitle}
        {fieldSchema.description && (
          <span className="text-xs text-muted-foreground ml-2">
            {fieldSchema.description}
          </span>
        )}
      </Label>
      {shouldUseTextarea ? (
        <Textarea
          id={fieldPath}
          value={inputValue}
          disabled={isReadOnly}
          maxLength={fieldSchema.maxLength}
          onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
          onBlur={(e) =>
            handleFieldBlur(fieldPath, e.target.value, fieldSchema)
          }
          className={isReadOnly ? "bg-muted" : ""}
        />
      ) : (
        <Input
          id={fieldPath}
          type={fieldType === "integer" ? "number" : "text"}
          value={inputValue}
          disabled={isReadOnly}
          maxLength={fieldSchema.maxLength}
          onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
          onBlur={(e) =>
            handleFieldBlur(fieldPath, e.target.value, fieldSchema)
          }
          className={isReadOnly ? "bg-muted" : ""}
        />
      )}
      {validationErrors[fieldPath] && (
        <p className="text-xs text-destructive">
          {validationErrors[fieldPath]}
        </p>
      )}
    </div>
  );
}

// ==================== 动态表单（渲染完整 Schema） ====================

interface DynamicFormProps {
  schema: JsonSchema;
  formActions: FormActions;
}

export function DynamicForm({ schema, formActions }: DynamicFormProps) {
  const { permissions } = formActions;

  return (
    <div className="space-y-6">
      {Object.entries(schema.properties).map(
        ([sectionName, sectionSchema]) => {
          const sectionType = resolveFieldType(sectionSchema);

          if (sectionType === "object" && sectionSchema.properties) {
            if (!hasReadableChildren(sectionSchema, permissions)) return null;

            return (
              <Card key={sectionName}>
                <CardHeader>
                  <CardTitle>{sectionSchema.title || sectionName}</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {Object.entries(sectionSchema.properties).map(
                      ([fieldName, fieldSchema]) => (
                        <FieldRenderer
                          key={`${sectionName}.${fieldName}`}
                          fieldName={fieldName}
                          fieldSchema={fieldSchema}
                          parentPath={sectionName}
                          formActions={formActions}
                        />
                      )
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          }

          return null;
        }
      )}
    </div>
  );
}
