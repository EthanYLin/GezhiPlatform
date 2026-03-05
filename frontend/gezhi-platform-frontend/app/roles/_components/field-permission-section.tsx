import { useMemo } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { ChevronDown } from "lucide-react";
import type { FieldTreeNode } from "../_types";

interface FieldPermissionSectionProps {
  flatFields: FieldTreeNode[];
  readableSet: Set<string>;
  writableSet: Set<string>;
  addArraySet: Set<string>;
  editArraySet: Set<string>;
  deleteArraySet: Set<string>;
  onToggleVisible: (jsonPath: string) => void;
  onToggleEditable: (jsonPath: string) => void;
  onToggleArrayPerm: (jsonPath: string, type: "add" | "edit" | "delete") => void;
}

interface TreeGuide {
  depth: number;
  isLast: boolean;
  parentIsLast: boolean[];
}

const GUIDE_INDENT = 24;

function isInsideArrayIdNode(node: FieldTreeNode): boolean {
  if (!node.insideArray) return false;

  const titleName = node.title.trim().toLowerCase();
  const segments = node.jsonPath.replace(/\[\*\]/g, "").split(".");
  const pathName = segments[segments.length - 1]?.toLowerCase();

  return titleName === "id" || pathName === "id";
}

function GuideLines({ guide }: { guide: TreeGuide }) {
  if (guide.depth === 0) return null;

  const width = guide.depth * GUIDE_INDENT;
  const elbowX = width - GUIDE_INDENT / 2;

  return (
    <span
      aria-hidden="true"
      className="relative mr-1 h-8 shrink-0"
      style={{ width: `${width}px` }}
    >
      {guide.parentIsLast.map((isLast, idx) =>
        isLast ? null : (
          <span
            key={`v-${idx}`}
            className="absolute top-0 bottom-0 border-l border-border/60"
            style={{
              left: `${idx * GUIDE_INDENT + GUIDE_INDENT / 2}px`,
            }}
          />
        )
      )}

      <span
        className="absolute border-l border-border/60"
        style={{
          left: `${elbowX}px`,
          top: 0,
          bottom: guide.isLast ? "50%" : 0,
        }}
      />

      <span
        className="absolute border-t border-border/60"
        style={{
          left: `${elbowX}px`,
          top: "50%",
          width: `${GUIDE_INDENT / 2}px`,
        }}
      />
    </span>
  );
}

