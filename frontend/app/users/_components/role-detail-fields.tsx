import { useState } from "react";
import { Input } from "@/components/ui/input";
import { type Tag, TagInput } from "tagmento";
import type {
  DetailsForClassAdvisor,
  DetailsForGradeDean,
  DetailsForMultiClassObserver,
  DetailsForParentOrCU,
  DetailsForStudentUser,
  GradeClass,
  RoleDetails,
  RoleType,
} from "../_types";

interface RoleDetailFieldsProps {
  roleType: RoleType;
  details: RoleDetails;
  onChange: (details: RoleDetails) => void;
}

/**
 * Inline fields for a role row — no labels, all info via placeholder.
 * Returns a fragment of input elements meant to sit in a flex row.
 */
export function RoleDetailFields({ roleType, details, onChange }: RoleDetailFieldsProps) {
  switch (roleType) {
    case "超级管理员":
    case "校级领导":
      return null;

    case "年级组长": {
      const d = details as DetailsForGradeDean;
      return (
        <Input
          type="number"
          placeholder="年级，如 2027"
          className="flex-1 min-w-0 w-24 sm:w-32 placeholder:text-xs"
          value={d.gradeNo ?? ""}
          onChange={(e) =>
            onChange({ gradeNo: e.target.value ? parseInt(e.target.value) : null })
          }
        />
      );
    }

    case "班主任": {
      const d = details as DetailsForClassAdvisor;
      const gc = d.gradeClass ?? { gradeNo: null, classNo: null };
      return (
        <div className="flex flex-col sm:flex-row gap-3 w-full">
          <Input
            type="number"
            placeholder="年级，如 2027"
            className="flex-1 min-w-0 sm:w-32 placeholder:text-xs"
            value={gc.gradeNo ?? ""}
            onChange={(e) =>
              onChange({
                gradeClass: {
                  ...gc,
                  gradeNo: e.target.value ? parseInt(e.target.value) : null,
                },
              })
            }
          />
          <Input
            type="number"
            placeholder="班级，如 1"
            className="flex-1 min-w-0 sm:w-32 placeholder:text-xs"
            value={gc.classNo ?? ""}
            onChange={(e) =>
              onChange({
                gradeClass: {
                  ...gc,
                  classNo: e.target.value ? parseInt(e.target.value) : null,
                },
              })
            }
          />
        </div>
      );
    }

    case "多班级观察员": {
      const d = details as DetailsForMultiClassObserver;
      const list = d.gradeClasses ?? [];
      const tags: Tag[] = list.map((gc, i) => ({
        id: String(i),
        text: gc.gradeNo != null && gc.classNo != null ? `${gc.gradeNo}-${gc.classNo}` : "",
      }));
      const [activeIdx, setActiveIdx] = useState<number | null>(null);
      return (
        <div className="flex-1 min-w-0">
          <TagInput
            tags={tags}
            setTags={(newTags) => {
              const resolved = typeof newTags === "function" ? newTags(tags) : newTags;
              const gcs: GradeClass[] = [];
              for (const t of resolved) {
                const m = t.text.match(/^(\d+)-(\d+)$/);
                if (m) gcs.push({ gradeNo: parseInt(m[1]), classNo: parseInt(m[2]) });
              }
              onChange({ gradeClasses: gcs });
            }}
            activeTagIndex={activeIdx}
            setActiveTagIndex={setActiveIdx}
            placeholder="年级-班级，如 2027-1"
            size="sm"
            styleClasses={{
              inlineTagsContainer: "border-input rounded-md bg-background shadow-xs px-2 py-1.5",
              input: "text-sm placeholder:text-xs placeholder:text-muted-foreground",
              tag: { body: "pl-2" },
            }}
          />
        </div>
      );
    }

    case "协作用户":
    case "家长用户":
    case "新生家长": {
      const d = details as DetailsForParentOrCU;
      const list = d.stuNos ?? [];
      const tags: Tag[] = list.map((s, i) => ({ id: String(i), text: s }));
      const [activeIdx, setActiveIdx] = useState<number | null>(null);
      return (
        <div className="flex-1 min-w-0">
          <TagInput
            tags={tags}
            setTags={(newTags) => {
              const resolved = typeof newTags === "function" ? newTags(tags) : newTags;
              onChange({ stuNos: resolved.map((t) => t.text) });
            }}
            activeTagIndex={activeIdx}
            setActiveTagIndex={setActiveIdx}
            placeholder="输入学号后回车"
            size="sm"
            styleClasses={{
              inlineTagsContainer: "border-input rounded-md bg-background shadow-xs px-2 py-1.5",
              input: "text-sm placeholder:text-xs placeholder:text-muted-foreground",
              tag: { body: "pl-2" },
            }}
          />
        </div>
      );
    }

    case "学生用户": {
      const d = details as DetailsForStudentUser;
      return (
        <Input
          placeholder="学号，如 270101"
          className="flex-1 min-w-0 w-36 sm:w-48 placeholder:text-xs"
          value={d.stuNo ?? ""}
          onChange={(e) => onChange({ stuNo: e.target.value || null })}
        />
      );
    }

    default:
      return null;
  }
}
