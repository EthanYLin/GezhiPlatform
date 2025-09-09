package org.example.gezhiplatform.seed;

import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.utils.XJsonPathAugmentUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class PermissionGroupFaker {

    public static String PERSONAL_PART = "$.personalPart";
    public static String ADMISSION_PART = "$.admissionPart";
    public static String ADDRESS_PART = "$.addressPart";
    public static String FAMILY_PART = "$.familyPart";
    public static String HEALTH_PART = "$.healthPart";

    private final Map<String, XJsonPathAugmentUtils.FieldMeta> fieldMetadata;

    public PermissionGroupFaker(Map<String, XJsonPathAugmentUtils.FieldMeta> fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
    }

    public final class PathsResult {
        private final HashSet<String> paths;
        public PathsResult(HashSet<String> paths) {this.paths = paths;}
        public PathsResult andBeginWith(String prefix) {
            this.paths.addAll(
                fieldMetadata.keySet().stream()
                             .filter(path -> path.startsWith(prefix))
                             .collect(Collectors.toCollection(HashSet::new))
            );
            return this;
        }
        public PathsResult and(String path) {
            this.paths.add(path);
            return this;
        }
        public PathsResult and(Set<String> paths) {
            this.paths.addAll(paths);
            return this;
        }
        public PathsResult except(String path) {
            this.paths.remove(path);
            return this;
        }
        public Set<String> get() {return this.paths;}
    }

    public PathsResult pathsBeginWith(String prefix) {
        return new PathsResult(fieldMetadata.keySet().stream()
                                            .filter(path -> path.startsWith(prefix))
                                            .collect(Collectors.toCollection(HashSet::new))
        );
    }

    public static Set<RoleType> whichLevelGe(int minLevel) {
        return Arrays.stream(RoleType.values()).filter(roleType -> roleType.getLevel() >= minLevel).collect(
            Collectors.toSet());
    }
}