export function FieldPermissionSection({
  flatFields,
  readableSet,
  writableSet,
  addArraySet,
  editArraySet,
  deleteArraySet,
  onToggleVisible,
  onToggleEditable,
  onToggleArrayPerm,
}: FieldPermissionSectionProps) {
  const fieldNodeMap = useMemo(
    () => new Map(flatFields.map((node) => [node.jsonPath, node])),
    [flatFields]
  );

  const rows = useMemo(
    () =>
      flatFields
        .filter((node) => !isInsideArrayIdNode(node))
        .map((node) => ({
          node,
          guide: {
            depth: node.depth,
            isLast: node.isLastChild,
            parentIsLast: node.ancestorPaths.map(
              (ancestorPath) => fieldNodeMap.get(ancestorPath)?.isLastChild ?? true
            ),
          },
        })),
    [flatFields, fieldNodeMap]
  );

  if (rows.length === 0) {
    return (
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-foreground">字段权限</h3>
        <div className="text-sm text-muted-foreground py-4 text-center">
          加载中...
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <h3 className="text-sm font-semibold text-foreground">字段权限</h3>

      <div className="border rounded-md">
        {/* Sticky header */}
        <div className="flex items-center h-8 px-3 border-b bg-muted/50 text-xs font-medium text-muted-foreground sticky top-0 z-10">
          <div className="flex-1 min-w-0">字段名称</div>
          <div className="w-12 shrink-0 flex justify-center">
            <span>可见</span>
          </div>
          <div className="w-[140px] shrink-0 flex justify-center">
            <span>可编辑</span>
          </div>
        </div>

        {/* Scrollable field list */}
        <div className="max-h-[400px] overflow-y-auto pr-1">
          <div>
            {rows.map(({ node, guide }) => (
              <FieldRow
                key={node.jsonPath}
                node={node}
                guide={guide}
                isVisible={readableSet.has(node.jsonPath)}
                isWritable={writableSet.has(node.jsonPath)}
                canAdd={addArraySet.has(node.jsonPath)}
                canEdit={editArraySet.has(node.jsonPath)}
                canDelete={deleteArraySet.has(node.jsonPath)}
                onToggleVisible={onToggleVisible}
                onToggleEditable={onToggleEditable}
                onToggleArrayPerm={onToggleArrayPerm}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

interface FieldRowProps {
  node: FieldTreeNode;
  guide: TreeGuide;
  isVisible: boolean;
  isWritable: boolean;
  canAdd: boolean;
  canEdit: boolean;
  canDelete: boolean;
  onToggleVisible: (jsonPath: string) => void;
  onToggleEditable: (jsonPath: string) => void;
  onToggleArrayPerm: (jsonPath: string, type: "add" | "edit" | "delete") => void;
}

function FieldRow({
  node,
  guide,
  isVisible,
  isWritable,
  canAdd,
  canEdit,
  canDelete,
  onToggleVisible,
  onToggleEditable,
  onToggleArrayPerm,
}: FieldRowProps) {
  const renderEditableColumn = () => {
    if (!node.allowEdit) {
      return (
        <span className="text-xs text-muted-foreground">只读</span>
      );
    }

    if (node.insideArray) {
      return (
        <Tooltip>
          <TooltipTrigger asChild>
            <span className="text-xs text-muted-foreground cursor-help">-</span>
          </TooltipTrigger>
          <TooltipContent side="top">
            <p className="text-xs">
              数组中的字段没有单独编辑权限，其权限与数组一致。
            </p>
          </TooltipContent>
        </Tooltip>
      );
    }

    if (node.isArray) {
      return (
        <Popover>
          <PopoverTrigger asChild>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-7 w-7"
            >
              <ChevronDown className="h-4 w-4" />
              <span className="sr-only">数组权限</span>
            </Button>
          </PopoverTrigger>
          <PopoverContent align="end" className="w-36 p-2">
            <div className="space-y-1">
              <label className="flex items-center justify-between gap-2 rounded px-2 py-1 hover:bg-accent cursor-pointer">
                <span className="text-xs text-muted-foreground">可添加</span>
                <Checkbox
                  checked={canAdd}
                  onCheckedChange={() =>
                    onToggleArrayPerm(node.jsonPath, "add")
                  }
                />
              </label>
              <label className="flex items-center justify-between gap-2 rounded px-2 py-1 hover:bg-accent cursor-pointer">
                <span className="text-xs text-muted-foreground">可编辑</span>
                <Checkbox
                  checked={canEdit}
                  onCheckedChange={() =>
                    onToggleArrayPerm(node.jsonPath, "edit")
                  }
                />
              </label>
              <label className="flex items-center justify-between gap-2 rounded px-2 py-1 hover:bg-accent cursor-pointer">
                <span className="text-xs text-muted-foreground">可删除</span>
                <Checkbox
                  checked={canDelete}
                  onCheckedChange={() =>
                    onToggleArrayPerm(node.jsonPath, "delete")
                  }
                />
              </label>
            </div>
          </PopoverContent>
        </Popover>
      );
    }

    return (
      <Checkbox
        checked={isWritable}
        onCheckedChange={() => onToggleEditable(node.jsonPath)}
      />
    );
  };

  return (
    <div className="flex items-center h-8 px-3 hover:bg-muted/30 transition-colors">
      <div className="flex-1 min-w-0 flex items-center overflow-hidden">
        <GuideLines guide={guide} />
        <Tooltip>
          <TooltipTrigger asChild>
            <span className="text-sm truncate cursor-default">
              {node.title}
            </span>
          </TooltipTrigger>
          <TooltipContent side="top">
            <p className="font-mono text-xs">{node.jsonPath}</p>
          </TooltipContent>
        </Tooltip>
      </div>
      <div className="w-12 flex justify-center shrink-0">
        <Checkbox
          checked={isVisible}
          onCheckedChange={() => onToggleVisible(node.jsonPath)}
        />
      </div>
      <div className="w-[140px] flex justify-center shrink-0">
        {renderEditableColumn()}
      </div>
    </div>
  );
}
