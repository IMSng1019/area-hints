package areahint.vizedit;

import areahint.data.AreaData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// 一次编辑会话使用固定的域名快照，搜索与折叠只筛选预先排好的行。
final class VizEditAreaTree {
    record Row(AreaData area, int depth, boolean children) { }

    private final Map<String, AreaData> byName = new HashMap<>();
    private final Set<String> parentNames = new HashSet<>();
    private final List<AreaData> sorted;
    private final List<Row> preorder = new ArrayList<>();
    private final Set<String> collapsed = new HashSet<>();
    private String cachedSearch;
    private List<Row> visibleRows;

    VizEditAreaTree(List<AreaData> areas) {
        // 与旧的 find 保持一致，重复域名优先使用原文件中的第一项。
        for (AreaData area : areas) byName.putIfAbsent(area.getName(), area);
        sorted = areas.stream().sorted(Comparator.comparingInt(AreaData::getLevel).thenComparing(AreaData::getName)).toList();
        Map<String, List<AreaData>> children = new HashMap<>();
        for (AreaData area : sorted) {
            if (area.getBaseName() != null) children.computeIfAbsent(area.getBaseName(), key -> new ArrayList<>()).add(area);
        }
        parentNames.addAll(children.keySet());
        Set<String> visited = new HashSet<>();
        for (AreaData area : sorted) {
            if (find(area.getBaseName()) == null) appendBranch(area, children, visited);
        }
        // 旧文件可能含有无根的循环引用，仍然依次展示每个可达域名。
        for (AreaData area : sorted) {
            if (!visited.contains(area.getName())) appendBranch(area, children, visited);
        }
    }

    private void appendBranch(AreaData root, Map<String, List<AreaData>> children, Set<String> visited) {
        // 显式栈保持深度优先的显示顺序，也支持层级很深的域名。
        ArrayDeque<Row> pending = new ArrayDeque<>();
        pending.push(new Row(root, 0, hasChildren(root.getName())));
        while (!pending.isEmpty()) {
            Row row = pending.pop();
            String name = row.area().getName();
            if (!visited.add(name)) continue;
            preorder.add(row);
            List<AreaData> branch = children.get(name);
            if (branch == null) continue;
            for (int i = branch.size() - 1; i >= 0; i--) {
                AreaData child = branch.get(i);
                pending.push(new Row(child, row.depth() + 1, hasChildren(child.getName())));
            }
        }
    }

    AreaData find(String name) { return byName.get(name); }
    boolean hasChildren(String name) { return parentNames.contains(name); }
    boolean isCollapsed(String name) { return collapsed.contains(name); }

    void toggleCollapsed(String name) {
        if (!collapsed.remove(name)) collapsed.add(name);
        cachedSearch = null;
    }

    List<Row> rows(String search) {
        if (search.equals(cachedSearch)) return visibleRows;
        String query = search.trim().toLowerCase(Locale.ROOT);
        List<Row> next = new ArrayList<>();
        if (query.isEmpty()) {
            int collapsedDepth = Integer.MAX_VALUE;
            for (Row row : preorder) {
                if (row.depth() > collapsedDepth) continue;
                next.add(row);
                collapsedDepth = isCollapsed(row.area().getName()) ? row.depth() : Integer.MAX_VALUE;
            }
        } else {
            for (AreaData area : sorted) {
                if (area.getName().toLowerCase(Locale.ROOT).contains(query)) next.add(new Row(area, 0, hasChildren(area.getName())));
            }
        }
        cachedSearch = search;
        visibleRows = List.copyOf(next);
        return visibleRows;
    }
}
