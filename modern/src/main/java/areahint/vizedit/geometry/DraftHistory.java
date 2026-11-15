package areahint.vizedit.geometry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.UnaryOperator;

// 参考 Province EditHistory（MIT），历史保存深拷贝快照，属性和几何可作为同一个草稿整体撤销。
public final class DraftHistory<T> {
    public static final int MAX_STEPS = 64;
    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();
    private final UnaryOperator<T> copier;
    private final int capacity;

    public DraftHistory(int capacity, UnaryOperator<T> copier) {
        this.capacity = Math.max(1, Math.min(MAX_STEPS, capacity));
        this.copier = Objects.requireNonNull(copier);
    }

    // 调用方在一次完整手势或属性修改之前压入旧状态，新操作自动清空重做分支。
    public void push(T snapshot) {
        undo.push(copier.apply(Objects.requireNonNull(snapshot)));
        while (undo.size() > capacity) undo.removeLast();
        redo.clear();
    }

    public T undo(T current) {
        if (undo.isEmpty()) return copier.apply(current);
        redo.push(copier.apply(current));
        return copier.apply(undo.pop());
    }

    public T redo(T current) {
        if (redo.isEmpty()) return copier.apply(current);
        undo.push(copier.apply(current));
        return copier.apply(redo.pop());
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public int undoDepth() { return undo.size(); }
    public void clear() { undo.clear(); redo.clear(); }
}
