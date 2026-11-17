package areahint.vizedit.geometry;

import java.awt.geom.Area;

// 同一份草稿报错只自动移动视角一次，标记保留到下一次编辑、撤销或重新报告错误。
public final class GeometryIssueFocus {
    private Area source;
    private GeometryIssueLocator.Issue issue;
    private boolean pending;

    public GeometryIssueLocator.Issue report(Area shape, String errorKey) {
        issue = GeometryIssueLocator.locate(shape, errorKey);
        source = issue == null ? null : shape;
        pending = issue != null;
        return issue;
    }

    public GeometryIssueLocator.Issue current(Area shape) {
        // 草稿的撤销副本即使几何相等也属于新的编辑状态，不能继续显示旧错误标记。
        if (source != shape) clear();
        return issue;
    }

    public GeometryIssueLocator.Issue consume(Area shape) {
        GeometryIssueLocator.Issue current = current(shape);
        if (!pending) return null;
        pending = false;
        return current;
    }

    public void clear() {
        source = null;
        issue = null;
        pending = false;
    }
}
