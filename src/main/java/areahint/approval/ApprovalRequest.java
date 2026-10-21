package areahint.approval;

import areahint.data.AreaData;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

// 审批记录同时用于服务端持久化和网络传输，修改前后快照不直接写入正式域名文件。
public final class ApprovalRequest {
    public String id;
    public String worldKey;
    public String worldName;
    public String applicantId;
    public String applicantName;
    public String operation;
    public String dimensionId;
    public String status = "pending";
    public int revision = 1;
    public long createdAt;
    public long updatedAt;
    public List<Change> changes = new ArrayList<>();
    public List<Review> reviews = new ArrayList<>();
    public List<AreaData> previewAreas = new ArrayList<>();

    // areas 为域名数组差异，document 为描述或维度配置；相对路径仅由服务端生成。
    public static final class Change {
        public String kind;
        public String path;
        public JsonElement before;
        public JsonElement after;
    }

    // 每轮意见都保存在服务端，重新申请只追加历史，客户端不能改写此前的审批人和意见。
    public static final class Review {
        public String reviewerId;
        public String reviewerName;
        public String result;
        public String comment;
        public long time;
    }
}
