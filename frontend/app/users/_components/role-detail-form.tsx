import {useState} from "react";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Button} from "@/components/ui/button";
import {type Tag, TagInput} from "tagmento";
import {Plus, X} from "lucide-react";
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

interface RoleDetailFormProps {
  roleType: RoleType;
  details: RoleDetails;
  onChange: (details: RoleDetails) => void;
}

export function RoleDetailForm({ roleType, details, onChange }: RoleDetailFormProps) {
  switch (roleType) {
    case "超级管理员":
    case "校级领导":
      return null;

    case "年级组长": {
      const d = details as DetailsForGradeDean;
      return (
        <div className="space-y-2">
          <Label className="text-xs">管理年级（届）</Label>
          <Input
            type="number"
            placeholder="如 2027"
            value={d.gradeNo ?? ""}
            onChange={(e) =>
              onChange({ gradeNo: e.target.value ? parseInt(e.target.value) : null })
            }
          />
        </div>
      );
    }

    case "班主任": {
      const d = details as DetailsForClassAdvisor;
      const gc = d.gradeClass ?? { gradeNo: null, classNo: null };
      return (
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2">
            <Label className="text-xs">年级（届）</Label>
            <Input
              type="number"
              placeholder="如 2027"
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
          </div>
          <div className="space-y-2">
            <Label className="text-xs">班级</Label>
            <Input
              type="number"
              placeholder="如 1"
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
        </div>
      );
    }

    case "多班级观察员": {
      const d = details as DetailsForMultiClassObserver;
      const list = d.gradeClasses ?? [];
      const updateItem = (index: number, gc: GradeClass) => {
        const next = [...list];
        next[index] = gc;
        onChange({ gradeClasses: next });
      };
      const removeItem = (index: number) => {
        onChange({ gradeClasses: list.filter((_, i) => i !== index) });
      };
      const addItem = () => {
        onChange({ gradeClasses: [...list, { gradeNo: null, classNo: null }] });
      };
      return (
        <div className="space-y-2">
          <Label className="text-xs">可观察的班级列表</Label>
          {list.map((gc, i) => (
            <div key={i} className="flex gap-2 items-center">
              <Input
                type="number"
                placeholder="年级（届）"
                className="flex-1"
                value={gc.gradeNo ?? ""}
                onChange={(e) =>
                  updateItem(i, {
                    ...gc,
                    gradeNo: e.target.value ? parseInt(e.target.value) : null,
                  })
                }
              />
              <Input
                type="number"
                placeholder="班级"
                className="flex-1"
                value={gc.classNo ?? ""}
                onChange={(e) =>
                  updateItem(i, {
                    ...gc,
                    classNo: e.target.value ? parseInt(e.target.value) : null,
                  })
                }
              />
              <Button type="button" variant="ghost" size="icon" className="shrink-0" onClick={() => removeItem(i)}>
                <X className="h-4 w-4" />
              </Button>
            </div>
          ))}
          <Button type="button" variant="outline" size="sm" onClick={addItem}>
            <Plus className="h-3 w-3 mr-1" />
            添加班级
          </Button>
        </div>
      );
    }

    case "协作用户":
    case "家长用户":
    case "新生家长": {
      const d = details as DetailsForParentOrCU;
      const list = d.stuNos ?? [];
      const tags: Tag[] = list.map((s, i) => ({ id: String(i), text: s }));
      const [activeTagIndex, setActiveTagIndex] = useState<number | null>(null);
      return (
        <div className="space-y-2">
          <Label className="text-xs">关联学号列表</Label>
          <TagInput
            tags={tags}
            setTags={(newTags) => {
              const resolved = typeof newTags === "function" ? newTags(tags) : newTags;
              onChange({ stuNos: resolved.map((t) => t.text) });
            }}
            activeTagIndex={activeTagIndex}
            setActiveTagIndex={setActiveTagIndex}
            placeholder="输入学号后回车"
            size="sm"
            styleClasses={{
              inlineTagsContainer: "border-input rounded-md bg-background shadow-xs px-2 py-1.5",
              input: "text-sm placeholder:text-muted-foreground",
              tag: { body: "pl-2" },
            }}
          />
        </div>
      );
    }

    case "学生用户": {
      const d = details as DetailsForStudentUser;
      return (
        <div className="space-y-2">
          <Label className="text-xs">学号</Label>
          <Input
            placeholder="如 270101"
            value={d.stuNo ?? ""}
            onChange={(e) => onChange({ stuNo: e.target.value || null })}
          />
        </div>
      );
    }

    default:
      return null;
  }
}
