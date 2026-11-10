package areahint.vizedit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// 可选元信息仅供 vizedit 审批使用，旧申请缺少此字段时继续采用原来的处理流程。
public final class VizEditMetadata {
    public String operation;
    public String targetName;
    public String resultName;
    public JsonObject fields;
    public JsonArray dependencies;

    public static VizEditMetadata from(VizEditChangeService.Prepared prepared) {
        VizEditMetadata metadata = new VizEditMetadata();
        metadata.operation = prepared.operation();
        metadata.targetName = prepared.targetName();
        metadata.resultName = prepared.resultName();
        metadata.fields = prepared.fields().deepCopy();
        metadata.dependencies = prepared.dependencies().deepCopy();
        return metadata;
    }
}
